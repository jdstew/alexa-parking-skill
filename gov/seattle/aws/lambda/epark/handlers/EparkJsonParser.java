package gov.seattle.aws.lambda.epark.handlers;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

public class EparkJsonParser {
	private static final long DATA_REFRESH_PERIOD_SECONDS = 10L;

	private static LocalDateTime dataLastUpdate;
	private static HashMap<String, HashMap<String, String>> garages = 
			new HashMap<String, HashMap<String, String>>();

	private static void parseJson(String inputJson) {

		JsonParser parser = Json.createParser(new StringReader(inputJson));
		while (parser.hasNext()) {
			JsonParser.Event event = parser.next();
			switch (event) {
			case START_ARRAY:
				//System.out.println("START_ARRAY");
				break;
			case END_ARRAY:
				//System.out.println("END_ARRAY");
				break;
			case START_OBJECT:
				//System.out.println("START_OBJECT");
				HashMap<String, String> garageMap = new HashMap<String, String>();
				String thisGarageKey = null;
				String mapKey = null;
				String mapValue = null;

				while (parser.hasNext()) {
					JsonParser.Event objectEvent = parser.next();
					if (objectEvent == Event.END_OBJECT) {
						//System.out.println("END_OBJECT");
						garages.put(thisGarageKey, garageMap);
						break;
					} else if (objectEvent == Event.KEY_NAME) {
						mapKey = parser.getString().toLowerCase();
						if (parser.hasNext()) {
							objectEvent = parser.next();
							if ((objectEvent == Event.VALUE_NUMBER) || (objectEvent == Event.VALUE_STRING)) {
								mapValue = parser.getString().toLowerCase();
							} else if (objectEvent == Event.VALUE_FALSE) {
								mapValue = "false";
							} else if (objectEvent == Event.VALUE_TRUE) {
								mapValue = "true";
							} else if (objectEvent == Event.VALUE_NULL) {
								mapValue = "null";
							}
						}

						if (mapKey.equalsIgnoreCase("garagename")) {
							thisGarageKey = mapValue;
						}
						garageMap.put(mapKey, mapValue);
					}
				}
				break;
			case END_OBJECT:
				//System.out.println("END_OBJECT");
				break;
			case KEY_NAME:
				//System.out.print(event.toString() + " " + parser.getString() + " - ");
				break;
			case VALUE_FALSE:
				//System.out.println("VALUE_FALSE");
				break;
			case VALUE_NULL:
				//System.out.println("VALUE_NULL");
				break;
			case VALUE_TRUE:
				//System.out.println(event.toString());
				break;
			case VALUE_STRING:
				//System.out.println(event.toString() + " " + parser.getString());
				break;
			case VALUE_NUMBER:
				//System.out.println(event.toString() + " " + parser.getString());
				break;
			}
		}

		//System.out.println("garages..." + garages.toString());
	}

	public static String getGarageSpaces (String garageName) {
		if (dataLastUpdate != null) {
			long staleness = Duration.between (LocalDateTime.now(), dataLastUpdate).getSeconds();
			if (staleness >= DATA_REFRESH_PERIOD_SECONDS) {
				// this will force a re-query of the data
				try {
					parseJson (EparkInterface.queryWebService());
				} catch (IOException e) {
					e.printStackTrace();
					return "I am unable to reach Seattle e-park at this time";
				}
			}
		} else {
			// this will force an initial of the data
			try {
				parseJson (EparkInterface.queryWebService());
			} catch (IOException e) {
				e.printStackTrace();
				return "I am unable to reach Seattle e-park at this time";
			}
		}
		
		HashMap<String, String> garage = garages.get(garageName);
		if (garage == null) {
			return "I do not recognize that garage";
		}
		
		long elapsed = 0L;
		try {
			LocalDateTime garageReportTime = LocalDateTime.parse (garage.get("timestamp").toUpperCase(),  
					DateTimeFormatter.ISO_ZONED_DATE_TIME);
			elapsed = Duration.between (LocalDateTime.now(), garageReportTime).getSeconds();
		} catch (DateTimeParseException e) {
            // an error here isn't consequential
		};
		
		String spaces = garage.get("vacantspaces");
		if (spaces.equalsIgnoreCase("null")) {
			return garageName + " is not reporting spaces available";
		}
		
		dataLastUpdate = LocalDateTime.now();
		if (elapsed > 120) {
            return  garageName + " had " + spaces + " parking spaces " + (elapsed / 60) + " minutes ago";
		} else {
            return garageName + " has " + spaces + " parking spaces";
		}
	}

