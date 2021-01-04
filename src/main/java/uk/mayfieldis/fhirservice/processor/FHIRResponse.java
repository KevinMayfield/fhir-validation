package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FHIRResponse implements Processor {


    FhirContext ctx;

    public FHIRResponse(FhirContext _ctx) {
        ctx = _ctx;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        Object body = exchange.getIn().getBody();

        if (body instanceof Resource || body instanceof IBaseResource) {
            if (body instanceof OperationOutcome) {
                OperationOutcome outcome = (OperationOutcome) body;
                if (outcome.hasIssue() && !outcome.getIssueFirstRep().getCode().equals(OperationOutcome.IssueType.INFORMATIONAL)) {
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "422");
                }
            }
            if (exchange.getIn().getHeader(HttpHeaders.ACCEPT) != null
                    && (exchange.getIn().getHeader(HttpHeaders.ACCEPT).toString().contains("xml"))) {
                exchange.getIn().setBody(ctx.newXmlParser().encodeResourceToString((IBaseResource) body));
                exchange.getIn().setHeader(HttpHeaders.CONTENT_TYPE,"application/fhir+xml");

            } else {
                exchange.getIn().setBody(ctx.newJsonParser().encodeResourceToString((IBaseResource) body));
            }
        }

    }

    private String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}

