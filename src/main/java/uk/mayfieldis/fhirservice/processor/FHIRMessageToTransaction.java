package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import uk.mayfieldis.fhirservice.MessageConfig;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;

public class FHIRMessageToTransaction implements Processor {

    FhirContext ctx = null;

    public FHIRMessageToTransaction(FhirContext _ctx) {
        this.ctx = _ctx;
    }

    private static final Logger log = LoggerFactory.getLogger(FHIRMessageToTransaction.class);

    Bundle bundleAdditions = new Bundle();

    Bundle bundle;

    @Override
    public void process(Exchange exchange) throws Exception {

        // HAPI needs a transaction bundle to process the resources (if we posted the message bundle in we would get a bundle stored)

        Object body = exchange.getIn().getBody();
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

        Task task = null;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {

            if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest medicationRequest = (MedicationRequest) entry.getResource();
                if (task == null) {
                    task = new Task();
                    if (medicationRequest.hasGroupIdentifier())
                        task.setGroupIdentifier(medicationRequest.getGroupIdentifier());

                    switch (medicationRequest.getStatus()) {
                        case ACTIVE:
                            task.setStatus(Task.TaskStatus.REQUESTED);
                            break;
                        case CANCELLED:
                            task.setStatus(Task.TaskStatus.CANCELLED);
                            break;
                    }
                    task.setAuthoredOn(new Date());
                    if (medicationRequest.hasRequester()) {
                        task.setRequester(medicationRequest.getRequester());
                    }
                    if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasPerformer()) {
                        task.setOwner(medicationRequest.getDispenseRequest().getPerformer());
                    }
                    if (medicationRequest.hasStatusReason()) {
                        task.setStatusReason(medicationRequest.getStatusReason());
                    }
                }
                if (medicationRequest.hasIdentifier()) {
                    task.addInput().setValue(new Reference().setIdentifier(medicationRequest.getIdentifierFirstRep()));
                }
            }

        }
        if (task != null) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(task);
            UUID uuid = UUID.randomUUID();
            entry.setFullUrl("urn:uuid:"+uuid);
        }

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {

            String conditionalUrl = getConditional(entry.getResource());
           // inspect(entry.getResource().getClass());
            analyze( entry.getResource());
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
        // Add in any newly created entries
        for (Bundle.BundleEntryComponent entry : bundleAdditions.getEntry()) {
            bundle.addEntry(entry);
        }

        exchange.getIn().setBody(ctx.newJsonParser().encodeResourceToString(bundle));
    }


    public void analyze(Object obj){
        ReflectionUtils.doWithFields(obj.getClass(), field -> {

            field.setAccessible(true);
            if (field.get(obj) instanceof BackboneElement) {
                log.info("Backbone detected");
                analyze(field.get(obj));
            }
            if (field.get(obj) instanceof Reference) {
                Reference reference = (Reference) field.get(obj);
                if (reference.hasIdentifier()) {
                  //  System.out.println(reference.getIdentifier().getSystem() + " - " + reference.getIdentifier().getValue());
                    if (reference.getIdentifier().getSystem().equals("https://fhir.nhs.uk/Id/nhs-number") && !reference.hasReference()) {
                        Patient patient = new Patient();
                        patient.addIdentifier(reference.getIdentifier());
                        addEntry(bundleAdditions,patient,reference);
                    }
                    if (reference.getIdentifier().getSystem().equals("https://fhir.nhs.uk/Id/ods-organization-code") && !reference.hasReference()) {
                        Organization organization = new Organization();
                        organization.addIdentifier(reference.getIdentifier());
                        addEntry(bundleAdditions,organization,reference);
                    }
                }
            }
        });
    }

    private void addEntry(Bundle bundle, Resource resource, Reference reference) {

        String ifNoneExists = resource.getClass().getSimpleName() + "?" + getConditional(resource);
        Bundle.BundleEntryComponent entry = null;
        for (Bundle.BundleEntryComponent entryComponent : bundle.getEntry()) {
            if (entryComponent.hasRequest()
                    && entryComponent.getRequest().hasIfNoneExist()
                    && entryComponent.getRequest().getIfNoneExist().equals(ifNoneExists)) {
                entry = entryComponent;
                log.info("duplicate found in original bundle: "+ifNoneExists);
            }
        }
        for (Bundle.BundleEntryComponent entryComponent : bundleAdditions.getEntry()) {
            if (entryComponent.hasRequest()
                    && entryComponent.getRequest().hasIfNoneExist()
                    && entryComponent.getRequest().getIfNoneExist().equals(ifNoneExists)) {
                entry = entryComponent;
                log.info("duplicate found in additions bundle: "+ifNoneExists);
            }
        }
        if (entry == null) {
            entry = bundle.addEntry();
            entry.setResource(resource);
            UUID uuid = UUID.randomUUID();
            entry.setFullUrl("urn:uuid:"+uuid);
            entry.getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl(resource.getClass().getSimpleName())
                    .setIfNoneExist(ifNoneExists);
        }
        reference.setReference(entry.getFullUrl());
    }


    private String getConditional(Resource resource) {
        String result = null;
        // On reflection .... this is better than earlier version

        MessageHeader message;
        try {
            Method hasIdentifierMethod
                    = resource.getClass().getMethod("hasIdentifier");
            if ((Boolean) hasIdentifierMethod.invoke(resource)) {
                try {
                    Method getIdentifierFirstRep
                            = resource.getClass().getMethod("getIdentifierFirstRep");
                    Identifier identifier = (Identifier) getIdentifierFirstRep.invoke(resource);
                    if (identifier != null){
                        return getConditional(identifier);
                    }
                } catch (Exception fr) {
                    log.info("no getIdentifierFirstRep "+resource.getClass().getSimpleName());
                }
                try {
                    Method getIdentifier
                            = resource.getClass().getMethod("getIdentifier");
                    Identifier identifier = (Identifier) getIdentifier.invoke(resource);
                    if (identifier != null){
                        return getConditional(identifier);
                    }
                } catch (Exception fr) {
                    log.info("no getIdentifier "+resource.getClass().getSimpleName());
                }
            }
        } catch (Exception ex) {
            log.warn("No identifier - " + resource.getClass().getSimpleName());
        }

        return result;
    }

    private String getConditional(Identifier identifier) {
        if (identifier.hasSystem()) return "identifier="+identifier.getSystem()+"|"+identifier.getValue().replace(' ','_');
        return "identifier="+identifier.getValue().replace(' ','_');
    }
}