	public static String getNearbyGarages(String garageName) {
		if (dataLastUpdate != null) {
			long staleness = Duration.between(LocalDateTime.now(), dataLastUpdate).getSeconds();
			if (staleness >= DATA_REFRESH_PERIOD_SECONDS) {
				// this will force a re-query of the data
				getGarageSpaces(garageName);
			}
		} else {
			// this will force an initial of the data
			getGarageSpaces(garageName);
		}
		
		String searchNeighborhood = garages.get(garageName).get("neighborhood");
		StringBuilder nearbyGarages = new StringBuilder();
		
		garages.values() // get Collection from Map
			.stream() 
				.filter(g -> g.get("neighborhood").equalsIgnoreCase(searchNeighborhood)) // get matching neighborhoods
					.filter(g -> !g.get("garagename").equalsIgnoreCase(garageName)) // remove searched-for garage
						.forEach(g -> nearbyGarages.append(g.get("garagename") + ", "));
		
		// remove last 2 characters
		if (nearbyGarages.length() > 2) {
			nearbyGarages.delete(nearbyGarages.length()-2, nearbyGarages.length()-1);
		} else {
			return "I was not able to find nearby garages";
		}
		
		return nearbyGarages.toString();
	}

	public static String getGarageAddress(String garageName) {
		if (dataLastUpdate != null) {
			long staleness = Duration.between(LocalDateTime.now(), dataLastUpdate).getSeconds();
			if (staleness >= DATA_REFRESH_PERIOD_SECONDS) {
				// this will force a re-query of the data
				getGarageSpaces(garageName);
			}
		} else {
			// this will force an initial of the data
			getGarageSpaces(garageName);
		}
		
		HashMap<String, String> garage = garages.get(garageName);
		if (garage == null) {
			return "I do not recognize that garage";
		}
		
		String address = garage.get("streetaddress");
		if (address == null) {
			return garageName + " address is not available";
		} else {
			return garageName + " is located at " + address;
		}
	}

