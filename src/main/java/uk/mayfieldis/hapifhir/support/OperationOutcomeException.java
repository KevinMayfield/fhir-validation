package uk.mayfieldis.hapifhir.support;


import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;

public class OperationOutcomeException extends Exception {
    private OperationOutcome outcome;

    public OperationOutcomeException(OperationOutcome operationOutcome) {
        this.outcome = operationOutcome;
    }

    public OperationOutcomeException(String message, String diagnostics, OperationOutcome.IssueType type) {
        this.outcome = new OperationOutcome();
        this.outcome.addIssue().setCode(type).setSeverity(OperationOutcome.IssueSeverity.FATAL).setDiagnostics(diagnostics).setDetails((new CodeableConcept()).setText(message));
    }

    public OperationOutcome getOutcome() {
        return this.outcome;
    }

    public void setOutcome(OperationOutcome outcome) {
        this.outcome = outcome;
    }
}
