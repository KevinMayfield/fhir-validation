package uk.mayfieldis.hapifhir.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.*;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUnnamed;
import ca.uhn.fhir.util.ParametersUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import org.hl7.fhir.r4.model.ValueSet;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RemoteTerminologyServiceValidationSupportOnto extends BaseValidationSupport implements IValidationSupport {

    // 21/6/2020 KGM Needs to reflect https://github.com/hapifhir/org.hl7.fhir.core/blob/master/org.hl7.fhir.r4/src/main/java/org/hl7/fhir/r4/context/BaseWorkerContext.java

    private String myBaseUrl;
    private List<Object> myClientInterceptors = new ArrayList();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RemoteTerminologyServiceValidationSupportOnto.class);

    public static final String SNOMEDCT = "http://snomed.info/sct";

    public RemoteTerminologyServiceValidationSupportOnto(FhirContext theFhirContext) {
        super(theFhirContext);
    }

    public IValidationSupport.CodeValidationResult validateCode(IValidationSupport theRootValidationSupport, ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl) {
        LOG.warn("validateCode {}",theCodeSystem);
        return this.invokeRemoteValidateCode(theCodeSystem, theCode, theDisplay, theValueSetUrl, (IBaseResource)null);
    }

    private IGenericClient provideClient() {
        IGenericClient retVal = this.myCtx.newRestfulGenericClient(this.myBaseUrl);
        Iterator var2 = this.myClientInterceptors.iterator();

        while(var2.hasNext()) {
            Object next = var2.next();
            retVal.registerInterceptor(next);
        }

        return retVal;
    }

    public IValidationSupport.CodeValidationResult validateCodeInValueSet(IValidationSupport theRootValidationSupport, ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, @Nonnull IBaseResource theValueSet) {
        LOG.debug("validateCodeInValueSet {}",theCodeSystem);
        // Should try to validate locally? KGM

        return this.invokeRemoteValidateCode(theCodeSystem, theCode, theDisplay, (String)null, theValueSet);
    }

    @Override
    public ValueSetExpansionOutcome expandValueSet(IValidationSupport theRootValidationSupport, @Nullable ValueSetExpansionOptions theExpansionOptions, @Nonnull IBaseResource theValueSetToExpand) {
        LOG.error("expandValueSet");
        return null;
    }

    protected IValidationSupport.CodeValidationResult invokeRemoteValidateCode(String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl, IBaseResource theValueSet) {
        if (StringUtils.isBlank(theCode)) {
            return null;
        } else {
            IGenericClient client = this.provideClient();
            IBaseParameters input = ParametersUtil.newInstance(this.getFhirContext());
            if (StringUtils.isNotBlank(theValueSetUrl)) {
                ParametersUtil.addParameterToParametersUri(this.getFhirContext(), input, "url", theValueSetUrl);
            }

            ParametersUtil.addParameterToParametersString(this.getFhirContext(), input, "code", theCode);
            if (StringUtils.isNotBlank(theCodeSystem)) {
                ParametersUtil.addParameterToParametersUri(this.getFhirContext(), input, "system", theCodeSystem);
                if (theCodeSystem.equals(SNOMEDCT) && StringUtils.isNotBlank(FHIRServerProperties.getSnomedVersionUrl()) ) {
                    ParametersUtil.addParameterToParametersString(this.getFhirContext(), input, "systemVersion", FHIRServerProperties.getSnomedVersionUrl() );
                }
            }

            if (StringUtils.isNotBlank(theDisplay)) {

                ParametersUtil.addParameterToParametersString(this.getFhirContext(), input, "display", theDisplay);
            }

            if (theValueSet != null) {

                ParametersUtil.addParameterToParameters(this.getFhirContext(), input, "valueSet", theValueSet);
                theValueSetUrl = ((ValueSet) theValueSet).getUrl();

                if (theValueSet instanceof ValueSet && !StringUtils.isNotBlank(theCodeSystem)) {
                    ValueSet valueSet = (ValueSet) theValueSet;
                    ParametersUtil.addParameterToParametersUri(this.getFhirContext(), input, "system", valueSet.getCompose().getIncludeFirstRep().getSystem());
                }
            }

            IBaseParameters output = null;
            try {
                output = (IBaseParameters)((IOperationUnnamed)client.operation()
                        .onType("ValueSet"))
                        .named("validate-code")
                        .withParameters(input)
                        .execute();
            } catch (
                    Exception validationError
            ) {
                LOG.error("validateCode Error: {} {} {} {} Msg = {}",theCodeSystem, theCode, theDisplay, theValueSetUrl, validationError.getMessage());
                throw validationError;
            }
            List<String> resultValues = ParametersUtil.getNamedParameterValuesAsString(this.getFhirContext(), output, "result");

            if (resultValues.size() >= 1 && !StringUtils.isBlank((CharSequence)resultValues.get(0))) {
                Validate.isTrue(resultValues.size() == 1, "Response contained %d 'result' values", (long)resultValues.size());
                boolean success = "true".equalsIgnoreCase((String)resultValues.get(0));
                IValidationSupport.CodeValidationResult retVal = new IValidationSupport.CodeValidationResult();
                List displayValues;
                if (success) {
                    retVal.setCode(theCode);
                    displayValues = ParametersUtil.getNamedParameterValuesAsString(this.getFhirContext(), output, "display");
                    if (displayValues.size() > 0) {
                        retVal.setDisplay((String)displayValues.get(0));
                    }
                } else {
                    retVal.setSeverity(IValidationSupport.IssueSeverity.ERROR);
                    displayValues = ParametersUtil.getNamedParameterValuesAsString(this.getFhirContext(), output, "message");
                    if (displayValues.size() > 0) {
                        retVal.setMessage((String)displayValues.get(0));
                    }
                }

                return retVal;
            } else {
                return null;
            }
        }
    }

    public void setBaseUrl(String theBaseUrl) {
        Validate.notBlank(theBaseUrl, "theBaseUrl must be provided", new Object[0]);
        this.myBaseUrl = theBaseUrl;
    }

    public void addClientInterceptor(@Nonnull Object theClientInterceptor) {
        Validate.notNull(theClientInterceptor, "theClientInterceptor must not be null", new Object[0]);
        this.myClientInterceptors.add(theClientInterceptor);
    }
}
