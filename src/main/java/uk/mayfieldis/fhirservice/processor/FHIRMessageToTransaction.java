package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import uk.mayfieldis.fhirservice.MessageConfig;

public class FHIRMessageToTransaction implements Processor {

    FhirContext ctx = null;

    public FHIRMessageToTransaction(FhirContext _ctx) {
        this.ctx = _ctx;
    }
    @Override
    public void process(Exchange exchange) throws Exception {

        // HAPI needs a transaction bundle to process the resources (if we posted the message bundle in we would get a bundle stored)

        Object body = exchange.getIn().getBody();
        Bundle bundle= null;
        if (body instanceof Bundle) {
            bundle = (Bundle) body;
        }
        if (body instanceof String) {
            String string = (String) body;
            IBaseResource resource = ctx.newJsonParser().parseResource(string);
            if (resource instanceof Bundle) {
                bundle = (Bundle) resource;
            }
        }
        if (bundle == null) {
            throw new UnprocessableEntityException("Empty Message or unknown type");
        }
        bundle.setType(Bundle.BundleType.TRANSACTION);
        MessageHeader messageHeader = null;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
           if (entry.hasResource() && entry.getResource() instanceof MessageHeader) {
               messageHeader = (MessageHeader) entry.getResource();
           }
        }
        if (messageHeader == null) {
            throw new UnprocessableEntityException("Missing MessageHeader");
        }
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {


            String conditionalUrl = getConditional(entry.getResource());
            if (conditionalUrl !=null && !conditionalUrl.isEmpty()) {
                if (MessageConfig.doUpdate(entry.getResource().getClass().getSimpleName(),messageHeader.getEventCoding().getCode())) {
                    // Always update, default is to create
                    entry.getRequest()
                            .setMethod(Bundle.HTTPVerb.PUT)
                            .setUrl(entry.getResource().getClass().getSimpleName() + "?" + conditionalUrl);;
                } else {
                    entry.getRequest()
                            .setMethod(Bundle.HTTPVerb.POST)
                            .setUrl(entry.getResource().getClass().getSimpleName())
                            .setIfNoneExist(entry.getResource().getClass().getSimpleName() + "?" + conditionalUrl);
                }
            } else {
                entry.getRequest()
                        .setMethod(Bundle.HTTPVerb.POST)
                        .setUrl(entry.getResource().getClass().getSimpleName());
            }
        }

        exchange.getIn().setBody(ctx.newJsonParser().encodeResourceToString(bundle));
    }

    private String getConditional(Resource resource) {
        String result = null;

        if (resource instanceof Composition) {
            Composition res = (Composition) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifier());
            }
        }
        if (resource instanceof Condition) {
            Condition condition = (Condition) resource;
            if (condition.hasIdentifier()) {
                return getConditional(condition.getIdentifierFirstRep());
            }
        }
        if (resource instanceof DiagnosticReport) {
            DiagnosticReport res = (DiagnosticReport) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof DocumentReference) {
            DocumentReference res = (DocumentReference) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof Encounter) {
            Encounter encounter = (Encounter) resource;
            if (encounter.hasIdentifier()) {
                return getConditional(encounter.getIdentifierFirstRep());
            }
        }
        if (resource instanceof HealthcareService) {
            HealthcareService res = (HealthcareService) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof MedicationRequest) {
            MedicationRequest res = (MedicationRequest) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof MedicationDispense) {
            MedicationDispense res = (MedicationDispense) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof MedicationAdministration) {
            MedicationAdministration res = (MedicationAdministration) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }


        if (resource instanceof Location) {
            Location res = (Location) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof Observation) {
            Observation observation = (Observation) resource;
            if (observation.hasIdentifier()) {
                return getConditional(observation.getIdentifierFirstRep());
            }
        }
        if (resource instanceof Organization) {
            Organization res = (Organization) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof QuestionnaireResponse) {
            QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) resource;
            if (questionnaireResponse.hasIdentifier()) {
                return getConditional(questionnaireResponse.getIdentifier());
            }
        }
        if (resource instanceof Patient) {
            Patient patient = (Patient) resource;
            if (patient.hasIdentifier()) {
                return getConditional(patient.getIdentifierFirstRep());
            }
        }
        if (resource instanceof Practitioner) {
            Practitioner patient = (Practitioner) resource;
            if (patient.hasIdentifier()) {
                return getConditional(patient.getIdentifierFirstRep());
            }
        }
        if (resource instanceof PractitionerRole) {
            PractitionerRole res = (PractitionerRole) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof Procedure) {
            Procedure res = (Procedure) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }
        if (resource instanceof ServiceRequest) {
            ServiceRequest res = (ServiceRequest) resource;
            if (res.hasIdentifier()) {
                return getConditional(res.getIdentifierFirstRep());
            }
        }

        return result;
    }
    private String getConditional(Identifier identifier) {

        if (identifier.hasSystem()) return "identifier="+identifier.getSystem()+"|"+identifier.getValue().replace(' ','_');
        return "identifier="+identifier.getValue().replace(' ','_');
    }
}