	public static void main(String[] args) {
		//String jsonData = "[{\"GaragePageName\":\"pikeplace.htm\",\"Disabled\":null,\"Id\":\"G2\",\"OutOfService\":true,\"Timestamp\":\"2018-04-20T13:23:35.3228584-07:00\",\"VacantSpaces\":728,\"GarageName\":\"Pike Place\",\"DisplayText\":\"OPEN\",\"Neighborhood\":\"Pike Place Market\",\"StreetAddress\":\"1531 Western Avenue\"},{\"GaragePageName\":\"Butler.htm\",\"Disabled\":null,\"Id\":\"G17\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.4009568-07:00\",\"VacantSpaces\":273,\"GarageName\":\"Butler Garage\",\"DisplayText\":\"OPEN\",\"Neighborhood\":\"Pioneer Square\",\"StreetAddress\":\"114 James Street\"},{\"GaragePageName\":\"1Columbia.htm\",\"Disabled\":null,\"Id\":\"G16\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.4009568-07:00\",\"VacantSpaces\":288,\"GarageName\":\"1st and Columbia\",\"DisplayText\":\"288\",\"Neighborhood\":\"Pioneer Square\",\"StreetAddress\":\"721 1st Avenue\"},{\"GaragePageName\":\"Stadiumplace.htm\",\"Disabled\":null,\"Id\":\"G15\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.4009568-07:00\",\"VacantSpaces\":4,\"GarageName\":\"Stadium Place\",\"DisplayText\":\"0\",\"Neighborhood\":\"Pioneer Square\",\"StreetAddress\":\"530 Occidental Avenue South\"},{\"GaragePageName\":\"pacificplace.htm\",\"Disabled\":null,\"Id\":\"G4\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.3228584-07:00\",\"VacantSpaces\":197,\"GarageName\":\"Pacific Place\",\"DisplayText\":\"197\",\"Neighborhood\":\"Retail District\",\"StreetAddress\":\"600 Pine Street\"},{\"GaragePageName\":\"pspcobb.htm\",\"Disabled\":null,\"Id\":\"G7\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.3384863-07:00\",\"VacantSpaces\":224,\"GarageName\":\"PSP Cobb\",\"DisplayText\":\"224\",\"Neighborhood\":\"Retail District\",\"StreetAddress\":\"315 Union Street\"},{\"GaragePageName\":\"3rdandstewart.htm\",\"Disabled\":null,\"Id\":\"G3\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.3228584-07:00\",\"VacantSpaces\":484,\"GarageName\":\"3rd and Stewart\",\"DisplayText\":\"OPEN\",\"Neighborhood\":\"Retail District\",\"StreetAddress\":\"1619 3rd Avenue\"},{\"GaragePageName\":\"wac.htm\",\"Disabled\":null,\"Id\":\"G11\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.3384863-07:00\",\"VacantSpaces\":40,\"GarageName\":\"WAC Parking\",\"DisplayText\":\"40\",\"Neighborhood\":\"Retail District\",\"StreetAddress\":\"1409 6th Avenue\"},{\"GaragePageName\":\"conventioncenter.htm\",\"Disabled\":null,\"Id\":\"G9\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.3853271-07:00\",\"VacantSpaces\":55,\"GarageName\":\"Convention Center\",\"DisplayText\":\"55\",\"Neighborhood\":\"Retail District\",\"StreetAddress\":\"800 Convention Place\"},{\"GaragePageName\":\"westedge.htm\",\"Disabled\":null,\"Id\":\"G22\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.5354264-07:00\",\"VacantSpaces\":207,\"GarageName\":\"West Edge\",\"DisplayText\":\"207\",\"Neighborhood\":\"Retail District\",\"StreetAddress\":\"1508 2nd Ave\"},{\"GaragePageName\":\"BellStPier.htm\",\"Disabled\":null,\"Id\":\"G13\",\"OutOfService\":true,\"Timestamp\":\"2018-04-20T13:23:35.3853271-07:00\",\"VacantSpaces\":850,\"GarageName\":\"Bell Street Pier\",\"DisplayText\":\"850\",\"Neighborhood\":\"Waterfront\",\"StreetAddress\":\"9 Wall Street\"},{\"GaragePageName\":\"Hilclimb.htm\",\"Disabled\":null,\"Id\":\"G14\",\"OutOfService\":true,\"Timestamp\":\"2018-04-20T13:23:35.4009568-07:00\",\"VacantSpaces\":8,\"GarageName\":\"Hillclimb Garage\",\"DisplayText\":\"8\",\"Neighborhood\":\"Waterfront\",\"StreetAddress\":\"1422 Alaskan Way\"},{\"GaragePageName\":\"Waterfront.htm\",\"Disabled\":null,\"Id\":\"G19\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.4322041-07:00\",\"VacantSpaces\":183,\"GarageName\":\"Waterfront Place\",\"DisplayText\":\"OPEN\",\"Neighborhood\":\"Waterfront\",\"StreetAddress\":\"1009 Western Ave\"},{\"GaragePageName\":\"Watermark.htm\",\"Disabled\":null,\"Id\":\"G18\",\"OutOfService\":false,\"Timestamp\":\"2018-04-20T13:23:35.4165804-07:00\",\"VacantSpaces\":0,\"GarageName\":\"Watermark Garage\",\"DisplayText\":\"0\",\"Neighborhood\":\"Waterfront\",\"StreetAddress\":\"1108 Western Avenue\"}]";
		//parseJson(jsonData);
		System.out.println(getGarageSpaces("1st and Columbia".toLowerCase()));
		System.out.println(getGarageAddress("1st and Columbia".toLowerCase()));
		System.out.println(getNearbyGarages("1st and Columbia".toLowerCase()));
	}
}
