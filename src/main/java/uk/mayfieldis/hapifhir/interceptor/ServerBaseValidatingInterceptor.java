package uk.mayfieldis.hapifhir.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import ca.uhn.fhir.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Interceptor
public abstract class ServerBaseValidatingInterceptor<T> {
    public static final String DEFAULT_RESPONSE_HEADER_VALUE = "${row}:${col} ${severity} ${message} (${location})";
    private static final Logger ourLog = LoggerFactory.getLogger(ca.uhn.fhir.rest.server.interceptor.BaseValidatingInterceptor.class);
    private Integer myAddResponseIssueHeaderOnSeverity = null;
    private Integer myAddResponseOutcomeHeaderOnSeverity = null;
    private Integer myFailOnSeverity;
    private boolean myIgnoreValidatorExceptions;
    private int myMaximumHeaderLength;
    private String myResponseIssueHeaderName;
    private String myResponseIssueHeaderValue;
    private String myResponseIssueHeaderValueNoIssues;
    private String myResponseOutcomeHeaderName;
    private List<IValidatorModule> myValidatorModules;

    public NpmPackage getNpm() {
        return npm;
    }

    public void setNpm(NpmPackage npm) {
        this.npm = npm;
    }

    public NpmPackage npm;

    public ServerBaseValidatingInterceptor() {
        this.myFailOnSeverity = ResultSeverityEnum.ERROR.ordinal();
        this.myMaximumHeaderLength = 200;
        this.myResponseIssueHeaderName = this.provideDefaultResponseHeaderName();
        this.myResponseIssueHeaderValue = "${row}:${col} ${severity} ${message} (${location})";
        this.myResponseIssueHeaderValueNoIssues = null;
        this.myResponseOutcomeHeaderName = this.provideDefaultResponseHeaderName();
    }

    private void addResponseIssueHeader(RequestDetails theRequestDetails, SingleValidationMessage theNext) {
        StrLookup<?> lookup = new MyLookup(theNext);
        StrSubstitutor subs = new StrSubstitutor(lookup, "${", "}", '\\');
        String headerValue = subs.replace(this.myResponseIssueHeaderValue);
        ourLog.trace("Adding header to response: {}", headerValue);
        theRequestDetails.getResponse().addHeader(this.myResponseIssueHeaderName, headerValue);
    }

    public ServerBaseValidatingInterceptor<T> addValidatorModule(IValidatorModule theModule) {
        Validate.notNull(theModule, "theModule must not be null", new Object[0]);
        if (this.getValidatorModules() == null) {
            this.setValidatorModules(new ArrayList());
        }

        this.getValidatorModules().add(theModule);
        return this;
    }

    abstract ValidationResult doValidate(FhirValidator var1, T var2);

    protected void fail(RequestDetails theRequestDetails, ValidationResult theValidationResult) {
        throw new UnprocessableEntityException(theRequestDetails.getServer().getFhirContext(), theValidationResult.toOperationOutcome());
    }

    public ResultSeverityEnum getAddResponseOutcomeHeaderOnSeverity() {
        return this.myAddResponseOutcomeHeaderOnSeverity != null ? ResultSeverityEnum.values()[this.myAddResponseOutcomeHeaderOnSeverity] : null;
    }

    public int getMaximumHeaderLength() {
        return this.myMaximumHeaderLength;
    }

    public String getResponseOutcomeHeaderName() {
        return this.myResponseOutcomeHeaderName;
    }

    public List<IValidatorModule> getValidatorModules() {
        return this.myValidatorModules;
    }

    public boolean isIgnoreValidatorExceptions() {
        return this.myIgnoreValidatorExceptions;
    }

    abstract String provideDefaultResponseHeaderName();

    public void setAddResponseHeaderOnSeverity(ResultSeverityEnum theSeverity) {
        this.myAddResponseIssueHeaderOnSeverity = theSeverity != null ? theSeverity.ordinal() : null;
    }

    public void setAddResponseOutcomeHeaderOnSeverity(ResultSeverityEnum theAddResponseOutcomeHeaderOnSeverity) {
        this.myAddResponseOutcomeHeaderOnSeverity = theAddResponseOutcomeHeaderOnSeverity != null ? theAddResponseOutcomeHeaderOnSeverity.ordinal() : null;
    }

    public void setFailOnSeverity(ResultSeverityEnum theSeverity) {
        this.myFailOnSeverity = theSeverity != null ? theSeverity.ordinal() : null;
    }

    public void setIgnoreValidatorExceptions(boolean theIgnoreValidatorExceptions) {
        this.myIgnoreValidatorExceptions = theIgnoreValidatorExceptions;
    }

    public void setMaximumHeaderLength(int theMaximumHeaderLength) {
        Validate.isTrue(theMaximumHeaderLength >= 100, "theMaximumHeadeerLength must be >= 100", new Object[0]);
        this.myMaximumHeaderLength = theMaximumHeaderLength;
    }

    protected void setResponseHeaderName(String theResponseHeaderName) {
        Validate.notBlank(theResponseHeaderName, "theResponseHeaderName must not be blank or null", new Object[0]);
        this.myResponseIssueHeaderName = theResponseHeaderName;
    }

    public void setResponseHeaderValue(String theResponseHeaderValue) {
        Validate.notBlank(theResponseHeaderValue, "theResponseHeaderValue must not be blank or null", new Object[0]);
        this.myResponseIssueHeaderValue = theResponseHeaderValue;
    }

