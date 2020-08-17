package uk.mayfieldis.hapifhir.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.common.hapi.validation.validator.ProfileKnowledgeWorkerR5;
import org.hl7.fhir.common.hapi.validation.validator.VersionSpecificWorkerContextWrapper;
import org.hl7.fhir.common.hapi.validation.validator.VersionTypeConverterDstu3;
import org.hl7.fhir.common.hapi.validation.validator.VersionTypeConverterR4;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.conformance.ProfileUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SnapshotGeneratingValidationSupport implements IValidationSupport {
    private static final Logger ourLog = LoggerFactory.getLogger(org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport.class);
    private final FhirContext myCtx;

    public SnapshotGeneratingValidationSupport(FhirContext theCtx) {
        Validate.notNull(theCtx);
        this.myCtx = theCtx;
    }

    public IBaseResource generateSnapshot(ValidationSupportContext theValidationSupportContext, IBaseResource theInput, String theUrl, String theWebUrl, String theProfileName) {
        String inputUrl = null;

        IBaseResource var9;
        try {
            assert theInput.getStructureFhirVersionEnum() == this.myCtx.getVersion().getVersion();

            Object converter;
            switch(theInput.getStructureFhirVersionEnum()) {
                case DSTU3:
                    converter = new VersionTypeConverterDstu3();
                    break;
                case R4:
                    converter = new VersionTypeConverterR4();
                    break;
                case R5:
                    converter = VersionSpecificWorkerContextWrapper.IDENTITY_VERSION_TYPE_CONVERTER;
                    break;
                case DSTU2:
                case DSTU2_HL7ORG:
                case DSTU2_1:
                default:
                    throw new IllegalStateException("Can not generate snapshot for version: " + theInput.getStructureFhirVersionEnum());
            }

            StructureDefinition inputCanonical = (StructureDefinition)((VersionSpecificWorkerContextWrapper.IVersionTypeConverter)converter).toCanonical(theInput);
            inputUrl = inputCanonical.getUrl();
            if (!theValidationSupportContext.getCurrentlyGeneratingSnapshots().contains(inputUrl)) {
                theValidationSupportContext.getCurrentlyGeneratingSnapshots().add(inputUrl);
                String baseDefinition = inputCanonical.getBaseDefinition();
                if (StringUtils.isBlank(baseDefinition)) {
                    throw new PreconditionFailedException("StructureDefinition[id=" + inputCanonical.getIdElement().getId() + ", url=" + inputCanonical.getUrl() + "] has no base");
                }

                IBaseResource base = theValidationSupportContext.getRootValidationSupport().fetchStructureDefinition(baseDefinition);
                if (base == null) {
                    throw new PreconditionFailedException("Unknown base definition: " + baseDefinition);
                }

                StructureDefinition baseCanonical = (StructureDefinition)((VersionSpecificWorkerContextWrapper.IVersionTypeConverter)converter).toCanonical(base);
                if (baseCanonical.getSnapshot().getElement().isEmpty()) {
                    theValidationSupportContext.getRootValidationSupport().generateSnapshot(theValidationSupportContext, base, (String)null, (String)null, (String)null);
                    baseCanonical = (StructureDefinition)((VersionSpecificWorkerContextWrapper.IVersionTypeConverter)converter).toCanonical(base);
                }

                ArrayList<ValidationMessage> messages = new ArrayList();
                ProfileUtilities.ProfileKnowledgeProvider profileKnowledgeProvider = new ProfileKnowledgeWorkerR5(this.myCtx);
                IWorkerContext context = new VersionSpecificWorkerContextWrapper(theValidationSupportContext, (VersionSpecificWorkerContextWrapper.IVersionTypeConverter)converter);
                ProfileUtilities profileUtilities = new ProfileUtilities(context, messages, profileKnowledgeProvider);
                profileUtilities.generateSnapshot(baseCanonical, inputCanonical, theUrl, theWebUrl, theProfileName);
                switch(theInput.getStructureFhirVersionEnum()) {
                    case DSTU3:
                        org.hl7.fhir.dstu3.model.StructureDefinition generatedDstu3 = (org.hl7.fhir.dstu3.model.StructureDefinition)((VersionSpecificWorkerContextWrapper.IVersionTypeConverter)converter).fromCanonical(inputCanonical);
                        ((org.hl7.fhir.dstu3.model.StructureDefinition)theInput).getSnapshot().getElement().clear();
                        ((org.hl7.fhir.dstu3.model.StructureDefinition)theInput).getSnapshot().getElement().addAll(generatedDstu3.getSnapshot().getElement());
                        break;
                    case R4:
                        org.hl7.fhir.r4.model.StructureDefinition generatedR4 = (org.hl7.fhir.r4.model.StructureDefinition)((VersionSpecificWorkerContextWrapper.IVersionTypeConverter)converter).fromCanonical(inputCanonical);
                        ((org.hl7.fhir.r4.model.StructureDefinition)theInput).getSnapshot().getElement().clear();
                        ((org.hl7.fhir.r4.model.StructureDefinition)theInput).getSnapshot().getElement().addAll(generatedR4.getSnapshot().getElement());
                        break;
                    case R5:
                        StructureDefinition generatedR5 = (StructureDefinition)((VersionSpecificWorkerContextWrapper.IVersionTypeConverter)converter).fromCanonical(inputCanonical);
                        ((StructureDefinition)theInput).getSnapshot().getElement().clear();
                        ((StructureDefinition)theInput).getSnapshot().getElement().addAll(generatedR5.getSnapshot().getElement());
                        break;
                    case DSTU2:
                    case DSTU2_HL7ORG:
                    case DSTU2_1:
                    default:
                        throw new IllegalStateException("Can not generate snapshot for version: " + theInput.getStructureFhirVersionEnum() );
                }

                IBaseResource var27 = theInput;
                return var27;
            }

            ourLog.warn("Detected circular dependency, already generating snapshot for: {} [{}]", inputUrl,  inputCanonical.getDerivation().getDisplay());
            var9 = theInput;
            inputUrl = null;
        } catch (BaseServerResponseException var23) {
            throw var23;
        } catch (Exception var24) {
            throw new InternalErrorException("Failed to generate snapshot", var24);
        } finally {
            if (inputUrl != null) {
                theValidationSupportContext.getCurrentlyGeneratingSnapshots().remove(inputUrl);
            }

        }

        return var9;
    }

    public FhirContext getFhirContext() {
        return this.myCtx;
    }
}
