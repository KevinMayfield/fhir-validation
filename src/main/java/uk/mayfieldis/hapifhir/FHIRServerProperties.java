package uk.mayfieldis.hapifhir;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import com.google.common.annotations.VisibleForTesting;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class FHIRServerProperties {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FHIRServerProperties.class);

    static final String ALLOW_EXTERNAL_REFERENCES = "allow_external_references";
    static final String ALLOW_MULTIPLE_DELETE = "allow_multiple_delete";
    static final String ALLOW_PLACEHOLDER_REFERENCES = "allow_placeholder_references";
    static final String REUSE_CACHED_SEARCH_RESULTS_MILLIS = "reuse_cached_search_results_millis";

    static final String DEFAULT_ENCODING = "default_encoding";
    static final String DEFAULT_PAGE_SIZE = "default_page_size";
    static final String DEFAULT_PRETTY_PRINT = "default_pretty_print";
    static final String ETAG_SUPPORT = "etag_support";
    static final String FHIR_VERSION = "fhir_version";
    static final String HAPI_PROPERTIES = "hapi.properties";
    static final String LOGGER_ERROR_FORMAT = "logger.error_format";
    static final String LOGGER_FORMAT = "logger.format";
    static final String LOGGER_LOG_EXCEPTIONS = "logger.log_exceptions";
    static final String LOGGER_NAME = "logger.name";
    static final String MAX_FETCH_SIZE = "max_fetch_size";
    static final String MAX_PAGE_SIZE = "max_page_size";
    static final String SERVER_ADDRESS = "host_address";
    static final String SERVER_BASE = "server.base";
    static final String SERVER_ID = "server.id";
    static final String SERVER_NAME = "server.name";
    static final String SUBSCRIPTION_EMAIL_ENABLED = "subscription.email.enabled";
    static final String SUBSCRIPTION_RESTHOOK_ENABLED = "subscription.resthook.enabled";
    static final String SUBSCRIPTION_WEBSOCKET_ENABLED = "subscription.websocket.enabled";
    static final String CORS_ENABLED = "cors.enabled";
    static final String CORS_ALLOWED_ORIGIN = "cors.allowed_origin";
    static final String ALLOW_CONTAINS_SEARCHES = "allow_contains_searches";
    static final String ALLOW_OVERRIDE_DEFAULT_SEARCH_PARAMS = "allow_override_default_search_params";
    static final String EMAIL_FROM = "email.from";

    static final String SOFTWARE_NAME = "software.name";
    static final String SOFTWARE_VERSION = "software.version";
    static final String SOFTWARE_PUBLISHER = "software.publisher";

    static final String VALIDATION_FLAG = "validate.flag";
    static final String VALIDATION_SERVER = "validation.server";

    static final String APP_USER = "jolokia.username";
    static final String APP_PASSWORD = "jolokia.password";

    static final String SECURITY_OAUTH2 = "security.oauth2";
    static final String SECURITY_OAUTH2_SERVER = "security.oauth2.server";
    static final String SECURITY_OAUTH2_CONFIG = "security.oauth2.configuration.server";
    static final String SECURITY_OAUTH2_ALLOW_READONLY = "security.oauth2.allowReadOnly";
    static final String SECURITY_OAUTH2_SCOPE = "security.oauth2.scope";
    static final String SECURITY_SMART_SCOPE = "security.oauth2.smart";

    static final String ALLOW_CASCADING_DELETES = "allow_cascading_deletes";
    static final String ALLOWED_BUNDLE_TYPES = "allowed_bundle_types";
    static final String CORS_ALLOW_CREDENTIALS = "cors.allowCredentials";

    private static final String FILTER_SEARCH_ENABLED = "filter_search.enabled";
    private static final String GRAPHQL_ENABLED = "graphql.enabled";
    private static final String BULK_EXPORT_ENABLED = "bulk.export.enabled";
    public static final String EXPIRE_SEARCH_RESULTS_AFTER_MINS = "retain_cached_searches_mins";

    private static final String VALIDATE_REQUESTS_ENABLED = "validation.requests.enabled";
    private static final String VALIDATE_RESPONSES_ENABLED = "validation.responses.enabled";

    private static final String VALIDATE_SCHEMA = "validation.standard.schema";
    private static final String VALIDATE_SCHEMATRON = "validation.standard.schematron";

    public static final String CORE_IG_PACKAGE = "core.ig.package";
    public static final String CORE_IG_VERSION = "core.ig.version";
    public static final String CORE_IG_URL = "core.ig.url";

    public static final String CORE2_IG_PACKAGE = "core2.ig.package";
    public static final String CORE2_IG_VERSION = "core2.ig.version";
    public static final String CORE2_IG_URL = "core2.ig.url";

    public static final String CORE3_IG_PACKAGE = "core3.ig.package";
    public static final String CORE3_IG_VERSION = "core3.ig.version";
    public static final String CORE3_IG_URL = "core3.ig.url";

    public static final String SERVER_IG_DESCRIPTION = "server.ig.description";
    public static final String SERVER_IG_PACKAGE = "server.ig.package";
    public static final String SERVER_IG_VERSION = "server.ig.version";
    public static final String SERVER_IG_URL = "server.ig.url";

    static final String SNOMED_VERSION_URL = "terminology.snomed.version";
    static final String TERMINOLOGY_VALIDATION_FLAG = "terminology.validation.flag";
    static final String TERMINOLOGY_SERVER = "terminology.server";


    private static Properties properties;

    /*
     * Force the configuration to be reloaded
     */
    public static void forceReload() {
        properties = null;
        getProperties();
    }

    /**
     * This is mostly here for unit tests. Use the actual properties file
     * to set values
     */
    @VisibleForTesting
    public static void setProperty(String theKey, String theValue) {
        getProperties().setProperty(theKey, theValue);
    }

    public static Properties getProperties() {
        if (properties == null) {
            // Load the configurable properties file
            try (InputStream in = FHIRServerProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)){
                FHIRServerProperties.properties = new Properties();
                FHIRServerProperties.properties.load(in);
            } catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties", e);
            }

            Properties overrideProps = loadOverrideProperties();
            if(overrideProps != null) {
                properties.putAll(overrideProps);
            }
        }

        return properties;
    }

    /**
     * If a configuration file path is explicitly specified via -Dhapi.properties=<path>, the properties there will
     * be used to override the entries in the default hapi.properties file (currently under WEB-INF/classes)
     * @return properties loaded from the explicitly specified configuraiton file if there is one, or null otherwise.
     */
    private static Properties loadOverrideProperties() {
        String confFile = System.getProperty(HAPI_PROPERTIES);
        if(confFile != null) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(confFile));
                return props;
            }
            catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties file: " + confFile, e);
            }
        }

        return null;
    }

    private static String getProperty(String propertyName) {
        Properties properties = FHIRServerProperties.getProperties();
        log.trace("Looking for property = {}", propertyName);
        if (System.getenv(propertyName)!= null) {
            String value= System.getenv(propertyName);
            log.info("System Environment property Found {} = {}", propertyName, value);
            return value;
        }
        if (System.getProperty(propertyName)!= null) {
            String value= System.getProperty(propertyName);
            log.info("System Property Found {} = {}" , propertyName, value);
            return value;
        }
        if (properties != null) {
            return properties.getProperty(propertyName);
        }

        return null;
    }

    private static String getProperty(String propertyName, String defaultValue) {
        Properties properties = FHIRServerProperties.getProperties();

        if (properties != null) {
            String value = properties.getProperty(propertyName);

            if (value != null && value.length() > 0) {
                return value;
            }
        }

        return defaultValue;
    }

    private static Boolean getPropertyBoolean(String propertyName, Boolean defaultValue) {
        String value = FHIRServerProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private static Integer getPropertyInteger(String propertyName, Integer defaultValue) {
        String value = FHIRServerProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    private static <T extends Enum> T getPropertyEnum(String thePropertyName, Class<T> theEnumType, T theDefaultValue) {
        String value = getProperty(thePropertyName, theDefaultValue.name());
        return (T) Enum.valueOf(theEnumType, value);
    }

    public static FhirVersionEnum getFhirVersion() {
        String fhirVersionString = FHIRServerProperties.getProperty(FHIR_VERSION);

        if (fhirVersionString != null && fhirVersionString.length() > 0) {
            return FhirVersionEnum.valueOf(fhirVersionString);
        }

        return FhirVersionEnum.R4;
    }
    public static String getFhirVersionStr() {
        return FHIRServerProperties.getProperty(FHIR_VERSION);
    }

    public static ETagSupportEnum getEtagSupport() {
        String etagSupportString = FHIRServerProperties.getProperty(ETAG_SUPPORT);

        if (etagSupportString != null && etagSupportString.length() > 0) {
            return ETagSupportEnum.valueOf(etagSupportString);
        }

        return ETagSupportEnum.ENABLED;
    }

    public static EncodingEnum getDefaultEncoding() {
        String defaultEncodingString = FHIRServerProperties.getProperty(DEFAULT_ENCODING);

        if (defaultEncodingString != null && defaultEncodingString.length() > 0) {
            return EncodingEnum.valueOf(defaultEncodingString);
        }

        return EncodingEnum.JSON;
    }

    public static Boolean getDefaultPrettyPrint() {
        return FHIRServerProperties.getPropertyBoolean(DEFAULT_PRETTY_PRINT, true);
    }

    public static String getServerAddress() {
        return FHIRServerProperties.getProperty(SERVER_ADDRESS);
    }

    public static Integer getMaximumFetchSize() {
        return FHIRServerProperties.getPropertyInteger(MAX_FETCH_SIZE, Integer.MAX_VALUE);
    }




    public static Boolean getAllowExternalReferences() {
        return FHIRServerProperties.getPropertyBoolean(ALLOW_EXTERNAL_REFERENCES, false);
    }

    public static Boolean getExpungeEnabled() {
        return FHIRServerProperties.getPropertyBoolean("expunge_enabled", true);
    }


    public static Boolean getCorsEnabled() {
        return FHIRServerProperties.getPropertyBoolean(CORS_ENABLED, true);
    }

    public static String getCorsAllowedOrigin() {
        return FHIRServerProperties.getProperty(CORS_ALLOWED_ORIGIN, "*");
    }

    public static Integer getDefaultPageSize() {
        return FHIRServerProperties.getPropertyInteger(DEFAULT_PAGE_SIZE, 20);
    }

    public static Integer getMaximumPageSize() {
        return FHIRServerProperties.getPropertyInteger(MAX_PAGE_SIZE, 200);
    }

    public static String getServerBase() {
        return FHIRServerProperties.getProperty(SERVER_BASE, "/fhir");
    }

    public static String getServerName() {
        return FHIRServerProperties.getProperty(SERVER_NAME);
    }

    public static String getServerId() {
        return FHIRServerProperties.getProperty(SERVER_ID, "home");
    }

    public static Boolean getAllowPlaceholderReferences() {
        return FHIRServerProperties.getPropertyBoolean(ALLOW_PLACEHOLDER_REFERENCES, true);
    }

    public static Boolean getSubscriptionEmailEnabled() {
        return FHIRServerProperties.getPropertyBoolean(SUBSCRIPTION_EMAIL_ENABLED, false);
    }

    public static Boolean getSubscriptionRestHookEnabled() {
        return FHIRServerProperties.getPropertyBoolean(SUBSCRIPTION_RESTHOOK_ENABLED, false);
    }

    public static Boolean getSubscriptionWebsocketEnabled() {
        return FHIRServerProperties.getPropertyBoolean(SUBSCRIPTION_WEBSOCKET_ENABLED, false);
    }

    public static Boolean getAllowContainsSearches() {
        return FHIRServerProperties.getPropertyBoolean(ALLOW_CONTAINS_SEARCHES, true);
    }

    public static Boolean getAllowOverrideDefaultSearchParams() {
        return FHIRServerProperties.getPropertyBoolean(ALLOW_OVERRIDE_DEFAULT_SEARCH_PARAMS, true);
    }

    public static String getEmailFrom() {
        return FHIRServerProperties.getProperty(EMAIL_FROM, "some@test.com");
    }

    public static Boolean getEmailEnabled() {
        return FHIRServerProperties.getPropertyBoolean("email.enabled", false);
    }

    public static String getEmailHost() {
        return FHIRServerProperties.getProperty("email.host");
    }

    public static Integer getEmailPort() {
        return FHIRServerProperties.getPropertyInteger("email.port", 0);
    }

    public static String getEmailUsername() {
        return FHIRServerProperties.getProperty("email.username");
    }

    public static String getEmailPassword() {
        return FHIRServerProperties.getProperty("email.password");
    }

    public static Long getReuseCachedSearchResultsMillis() {
        String value = FHIRServerProperties.getProperty(REUSE_CACHED_SEARCH_RESULTS_MILLIS, "-1");
        return Long.valueOf(value);
    }

    public static String getSoftwareName() {
        return FHIRServerProperties.getProperty(SOFTWARE_NAME);
    }

    public static String getPublisher() {
        return FHIRServerProperties.getProperty(SOFTWARE_PUBLISHER);
    }


    public static String getSoftwareVersion() {
        return FHIRServerProperties.getProperty(SOFTWARE_VERSION);
    }

    public static String getServerIgDescription() {
        return FHIRServerProperties.getProperty(SERVER_IG_DESCRIPTION);
    }


    public static String getAppUser() {
        return FHIRServerProperties.getProperty(APP_USER);
    }

    public static String getAppPassword() {
        return FHIRServerProperties.getProperty(APP_PASSWORD);
    }


    public static boolean getSecurityOAuth2() {
        return FHIRServerProperties.getPropertyBoolean(SECURITY_OAUTH2, false);
    }


    public static boolean getSecurityOAuth2AllowReadOnly() {
        return FHIRServerProperties.getPropertyBoolean(SECURITY_OAUTH2_ALLOW_READONLY, false);
    }

    public static String getSecurityOAuth2RequiredScope() {
        return FHIRServerProperties.getProperty(SECURITY_OAUTH2_SCOPE);
    }

    public static String getSecurityOAuth2Config() {
        return FHIRServerProperties.getProperty(SECURITY_OAUTH2_CONFIG);
    }

    public static String getSecurityOAuth2Server() {
        return FHIRServerProperties.getProperty(SECURITY_OAUTH2_SERVER);
    }


    public static Long getExpireSearchResultsAfterMins() {
        String value = FHIRServerProperties.getProperty(EXPIRE_SEARCH_RESULTS_AFTER_MINS, "60");
        return Long.valueOf(value);
    }


    public static boolean getValidationFlag() {
        return FHIRServerProperties.getPropertyBoolean(VALIDATION_FLAG, false);
    }

    public static boolean getValidationSchemaFlag() {
        return FHIRServerProperties.getPropertyBoolean(VALIDATE_SCHEMA, false);
    }
    public static boolean getValidationSchemaTronFlag() {
        return FHIRServerProperties.getPropertyBoolean(VALIDATE_SCHEMATRON, false);
    }

    public static Boolean getCorsAllowedCredentials() {
        return FHIRServerProperties.getPropertyBoolean(CORS_ALLOW_CREDENTIALS, false);
    }

    public static boolean getValidateRequestsEnabled() {
        return FHIRServerProperties.getPropertyBoolean(VALIDATE_REQUESTS_ENABLED, false);
    }

    public static boolean getValidateResponsesEnabled() {
        return FHIRServerProperties.getPropertyBoolean(VALIDATE_RESPONSES_ENABLED, false);
    }

    public static boolean getFilterSearchEnabled() {
        return FHIRServerProperties.getPropertyBoolean(FILTER_SEARCH_ENABLED, true);
    }

    public static boolean getGraphqlEnabled() {
        return FHIRServerProperties.getPropertyBoolean(GRAPHQL_ENABLED, true);
    }


    public static  String getAllowedBundleTypes() {
        return FHIRServerProperties.getProperty(ALLOWED_BUNDLE_TYPES, "");
    }

    public static Boolean getAllowCascadingDeletes() {
        return FHIRServerProperties.getPropertyBoolean(ALLOW_CASCADING_DELETES, false);
    }

    public static boolean getBulkExportEnabled() {
        return FHIRServerProperties.getPropertyBoolean(BULK_EXPORT_ENABLED, true);
    }

    public static String getCoreIgPackage() {
        return FHIRServerProperties.getProperty(CORE_IG_PACKAGE,"");
    }
    public static String getCoreIgVersion() {
        return FHIRServerProperties.getProperty(CORE_IG_VERSION,"");
    }
    public static String getCoreIgUrl() {
        return FHIRServerProperties.getProperty(CORE_IG_URL,"");
    }

    public static String getCore2IgPackage() {
        return FHIRServerProperties.getProperty(CORE2_IG_PACKAGE,"");
    }
    public static String getCore2IgVersion() {
        return FHIRServerProperties.getProperty(CORE2_IG_VERSION,"");
    }
    public static String getCore2IgUrl() {
        return FHIRServerProperties.getProperty(CORE2_IG_URL,"");
    }

    public static String getCore3IgPackage() {
        return FHIRServerProperties.getProperty(CORE3_IG_PACKAGE,"");
    }
    public static String getCore3IgVersion() {
        return FHIRServerProperties.getProperty(CORE3_IG_VERSION,"");
    }
    public static String getCore3IgUrl() {
        return FHIRServerProperties.getProperty(CORE3_IG_URL,"");
    }

    public static String getServerIgPackage() {
        return FHIRServerProperties.getProperty(SERVER_IG_PACKAGE,"");
    }
    public static String getServerIgVersion() {
        return FHIRServerProperties.getProperty(SERVER_IG_VERSION,"");
    }
    public static String getServerIgUrl() {
        return FHIRServerProperties.getProperty(SERVER_IG_URL,"");
    }

    public static String getSnomedVersionUrl() {
        return FHIRServerProperties.getProperty(SNOMED_VERSION_URL);
    }
    public static String getTerminologyServer() {
        return FHIRServerProperties.getProperty(TERMINOLOGY_SERVER);
    }
    public static boolean getValidateTerminologyEnabled() {
        return FHIRServerProperties.getPropertyBoolean(TERMINOLOGY_VALIDATION_FLAG, false);
    }


    private static Properties loadProperties() {
        // Load the configurable properties file
        Properties properties;
        try (InputStream in = FHIRServerProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)) {
            properties = new Properties();
            properties.load(in);
        } catch (Exception e) {
            throw new ConfigurationException("Could not load HAPI properties", e);
        }

        Properties overrideProps = loadOverrideProperties();
        if (overrideProps != null) {
            properties.putAll(overrideProps);
        }
        return properties;
    }

    public static String getLoggerName() {
        return FHIRServerProperties.getProperty(LOGGER_NAME, "fhirtest.access");
    }

    public static String getLoggerFormat() {
        return FHIRServerProperties.getProperty(LOGGER_FORMAT, "Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
    }

    public static String getLoggerErrorFormat() {
        return FHIRServerProperties.getProperty(LOGGER_ERROR_FORMAT, "ERROR - ${requestVerb} ${requestUrl}");
    }

    public static Boolean getLoggerLogExceptions() {
        return FHIRServerProperties.getPropertyBoolean(LOGGER_LOG_EXCEPTIONS, true);
    }

}