    public void setResponseHeaderValueNoIssues(String theResponseHeaderValueNoIssues) {
        this.myResponseIssueHeaderValueNoIssues = theResponseHeaderValueNoIssues;
    }

    public void setResponseOutcomeHeaderName(String theResponseOutcomeHeaderName) {
        Validate.notEmpty(theResponseOutcomeHeaderName, "theResponseOutcomeHeaderName can not be empty or null", new Object[0]);
        this.myResponseOutcomeHeaderName = theResponseOutcomeHeaderName;
    }

    public void setValidatorModules(List<IValidatorModule> theValidatorModules) {
        this.myValidatorModules = theValidatorModules;
    }

    protected void postProcessResult(RequestDetails theRequestDetails, ValidationResult theValidationResult) {
    }

    protected void postProcessResultOnFailure(RequestDetails theRequestDetails, ValidationResult theValidationResult) {
    }

    protected ValidationResult validate(T theRequest, RequestDetails theRequestDetails) {
        FhirValidator validator = theRequestDetails.getServer().getFhirContext().newValidator();
        if (this.myValidatorModules != null) {
            Iterator var4 = this.myValidatorModules.iterator();

            while(var4.hasNext()) {
                IValidatorModule next = (IValidatorModule)var4.next();
                validator.registerValidatorModule(next);
            }
        }

        if (theRequest == null) {
            return null;
        } else {
            ValidationResult validationResult;
            try {
                validationResult = this.doValidate(validator, theRequest);
            } catch (Exception var8) {
                if (this.myIgnoreValidatorExceptions) {
                    ourLog.warn("Validator threw an exception during validation", var8);
                    return null;
                }

                if (var8 instanceof BaseServerResponseException) {
                    throw (BaseServerResponseException)var8;
                }

                throw new InternalErrorException(var8);
            }

            Iterator var6;
            SingleValidationMessage next;
            if (this.myAddResponseIssueHeaderOnSeverity != null) {
                boolean found = false;
                var6 = validationResult.getMessages().iterator();

                while(var6.hasNext()) {
                    next = (SingleValidationMessage)var6.next();
                    if (next.getSeverity().ordinal() >= this.myAddResponseIssueHeaderOnSeverity) {
                        this.addResponseIssueHeader(theRequestDetails, next);
                        found = true;
                    }
                }

                if (!found && StringUtils.isNotBlank(this.myResponseIssueHeaderValueNoIssues)) {
                    theRequestDetails.getResponse().addHeader(this.myResponseIssueHeaderName, this.myResponseIssueHeaderValueNoIssues);
                }
            }

            if (this.myFailOnSeverity != null) {
                Iterator var11 = validationResult.getMessages().iterator();

                while(var11.hasNext()) {
                    SingleValidationMessage next11 = (SingleValidationMessage)var11.next();
                    if (next11.getSeverity().ordinal() >= this.myFailOnSeverity) {
                        this.postProcessResultOnFailure(theRequestDetails, validationResult);
                        this.fail(theRequestDetails, validationResult);
                        return validationResult;
                    }
                }
            }

            if (this.myAddResponseOutcomeHeaderOnSeverity != null) {
                IBaseOperationOutcome outcome = null;
                var6 = validationResult.getMessages().iterator();

                while(var6.hasNext()) {
                    next = (SingleValidationMessage)var6.next();
                    if (next.getSeverity().ordinal() >= this.myAddResponseOutcomeHeaderOnSeverity) {
                        outcome = validationResult.toOperationOutcome();
                        break;
                    }
                }

                if (outcome == null && this.myAddResponseOutcomeHeaderOnSeverity != null && this.myAddResponseOutcomeHeaderOnSeverity == ResultSeverityEnum.INFORMATION.ordinal()) {
                    FhirContext ctx = theRequestDetails.getServer().getFhirContext();
                    outcome = OperationOutcomeUtil.newInstance(ctx);
                    OperationOutcomeUtil.addIssue(ctx, outcome, "information", "No issues detected", "", "informational");
                }

                if (outcome != null) {
                    IParser parser = theRequestDetails.getServer().getFhirContext().newJsonParser().setPrettyPrint(false);
                    String encoded = parser.encodeResourceToString(outcome);
                    if (encoded.length() > this.getMaximumHeaderLength()) {
                        encoded = encoded.substring(0, this.getMaximumHeaderLength() - 3) + "...";
                    }

                    theRequestDetails.getResponse().addHeader(this.myResponseOutcomeHeaderName, encoded);
                }
            }

            this.postProcessResult(theRequestDetails, validationResult);
            return validationResult;
        }
    }

    private static class MyLookup extends StrLookup<String> {
        private SingleValidationMessage myMessage;

        public MyLookup(SingleValidationMessage theMessage) {
            this.myMessage = theMessage;
        }

        public String lookup(String theKey) {
            if ("line".equals(theKey)) {
                return toString(this.myMessage.getLocationLine());
            } else if ("col".equals(theKey)) {
                return toString(this.myMessage.getLocationCol());
            } else if ("message".equals(theKey)) {
                return toString(this.myMessage.getMessage());
            } else if ("location".equals(theKey)) {
                return toString(this.myMessage.getLocationString());
            } else if ("severity".equals(theKey)) {
                return this.myMessage.getSeverity() != null ? this.myMessage.getSeverity().name() : null;
            } else {
                return "";
            }
        }

        private static String toString(Object theInt) {
            return theInt != null ? theInt.toString() : "";
        }
    }
}
