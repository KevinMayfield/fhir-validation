package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.OperationOutcome;

public class FhirException implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        if (caused != null ) {
            if (caused instanceof BaseServerResponseException) {
                BaseServerResponseException fhirException = ( BaseServerResponseException) caused;

                exchange.getIn().setBody(fhirException.getOperationOutcome());
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "422");
            } else if (caused instanceof UnprocessableEntityException) {
                UnprocessableEntityException fhirException = ( UnprocessableEntityException) caused;
                exchange.getIn().setBody(fhirException.getOperationOutcome());
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "422");
            } else {
                String message = caused.getMessage();
                if (message.contains("HTTP 401")) {
                    OperationOutcome operationOutcome = new OperationOutcome();
                    operationOutcome.addIssue()
                            .setCode(OperationOutcome.IssueType.SECURITY)
                            .setDiagnostics(message);
                    exchange.getIn().setBody(operationOutcome);
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "401");
                } else if (message.contains("HTTP 403")) {
                    OperationOutcome operationOutcome = new OperationOutcome();
                    operationOutcome.addIssue()
                            .setCode(OperationOutcome.IssueType.FORBIDDEN)
                            .setDiagnostics(message);
                    exchange.getIn().setBody(operationOutcome);
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "403");
                } else {
                    OperationOutcome operationOutcome = new OperationOutcome();
                    operationOutcome.addIssue()
                            .setCode(OperationOutcome.IssueType.EXCEPTION)
                            .setDiagnostics(message);
                    exchange.getIn().setBody(operationOutcome);
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "500");
                }
            }
        }
    }
}
