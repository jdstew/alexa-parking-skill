package gov.seattle.aws.lambda.epark.handlers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.InputStream;

class EparkInterface {
	static final String WEB_SERVICE_URL = "http://web6.seattle.gov/sdot/wsvcEparkGarageOccupancy/Occupancy.asmx/GetGarageList?prmGarageID=G&prmMyCallbackFunctionName=?";
	
	static public String queryWebService () throws IOException {
		URL url = new URL (WEB_SERVICE_URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		// set up the request parameters
		conn.setRequestMethod("GET");
		//note: the below line will cause a response code of 500 (internal server error)
		//conn.setRequestProperty("Content-Type", "application/json");
		conn.setConnectTimeout(5000); // 5000 milliseconds = 5 seconds
		conn.setReadTimeout(5000); // 5000 milliseconds = 5 seconds
		conn.setInstanceFollowRedirects(false);
		
		// get the http response
		conn.connect();
		
		// check for a normal response
		int responseCode = conn.getResponseCode();
		if ((responseCode < 200) && (responseCode > 299)) {
			throw new IOException ("An abnormal http response code was received (" +
				responseCode + ")");
		}
		
		StringBuffer sb; // this is the response content
		
		// read the response
		InputStream is = conn.getInputStream();
		if (is.available() > 0) {
			byte[] inputByte = new byte[is.available()];
			is.read(inputByte);
			sb = new StringBuffer (new String (inputByte, "UTF-8"));
		} else sb = null;
		is.close();
		conn.disconnect();
		
		// remove any non-JSON start characters in the response
		while ((sb.charAt(0) != '{') && (sb.charAt(0) != '[')) {
			sb.deleteCharAt(0);
		}
		// remove any non-JSON end characters at the end of the response
		while (((sb.charAt(sb.length() - 1)) != '}') && ((sb.charAt(sb.length() - 1)) != ']')) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
	public static void main(String[] args) {
		System.out.println("Response was:");
		try {
			System.out.println(EparkInterface.queryWebService());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
