package uk.mayfieldis.hapifhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.cache.NpmPackage;

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
            LOG.debug("Loading: {}",structureDefinition.getUrl());
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
    public List<StructureDefinition> fetchAllStructureDefinitions() {
        return new ArrayList<>(this.myStructureDefinitions.values());
    }

    @Override
    public CodeSystem fetchCodeSystem(String theSystem) {
        return (CodeSystem)this.fetchCodeSystemOrValueSet(theSystem, true);
    }



    @Override
    public ValueSet fetchValueSet(String uri) {
        return (ValueSet)this.fetchCodeSystemOrValueSet( uri, false);
    }

    @Override
    public StructureDefinition fetchStructureDefinition(String url) {
        return (StructureDefinition)this.myStructureDefinitions.get(url);
    }



    @Override
    public List<IBaseResource> fetchAllConformanceResources() {
        ArrayList<IBaseResource> retVal = new ArrayList();
        retVal.addAll(this.myCodeSystems.values());
        retVal.addAll(this.myStructureDefinitions.values());
        retVal.addAll(this.myValueSets.values());
        return retVal;
    }

    @Override
    public <T extends IBaseResource> T fetchResource(Class<T> theClass, String theUri) {
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


}
