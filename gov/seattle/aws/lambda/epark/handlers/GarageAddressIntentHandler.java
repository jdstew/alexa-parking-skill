/*
     Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

     Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
     except in compliance with the License. A copy of the License is located at

         http://aws.amazon.com/apache2.0/

     or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
     the specific language governing permissions and limitations under the License.
*/
package gov.seattle.aws.lambda.epark.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.Request;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class GarageAddressIntentHandler implements RequestHandler {
	public static final String INTENT_NAME = "get_garage_address";

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName(INTENT_NAME));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
    	Request request = input.getRequestEnvelope().getRequest();
        IntentRequest intentRequest = (IntentRequest) request;
        Intent intent = intentRequest.getIntent();
        Map<String, Slot> intentSlots = intent.getSlots();
        Slot garageSlot = intentSlots.get("location");
        String garageName = garageSlot.getValue().toLowerCase();
    	
        String speechText = EparkJsonParser.getGarageAddress(garageName.toLowerCase());
        
        return input.getResponseBuilder()
                .withSpeech(speechText)
                .withSimpleCard("seattle e park", speechText)
                .build();
    }
    
    public static void main (String [] args) {
    	//todo: test case
    }
}
