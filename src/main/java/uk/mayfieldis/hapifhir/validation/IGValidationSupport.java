package uk.mayfieldis.hapifhir.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.conformance.ProfileUtilities;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.cache.NpmPackage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class IGValidationSupport implements IValidationSupport
{

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IGValidationSupport.class);

    private Map<String, CodeSystem> myCodeSystems;
    private Map<String, StructureDefinition> myStructureDefinitions;
    private Map<String, ValueSet> myValueSets;
    FhirContext ctx;



    NpmPackage npm = null;

    public IGValidationSupport(FhirContext ctx, NpmPackage _npm) throws Exception {

        this.myCodeSystems = new HashMap();
        this.myValueSets = new HashMap<>();
        this.myStructureDefinitions = new HashMap<>();
        this.ctx = ctx;
        npm = _npm;

        LOG.info("Loading IG Validation Support {}", _npm.getPath());
        for (String resource : npm.listResources( "StructureDefinition")) {

            StructureDefinition structureDefinition = (StructureDefinition) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {} fhirVersion {}",structureDefinition.getUrl(), structureDefinition.getFhirVersion().toString());
            if (!structureDefinition.hasSnapshot() && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
                LOG.debug("Missing Snapshot {}", structureDefinition.getUrl());
            }
            this.myStructureDefinitions.put(structureDefinition.getUrl(),structureDefinition);
        }
        for (String resource : npm.listResources("ValueSet")) {
            ValueSet valueSet = (ValueSet) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {}", valueSet.getUrl());
            this.myValueSets.put(valueSet.getUrl(), valueSet);
        }
        for (String resource : npm.listResources("CodeSystem")) {
            CodeSystem codeSys = (CodeSystem) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {}", codeSys.getUrl());
            this.myCodeSystems.put(codeSys.getUrl(), codeSys);
        }

    }

    @Override
    public FhirContext getFhirContext() {
        return this.ctx;
    }

    @Override
    public ValueSetExpansionOutcome expandValueSet(IValidationSupport theRootValidationSupport, @Nullable ValueSetExpansionOptions theExpansionOptions, @Nonnull IBaseResource theValueSetToExpand) {
        LOG.error("expandValueSet");
        return null;
    }

    @Override
    public boolean isValueSetSupported(IValidationSupport theRootValidationSupport, String theValueSetUrl) {
        LOG.debug("isValueSetSupported {}",theValueSetUrl);
        return (this.fetchCodeSystemOrValueSet(theValueSetUrl, false) != null) ? true : false;
    }

    @Override
    public boolean isCodeSystemSupported(IValidationSupport theRootValidationSupport, String theSystem) {
        LOG.debug("isCodeSystemSupported {}",theSystem);
        return (this.fetchCodeSystemOrValueSet(theSystem, true) != null) ? true : false;
    }

    @Override
    public List<StructureDefinition> fetchAllStructureDefinitions() {
        LOG.debug("fetchAllStructureDefinitions ALL");
        return new ArrayList<>(this.myStructureDefinitions.values());
    }

    @Override
    public CodeSystem fetchCodeSystem(String theSystem) {
        LOG.debug("fetchCodeSystem {}",theSystem);
        return (CodeSystem)this.fetchCodeSystemOrValueSet(theSystem, true);
    }



    @Override
    public ValueSet fetchValueSet(String uri) {
        LOG.debug("fetchValueSet {}",uri);
        return (ValueSet)this.fetchCodeSystemOrValueSet( uri, false);
    }

    @Override
    public StructureDefinition fetchStructureDefinition(String url) {
        StructureDefinition structureDefinition = (StructureDefinition)this.myStructureDefinitions.get(url);

        return structureDefinition;
    }



    @Override
    public List<IBaseResource> fetchAllConformanceResources() {
        LOG.debug("fetchAllConformanceResources");
        ArrayList<IBaseResource> retVal = new ArrayList();
        retVal.addAll(this.myCodeSystems.values());
        retVal.addAll(this.myStructureDefinitions.values());
        retVal.addAll(this.myValueSets.values());
        return retVal;
    }


    @Override
    public <T extends IBaseResource> T fetchResource(Class<T> theClass, String theUri) {
        LOG.debug("fetchResource {} {} ",theUri, theClass.getSimpleName());
        Validate.notBlank(theUri, "theUri must not be null or blank", new Object[0]);
        if (theClass.equals(StructureDefinition.class)) {
            return (T) this.fetchStructureDefinition( theUri);
        } else {
            return !theClass.equals(ValueSet.class) && !theUri.startsWith("http://hl7.org/fhir/ValueSet/") ? null : (T) this.fetchValueSet(theUri);
        }
    }

    private DomainResource fetchCodeSystemOrValueSet(String theSystem, boolean codeSystem) {
        synchronized(this) {
            return codeSystem ? (DomainResource)((Map)this.myCodeSystems).get(theSystem) : (DomainResource)((Map)this.myValueSets).get(theSystem);
        }
    }

    public void createSnapshots(IWorkerContext context, IValidationSupport validationSupport) {

        ProfileUtilities tool = new ProfileUtilities(context, null, null);
        // This section first processes the level 2 profiles and the following section the level derived
        for (StructureDefinition structureDefinition : myStructureDefinitions.values()) {
            if (!structureDefinition.hasSnapshot()
                    && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)
                    && structureDefinition.getBaseDefinition().contains("http://hl7.org/fhir/")
            ) {
               buildSnapshot(validationSupport,tool,structureDefinition);
            }
        }
        for (StructureDefinition structureDefinition : myStructureDefinitions.values()) {
            if (!structureDefinition.hasSnapshot()
                    && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)
                    && !structureDefinition.getBaseDefinition().contains("http://hl7.org/fhir/")) {
                buildSnapshot(validationSupport,tool,structureDefinition);
            }
        }
    }
    public StructureDefinition buildSnapshot(IValidationSupport validationSupport, ProfileUtilities tool, StructureDefinition structureDefinition) {
        LOG.debug("Creating Snapshot {}", structureDefinition.getUrl());

        StructureDefinition base = (StructureDefinition) validationSupport.fetchStructureDefinition(structureDefinition.getBaseDefinition());
        if (base != null) {
            if (!base.hasSnapshot() && base.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
                LOG.warn("Base Missing Snapshot {}", base.getUrl());
                base = buildSnapshot(validationSupport,tool,base);
            }
            tool.generateSnapshot(base,
                    structureDefinition,
                    structureDefinition.getUrl(),
                    "https://fhir.nhs.uk/R4",
                    structureDefinition.getName());
            if (!structureDefinition.hasSnapshot() && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
                LOG.warn("Missing Snapshot {}", structureDefinition.getUrl());
            }
        } else {
            LOG.error("No base profile for {}",structureDefinition.getUrl());
        }
        return structureDefinition;
    }


}
