package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Bundle;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;

public class FHIRValidation implements Processor {

    ServerFHIRValidation serverFHIRValidation;

    FhirContext ctx;

    public FHIRValidation(FhirContext ctx, ServerFHIRValidation serverFHIRValidation) {
        this.serverFHIRValidation = serverFHIRValidation;
        this.ctx = ctx;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody();
        Bundle bundle = null;
        if (body instanceof Bundle) {
            bundle = (Bundle) body;
        } else if (body instanceof Bundle)  {
            ctx.newJsonParser().parseResource((String) body);
        }
        if (bundle != null && FHIRServerProperties.getValidationFlag()) {
            bundle.setMeta(null);
            serverFHIRValidation.validateWithResult(bundle);
        }
    }

}
