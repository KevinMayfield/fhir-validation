package uk.mayfieldis.fhirservice.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

public class Clients implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String message;
        JSONObject json = new JSONObject();

        JSONObject hrv = new JSONObject();
        // No client secret as this handled within camel
        hrv.put("client_id", FHIRServerProperties.getIhealthClientId());
        json.put("ihealth", hrv);

        JSONObject strava = new JSONObject();
        strava.put("client_id", FHIRServerProperties.getStravaClientId());
        strava.put("client_secret", FHIRServerProperties.getStravaClientSecret());
        json.put("strava", strava);

        JSONObject withings = new JSONObject();
        withings.put("client_id", FHIRServerProperties.getWithingsClientId());
        withings.put("client_secret", FHIRServerProperties.getWithingsClientSecret());
        json.put("withings", withings);

        exchange.getIn().setBody(json.toString());
    }
}
