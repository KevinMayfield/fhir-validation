package uk.mayfieldis.hapifhir.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.UriParam;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.rmi.ServerException;
import java.util.*;

public class TerminologyServerValidationSupport implements IValidationSupport {

    // Potentially obsolete but may need it as a STU3 conversion step 20/June/2020 KGM

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TerminologyServerValidationSupport.class);

    public static final String SNOMEDCT = "http://snomed.info/sct";

    private Map<String, CodeSystem> myCodeSystems;
    private Map<String, ValueSet> myValueSets;
    private List<String> notSupportedValueSet;
    private List<String> notSupportedCodeSystem;
    FhirContext ctx;
    IGenericClient client;



    public TerminologyServerValidationSupport(FhirContext ctx, String terminologyUri) throws ServerException {
        LOG.trace("IG Validation Support Constructor");

        LOG.info("Creating Terminology Server Client {}",terminologyUri);

        this.ctx = ctx;

        this.myCodeSystems = new HashMap();
        this.myValueSets = new HashMap<>();
        notSupportedValueSet = new ArrayList<>();
        notSupportedCodeSystem = new ArrayList<>();
        try {
            client = this.ctx.newRestfulGenericClient(terminologyUri);
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            throw new ServerException(ex.getMessage());
        }
    }

    @Override
    public FhirContext getFhirContext() {
        return this.ctx;
    }

    @Override
    public boolean isCodeSystemSupported(IValidationSupport theRootValidationSupport, String uri) {
        if (notSupportedCodeSystem.contains(uri)) return false;
        if (myCodeSystems.get(uri) != null) return true;
        return fetchCodeSystem(uri) != null;
    }


    @Override
    public CodeValidationResult validateCode(IValidationSupport theRootValidationSupport, ConceptValidationOptions theOptions, String theCodeSystem, String
    theCode, String theDisplay, String valueSetUri) {
        Parameters params = new Parameters();

        boolean validateCode = true;
        if (valueSetUri != null) {
            LOG.debug("ONTO UK ValidateCode [System {} ] [Code= {} ] [ValueSet= {} ]", theCodeSystem, theCode, valueSetUri);
            params.addParameter(
                    new Parameters.ParametersParameterComponent(
                            new StringType("url"))
                            .setValue(new StringType(valueSetUri)));
        } else {
            LOG.debug("ONTO UK ValidateCode [System {} ] [Code= {} ]", theCodeSystem, theCode);
            if (theCodeSystem.equals(SNOMEDCT)) {
                params.addParameter(
                        new Parameters.ParametersParameterComponent(
                                new StringType("url"))
                                .setValue(new StringType(FHIRServerProperties.getSnomedVersionUrl() + "?fhir_vs")));
            } else {
                validateCode = false;
            }
        }

        // To validate SNOMED we need to use the UK ValueSet else use CodeSystem
        // Unless the CodeSystem isn't supplied
        // TODO double check this work
        if (theOptions.isInferSystem()) {
            params.addParameter(
                    new Parameters.ParametersParameterComponent(
                            new StringType("system"))
                            .setValue(new StringType(theCodeSystem)));
        } else {
            if (valueSetUri != null && !valueSetUri.isEmpty()) {
                ValueSet vs = fetchValueSetCall(valueSetUri);
                if (vs != null
                        && vs.hasCompose()
                        && vs.getCompose().hasInclude()
                        && vs.getCompose().getInclude().size() == 1
                        && vs.getCompose().getIncludeFirstRep().hasSystem()) {
                    params.addParameter(
                            new Parameters.ParametersParameterComponent(
                                    new StringType("system"))
                                    .setValue(new StringType(vs.getCompose().getIncludeFirstRep().getSystem())));
                }


            }
        }

        if (theDisplay != null) {
            params.addParameter(
                    new Parameters.ParametersParameterComponent(
                            new StringType("display"))
                            .setValue(new StringType(theDisplay)));
        }

        params.addParameter(
                new Parameters.ParametersParameterComponent(
                        new StringType("code"))
                        .setValue(new StringType(theCode)));

        Parameters paramResult= null;
        if (validateCode) {
            try {
                paramResult = client
                        .operation()
                        .onType(ValueSet.class)
                        .named("validate-code")
                        .withParameters(params)
                        .returnResourceType(Parameters.class)
                        .useHttpGet()
                        .execute();
                if (paramResult != null) {
                    String message = null;
                    for (Parameters.ParametersParameterComponent param : paramResult.getParameter()) {
                        if (param.getName().equals("result")
                                && param.getValue() instanceof BooleanType) {
                            BooleanType bool = (BooleanType) param.getValue();
                            if (bool.booleanValue()) {

                                return new CodeValidationResult().setCode(theCode);
                            }

                        }
                        if (param.getName().equals("message")
                                && param.getValue() instanceof StringType) {
                            StringType paramValue = (StringType) param.getValue();
                            message = paramValue.getValue();

                        }
                    }
                    if (message != null) {
                        return new CodeValidationResult().setSeverity(IssueSeverity.WARNING).setMessage(message);
                    }
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
            }
        } else {
            try {
                paramResult = client
                        .operation()
                        .onType(CodeSystem.class)
                        .named("lookup")
                        .withParameters(params)
                        .returnResourceType(Parameters.class)
                        .useHttpGet()
                        .execute();
                if (paramResult != null) {

                    return new CodeValidationResult().setCode(theCode);
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
            }
        }

        return new CodeValidationResult().setSeverity(IssueSeverity.WARNING).setMessage("SNOMEDValidator Unknown code: " + theCodeSystem + " / " + theCode);
    }


    @Override
    public CodeValidationResult validateCodeInValueSet(IValidationSupport theRootValidationSupport, ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, @Nonnull IBaseResource theValueSet) {
        return null;
    }

    @Override
    public LookupCodeResult lookupCode(IValidationSupport theRootValidationSupport, String theSystem, String theCode) {
        return null;
    }

    @Override
    public boolean isValueSetSupported(IValidationSupport theRootValidationSupport, String uri) {
        if (notSupportedValueSet.contains(uri)) return false;
        if (myValueSets.get(uri) != null) return true;
        return fetchValueSet(uri) != null;
    }

    @Override
    public IBaseResource generateSnapshot(IValidationSupport theRootValidationSupport, IBaseResource theInput, String theUrl, String theWebUrl, String theProfileName) {
        return null;
    }

    @Override
    public void invalidateCaches() {

    }

    @Override
    public StructureDefinition fetchStructureDefinition(String s) {
        return null;
    }

    @Override
    public ValueSetExpansionOutcome expandValueSet(IValidationSupport theRootValidationSupport, @Nullable ValueSetExpansionOptions theExpansionOptions, @Nonnull IBaseResource theValueSetToExpand) {


        ValueSetExpansionOutcome expand = null;

        if (theValueSetToExpand instanceof ValueSet) {
            ValueSet valueSet = (ValueSet) theValueSetToExpand;
            ValueSet.ValueSetComposeComponent valueSetComposeComponent = valueSet.getCompose();
            for (ValueSet.ConceptSetComponent conceptSetComponent : valueSetComposeComponent.getInclude()) {
                for (ValueSet.ConceptSetFilterComponent filter : conceptSetComponent.getFilter()) {
                    if (conceptSetComponent.hasValueSet()) {
                        LOG.debug("SNOMED expandValueSet ValueSet= {}", conceptSetComponent.getValueSet().get(0).getValue());
                    } else {
                        LOG.debug("SNOMED expandValueSet System= {}", conceptSetComponent.getSystem());
                    }
                    if (filter.hasOp()) {

                        ValueSet vsExpansion = null;
                        switch (filter.getOp()) {
                            case IN:
                                LOG.debug("IN Filter detected - {}", filter.getValue());
                                vsExpansion = client
                                        .operation()
                                        .onType(ValueSet.class)
                                        .named("expand")
                                        .withSearchParameter(Parameters.class, "identifier", new UriParam(FHIRServerProperties.getSnomedVersionUrl() + "?fhir_vs=refset/" + filter.getValue()))
                                        .returnResourceType(ValueSet.class)
                                        .useHttpGet()
                                        .execute();
                                break;
                            case EQUAL:
                                LOG.debug("EQUAL Filter detected - {}", filter.getValue());
                                String url = FHIRServerProperties.getSnomedVersionUrl() + "?fhir_vs=ecl/" + filter.getValue();
                                LOG.debug(url);
                                url = url.replace("^", "%5E");
                                url = url.replace("|", "%7C");
                                url = url.replace("<", "%3C");
                                vsExpansion = client
                                        .operation()
                                        .onType(ValueSet.class)
                                        .named("expand")
                                        .withSearchParameter(Parameters.class, "identifier", new UriParam(url))
                                        .returnResourceType(ValueSet.class)
                                        .useHttpGet()
                                        .execute();
                            default:

                        }
                        if (vsExpansion != null) {

                            LOG.debug("EXPANSION RETURNED");
                            expand = new ValueSetExpansionOutcome(vsExpansion);

                        }
                    }
                }
            }
        }
        return expand;
    }


    @Override
    public List<StructureDefinition> fetchAllStructureDefinitions() {
        return Collections.emptyList();
    }

    @Override
    public CodeSystem fetchCodeSystem(String uri) {
        if (notSupportedCodeSystem.contains(uri)) return null;
        if (myCodeSystems.get(uri) != null) return myCodeSystems.get(uri);

        CodeSystem codeSystem = fetchCodeSystemCall(uri);
        if (codeSystem != null) {
            myCodeSystems.put(uri,codeSystem);
        } else {
            notSupportedCodeSystem.add(uri);
        }
        return codeSystem;
    }

    @Override
    public ValueSet fetchValueSet(String uri) {
        if (notSupportedValueSet.contains(uri)) return null;
        if (myValueSets.get(uri) != null) return myValueSets.get(uri);
        ValueSet valueSet = fetchValueSetCall(uri);
        if (valueSet != null) {
            myValueSets.put(uri,valueSet);
        } else {
            notSupportedValueSet.add(uri);
        }
        return valueSet;
    }



    @Override
    public List<IBaseResource> fetchAllConformanceResources() {
        ArrayList<IBaseResource> retVal = new ArrayList();
        retVal.addAll(this.myCodeSystems.values());
        retVal.addAll(this.myValueSets.values());
        return retVal;
    }

    @Override
    public <T extends IBaseResource> T fetchResource(Class<T> theClass, String theUri) {
        Validate.notBlank(theUri, "theUri must not be null or blank");
        if (theClass.equals(CodeSystem.class)) {
            return (T) this.fetchCodeSystem(theUri);
        } else if (theClass.equals(ValueSet.class)) {
            return (T) this.fetchValueSet(theUri);
        }
        return null;
    }

    private ValueSet fetchValueSetCall(String uri) {
        Bundle bundle = client.search().forResource(ValueSet.class).where(ValueSet.URL.matches().value(uri))
                .returnBundle(Bundle.class)
                .execute();
        if (bundle.hasEntry() && bundle.getEntryFirstRep().getResource() instanceof ValueSet) {
            LOG.debug("fetchValueSet OK {}} ",uri);
            return (ValueSet) bundle.getEntryFirstRep().getResource();
        } else {
            LOG.info("fetchValueSet MISSING {} ", uri);
        }

        return null;
    }

    private CodeSystem fetchCodeSystemCall(String uri) {
        Bundle results = null;
        try {
            results = client.search().forResource(CodeSystem.class).where(ValueSet.URL.matches().value(uri))
                    .returnBundle(Bundle.class)
                    .execute();
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        }
        if (results == null) return null;

        if (results.hasEntry() && results.getEntryFirstRep().getResource() instanceof CodeSystem) {
            LOG.debug("fetchCodeSystem OK {}", uri);
            return (CodeSystem) results.getEntryFirstRep().getResource();
        } else {
            LOG.info("fetchCodeSystem MISSING {}", uri);
        }

        return null;
    }
}
