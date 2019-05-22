package gov.seattle.aws.lambda.epark;

import com.amazon.ask.Skill;
import com.amazon.ask.Skills;

import gov.seattle.aws.lambda.epark.handlers.CancelandStopIntentHandler;
import gov.seattle.aws.lambda.epark.handlers.GarageAddressIntentHandler;
import gov.seattle.aws.lambda.epark.handlers.GarageSpacesIntentHandler;
import gov.seattle.aws.lambda.epark.handlers.GaragesNearbyIntentHandler;
import gov.seattle.aws.lambda.epark.handlers.HelpIntentHandler;
import gov.seattle.aws.lambda.epark.handlers.LaunchRequestHandler;
import gov.seattle.aws.lambda.epark.handlers.SessionEndedRequestHandler;

import com.amazon.ask.SkillStreamHandler;

public class SeattleEParkStreamHandler extends SkillStreamHandler {

    private static Skill getSkill() {
        return Skills.standard()
                .addRequestHandlers(
                        new CancelandStopIntentHandler(),
                        new GarageAddressIntentHandler(),
                        new GaragesNearbyIntentHandler(),
                        new GarageSpacesIntentHandler(),
                        new HelpIntentHandler(),
                        new LaunchRequestHandler(),
                        new SessionEndedRequestHandler())
                .withSkillId("amzn1.ask.skill.b331dfa5-b2b1-4467-b7a7-dc867d88b0b9")
                .build();
    }

    public SeattleEParkStreamHandler() {
        super(getSkill());
    }

}
