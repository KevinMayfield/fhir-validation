package uk.mayfieldis.hapifhir.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.*;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.utilities.cache.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerFHIRValidation {

    FhirValidator validator;

    List<CapabilityStatement> capabilityStatements = null;
    List<MessageDefinition> messageDefinitions = null;
    FhirContext ctx;

  //  PackageCacheManager pcm;
    NpmPackage serverIgPackage;

    private static final Logger log = LoggerFactory.getLogger(ServerFHIRValidation.class);


    public ServerFHIRValidation(FhirValidator _validator, FhirContext _ctx, NpmPackage serverIgPackage) throws Exception
    {
        this.setup(_validator, _ctx, serverIgPackage );
    }

    public ServerFHIRValidation() {
    }

    public void setup(FhirValidator _validator, FhirContext _ctx, NpmPackage serverIgPackage) throws Exception {

        this.validator = _validator;
        this.ctx = _ctx;
        this.serverIgPackage = serverIgPackage;

        if (capabilityStatements ==null ) {
            // Load if not setup
            try {
                capabilityStatements = new ArrayList<>();
                for (String uri : serverIgPackage.listResources("CapabilityStatement")) {

                    CapabilityStatement capabilityStatement = (CapabilityStatement) ctx.newJsonParser().parseResource(serverIgPackage.load("package", uri));
                    capabilityStatements.add(capabilityStatement);
                }
                messageDefinitions = new ArrayList<>();

                for (String uri : serverIgPackage.listResources("MessageDefinition")) {

                    MessageDefinition messageDefinition = (MessageDefinition) ctx.newJsonParser().parseResource(serverIgPackage.load("package", uri));
                    messageDefinitions.add(messageDefinition);
                }
                for (String uri : serverIgPackage.list("package/examples")) {
                    if (uri.startsWith("MessageDefinition")) {
                        MessageDefinition messageDefinition = (MessageDefinition) ctx.newJsonParser().parseResource(serverIgPackage.load("package/examples", uri));
                        messageDefinitions.add(messageDefinition);
                    }
                }
                // For windows .... !!!
                for (String uri : serverIgPackage.list("package\\examples")) {
                    if (uri.startsWith("MessageDefinition")) {
                        MessageDefinition messageDefinition = (MessageDefinition) ctx.newJsonParser().parseResource(serverIgPackage.load("package\\examples", uri));
                        messageDefinitions.add(messageDefinition);
                    }
                }
            } catch  (IOException ioex) {
                log.error(ioex.getMessage());
                throw new InternalErrorException(ioex.getMessage());
            }
        }

    }

    public ValidationResult validateWithResult(IBaseResource theResource) {
        return validateWithResult(theResource, null);
    }
    public ValidationResult validateWithResult(IBaseResource resource, ValidationOptions validationOptions) {


        ValidationResult validationResult;
        try {
            validationResult = this.doValidate(resource, validationOptions);
        } catch (Exception var8) {
            log.error(var8.getMessage());
            if (var8 instanceof BaseServerResponseException) {
                throw (BaseServerResponseException)var8;
            }
            if (var8 instanceof IllegalArgumentException && var8.getCause() != null) {
                throw new UnprocessableEntityException(var8.getCause().getMessage());
            }

            throw new InternalErrorException(var8);
        }
        Iterator var11 = validationResult.getMessages().iterator();

        while(var11.hasNext()) {

            SingleValidationMessage next11 = (SingleValidationMessage)var11.next();

            if (next11.getMessage().contains("Observation obs-7")) continue;
            if (next11.getSeverity().ordinal() >= ResultSeverityEnum.ERROR.ordinal()) {
                throw new UnprocessableEntityException(ctx, validationResult.toOperationOutcome());
            } else {
                log.info(next11.getMessage());
            }
        }
        return validationResult;
    }


    private ValidationResult doValidate(IBaseResource resource, ValidationOptions validationOptions ) {
        if (resource instanceof Bundle) {
            // Bundle special case
            Bundle bundle = (Bundle) resource;
            if (bundle.getType().equals(Bundle.BundleType.MESSAGE)) {
                // Message Defintition validation
                if (capabilityStatements != null) {
                    ValidationResult result = checkMessageAcceptable(bundle);
                    if (result != null) return result;

                    // Use this to force entries to use specified profiles
                    // Hopefully not required in a messaging context
                    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                        if (entry.hasResource()) {
                            setProfile(entry.getResource());
                        }
                    }
                }
            }
            resource = bundle;
        } else {
            if (validationOptions == null || validationOptions.getProfiles().size() == 0) {
                // Force validation to use minimum acceptable profile
                setProfile(resource);
            } else {
                // Expected validationOptions to take care of this but resorting to old fashioned profile selection
                resource.getMeta().getProfile().clear();
                for (String profile : validationOptions.getProfiles()) {
                    resource.getMeta().addProfile(profile);
                }
            }
        }

        return validator.validateWithResult(resource, validationOptions);

    }

    private MessageDefinition findMatchingMessageDefinition(MessageHeader messageHeader) {
        for (CapabilityStatement statement: this.capabilityStatements) {
            for (CapabilityStatement.CapabilityStatementMessagingComponent component :statement.getMessaging()) {
                for (CapabilityStatement.CapabilityStatementMessagingSupportedMessageComponent supported: component.getSupportedMessage()) {
                    MessageDefinition messageDefinition = getMessageDefinition(messageHeader.getEventCoding().getSystem(), messageHeader.getEventCoding().getCode());
                    if (messageDefinition != null) {
                        return messageDefinition;
                    }
                }
            }
        }
        return null;
    }

    private MessageDefinition getMessageDefinition(String system, String code) {
        for (MessageDefinition messageDefinition : this.messageDefinitions) {
            if (messageDefinition.getEventCoding().getCode().equals(code)
                    && messageDefinition.getEventCoding().getSystem().equals(system)) {
                return messageDefinition;
            }
        }
        return null;
    }

    private ValidationResult checkMessageAcceptable(Bundle bundle) {
        MessageDefinition messageDefinition = null;
        if (bundle.hasEntry() && bundle.getType().equals(Bundle.BundleType.MESSAGE)) {
            if (bundle.getEntryFirstRep().getResource() instanceof MessageHeader) {
                MessageHeader messageHeader = (MessageHeader) bundle.getEntryFirstRep().getResource();
                messageDefinition = findMatchingMessageDefinition(messageHeader);
                if (messageDefinition == null) {
                    SingleValidationMessage message = new SingleValidationMessage();
                    message.setSeverity(ResultSeverityEnum.ERROR);
                    message.setMessage("Messages of type: system = "+messageHeader.getEventCoding().getSystem()
                            + " and code = " + messageHeader.getEventCoding().getCode()
                            + " are not accepted by this server");
                    List<SingleValidationMessage> messages = new ArrayList<>();
                    messages.add(message);
                    return new ValidationResult(ctx, messages);
                }
            } else {
                SingleValidationMessage message = new SingleValidationMessage();
                message.setSeverity(ResultSeverityEnum.ERROR);
                message.setMessage("First entry in the Message Bundle MUST be a MessageHeader");
                List<SingleValidationMessage> messages = new ArrayList<>();
                messages.add(message);
                return new ValidationResult(ctx, messages);
            }
        }
        // This section sets the meta.profile to be the one stated in the messageDefinition
        if ((bundle.hasEntry() && bundle.getType().equals(Bundle.BundleType.MESSAGE)) && messageDefinition != null) {
            if (bundle.getEntryFirstRep().getResource() instanceof MessageHeader) {
                MessageHeader messageHeader = (MessageHeader) bundle.getEntryFirstRep().getResource();
                for (Reference reference : messageHeader.getFocus()) {
                    for(Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                        if (entry.hasFullUrl() && entry.getFullUrl().equals(reference.getReference())) {
                            log.info("{}",entry.getResource().getClass().getSimpleName());
                            if (entry.getResource() instanceof DomainResource) {
                                // Need to add in
                                for (MessageDefinition.MessageDefinitionFocusComponent focusComponent : messageDefinition.getFocus()) {

                                    if (focusComponent.getCode().equals(entry.getResource().getClass().getSimpleName())
                                        && focusComponent.hasProfile()) {
                                        log.info("Forcing resource profile conformance (from MessageDefinition.focus) to {} for entry {}", focusComponent.getProfile(), entry.getFullUrl());
                                        (entry.getResource()).getMeta().addProfile(focusComponent.getProfile());
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void setProfile(IBaseResource resource) {
        for (CapabilityStatement capabilityStatement : capabilityStatements) {
            for (CapabilityStatement.CapabilityStatementRestComponent component : capabilityStatement.getRest()) {
                for (CapabilityStatement.CapabilityStatementRestResourceComponent restResource : component.getResource()) {
                    if (restResource.getType().equals(resource.getClass().getSimpleName()) && restResource.hasProfile()) {
                        log.info("Validating as {}", restResource.getProfile());
                        if (resource instanceof DomainResource) {
                            ((DomainResource) resource).getMeta().addProfile(restResource.getProfile());
                        }
                    }
                }
            }
        }
    }

    /*

    public static OperationOutcome removeUnsupportedIssues(OperationOutcome outcome) {



        List<OperationOutcome.OperationOutcomeIssueComponent> issueRemove = new ArrayList<>();
        for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            boolean remove = false;


            // Fault in profile?? Yes

            if (issue.getDiagnostics().contains("(fhirPath = true and (use memberOf 'https://fhir.hl7.org.uk/STU3/ValueSet/CareConnect-NameUse-1'))")) {
                remove = true;
            }

            if (issue.getDiagnostics().contains("Patient.name[official]")) {
                remove = true;
            }

            // Need to check further, poss hapi issue?

            if (issue.getDiagnostics().contains("Could not verify slice for profile https://fhir.nhs.uk/STU3/StructureDefinition")) {
                remove = true;
            }

            // Appears to be a fault in CareConnect profiles

            if (issue.getDiagnostics().contains("Could not match discriminator (code) for slice Observation")) {
                remove = true;
            }

            //
            // Logged as issue https://github.com/jamesagnew/hapi-fhir/issues/1235
           if (issue.getDiagnostics().contains("Entry isn't reachable by traversing from first Bundle entry")) {
        remove = true;
    }
            if (remove) {
        log.debug("Stripped {}", issue.getDiagnostics());
        issueRemove.add(issue);
    }
}
        outcome.getIssue().removeAll(issueRemove);
                return outcome;
                }
     */
}
