package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.OperationOutcome;

public class MessageSent implements Processor {

    FhirContext ctx;

    public MessageSent(FhirContext _ctx) {
        ctx = _ctx;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome.addIssue().setCode(OperationOutcome.IssueType.INFORMATIONAL);

        exchange.getIn().setBody(ctx.newJsonParser().encodeResourceToString(operationOutcome));
    }
}
