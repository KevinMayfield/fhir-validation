package uk.mayfieldis.fhirservice;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.validation.FhirValidator;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.hapifhir.PackageManager;
import uk.mayfieldis.hapifhir.validation.NPMConformanceParser;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;
import uk.mayfieldis.hapifhir.validation.RemoteTerminologyServiceValidationSupportOnto;

import java.util.*;

//CHECKSTYLE:OFF

@SpringBootApplication(exclude = {
         ElasticsearchAutoConfiguration.class, DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ServletComponentScan()
@ComponentScan({"uk.mayfieldis.fhirservice", "uk.mayfieldis.hapifhir"})
public class SupportServer extends SpringBootServletInitializer {

    /**
     * A main method to start this application.
     */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SupportServer.class);

    FhirContext r4ctx;

    FhirContext r3ctx;

    @Autowired
    ApplicationContext context;

    public static void main(String[] args) {

        System.setProperty("hawtio.authenticationEnabled", "false");
        System.setProperty("management.security.enabled","false");
        System.setProperty("management.contextPath","");

        SpringApplication.run(SupportServer.class, args);
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Bean
    CamelContextConfiguration contextConfiguration() {

        return new CamelContextConfiguration() {

            @Override
            public void beforeApplicationStart(CamelContext camelContext) {

            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // Needs to be overridden - no action required

            }
        };
    }

    @Bean
    public IGenericClient fhirClient(FhirContext r4ctx) {
        String serverBase =  FHIRServerProperties.getFHIRServer();
        return r4ctx.newRestfulGenericClient(serverBase);
    }

    @Bean(name="r4ctx")
    public FhirContext fhirContext() {




        if (this.r4ctx == null) {
            LenientErrorHandler lenientErrorHandler = new LenientErrorHandler();
            lenientErrorHandler.setErrorOnInvalidValue(false);
            this.r4ctx = FhirContext.forR4();
            this.r4ctx.setParserErrorHandler(lenientErrorHandler);
        }
       return this.r4ctx;
    }

    @Bean(name="r3ctx")
    public FhirContext fhirStu3Context() {
        if (this.r3ctx == null) {
            this.r3ctx = FhirContext.forDstu3();
        }
        return this.r3ctx;
    }

    @Bean
    public FhirValidator fhirValidator(@Qualifier("r4ctx") FhirContext r4ctx) {

        return r4ctx.newValidator();
    }

    @Bean(name="serverIgPackage")
    public NpmPackage getServerIgPackage() throws Exception {
        NpmPackage serverIgPackage =null;

        if (!FHIRServerProperties.getServerIgPackage().isEmpty()) {
            serverIgPackage =
                    PackageManager.getPackage(FHIRServerProperties.getServerIgPackage(),
                    FHIRServerProperties.getServerIgVersion(),
                    FHIRServerProperties.getServerIgUrl());
            if (serverIgPackage== null)  throw new InternalErrorException("Unable to load API Server Conformance package");
        }
        return serverIgPackage;
    }

    @Bean(name="coreIgPackage")
    public NpmPackage getCoreIgPackage() throws Exception {
        NpmPackage validationIgPackage =null;

        if (!FHIRServerProperties.getCoreIgPackage().isEmpty()) {
            validationIgPackage = PackageManager.getPackage(FHIRServerProperties.getCoreIgPackage(),
                    FHIRServerProperties.getCoreIgVersion(),
                    FHIRServerProperties.getCoreIgUrl());
            if (validationIgPackage== null)  throw new InternalErrorException("Unable to load API Server Conformance package");
        }
        return validationIgPackage;
    }

    @Bean(name="core2IgPackage")
    public NpmPackage getCore2IgPackage() throws Exception {
        NpmPackage validationIgPackage =null;

        if (!FHIRServerProperties.getCore2IgPackage().isEmpty()) {
            validationIgPackage = PackageManager.getPackage(FHIRServerProperties.getCore2IgPackage(),
                    FHIRServerProperties.getCore2IgVersion(),
                    FHIRServerProperties.getCore2IgUrl());
            if (validationIgPackage== null)  throw new InternalErrorException("Unable to load API Server Conformance package");
        }
        return validationIgPackage;
    }

    @Bean(name="core3IgPackage")
    public NpmPackage getCore3IgPackage() throws Exception {
        NpmPackage validationIgPackage =null;

        if (!FHIRServerProperties.getCore3IgPackage().isEmpty()) {
            validationIgPackage = PackageManager.getPackage(FHIRServerProperties.getCore3IgPackage(),
                    FHIRServerProperties.getCore3IgVersion(),
                    FHIRServerProperties.getCore3IgUrl());
            if (validationIgPackage== null)  throw new InternalErrorException("Unable to load API Server Conformance package");
        }
        return validationIgPackage;
    }

    @Bean
    public ServerFHIRValidation getValidation(FhirValidator val,@Qualifier("r4ctx") FhirContext ctx, @Qualifier("coreIgPackage") NpmPackage validationIgPackage,
                                              @Qualifier("core2IgPackage") NpmPackage validation2IgPackage,
                                              @Qualifier("serverIgPackage") NpmPackage serverIgPackage) throws Exception {
        List<NpmPackage> packages = new ArrayList<>();
        if (serverIgPackage != null) packages.add(serverIgPackage);
        if (validationIgPackage != null) packages.add(validationIgPackage);
        if (validation2IgPackage != null) packages.add(validation2IgPackage);
       // if (validation3IgPackage != null) packages.add(validation3IgPackage);
        return new ServerFHIRValidation(val,ctx,packages);
    }

    @Bean
    public FhirInstanceValidator fhirInstanceValidator (FhirValidator val, FhirContext r4ctx,
                                                        @Qualifier("coreIgPackage") NpmPackage validationIgPackage,
                                                        @Qualifier("core2IgPackage") NpmPackage validation2IgPackage,
                                                        @Qualifier("core3IgPackage") NpmPackage validation3IgPackage,
                                                        @Qualifier("serverIgPackage") NpmPackage serverIgPackage) throws Exception {

        ValidationSupportChain
                validationSupportChain = new ValidationSupportChain();


        DefaultProfileValidationSupport defaultProfileValidationSupport = new DefaultProfileValidationSupport(r4ctx);
        validationSupportChain.addValidationSupport(defaultProfileValidationSupport);

        // Ideally Terminology Server needs to run first to provide code validation
        if (FHIRServerProperties.getValidateTerminologyEnabled() && !FHIRServerProperties.getTerminologyServer().isEmpty()) {
            log.debug("Remote Terminology Support");
            RemoteTerminologyServiceValidationSupport remoteTerminologyServiceValidationSupport = new RemoteTerminologyServiceValidationSupport(r4ctx);
            remoteTerminologyServiceValidationSupport.setBaseUrl(FHIRServerProperties.getTerminologyServer());

           // String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJxbmg2ZDBrUWFqTDhYY01IXzgxdFFFd1pWV1daNXpkMnFPbF9vY0g5LTVJIn0.eyJleHAiOjE2MTc3NzU3NzQsImlhdCI6MTYxNzc3Mzk3NCwianRpIjoiOTI0YmVhMGMtOTMzYi00NWU4LWE0NWUtZjQ1OTVmOGEwY2EzIiwiaXNzIjoiaHR0cHM6Ly9vbnRvbG9neS5uaHMudWsvYXV0aG9yaXNhdGlvbi9hdXRoL3JlYWxtcy9uaHMtZGlnaXRhbC10ZXJtaW5vbG9neSIsImF1ZCI6WyJodHRwczovL29udG9sb2d5Lm5ocy51ay9wcm9kdWN0aW9uMS9maGlyIiwiaHR0cHM6Ly9vbnRvbG9neS5uaHMudWsvYXV0aG9yaW5nL2ZoaXIiXSwic3ViIjoiNTM3MzIxNzctYTNhZC00NDcyLWFkZTEtOWFlMGI4YzI4NzI1IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiTkhTRF9LZXZpbl9NYXlmaWVsZCIsInNlc3Npb25fc3RhdGUiOiIwZGMyNGNjNS1mYTViLTQ3NGItYTU4OC1hYmI1Njc5Yjc2Y2QiLCJhY3IiOiIxIiwic2NvcGUiOiJyZXN0cmljdGVkLXJlYWQiLCJjbGllbnRIb3N0IjoiMTAuMy4yMTAuNCIsImNsaWVudElkIjoiTkhTRF9LZXZpbl9NYXlmaWVsZCIsImNsaWVudEFkZHJlc3MiOiIxMC4zLjIxMC40IiwiYXV0aG9yaXRpZXMiOlsiaHR0cHM6Ly9vbnRvbG9neS5uaHMudWsvcHJvZHVjdGlvbjEvZmhpckZISVJfUkVBRCIsImh0dHBzOi8vb250b2xvZ3kubmhzLnVrL3Byb2R1Y3Rpb24xL2ZoaXJTWU5EX1JFQUQiLCJodHRwczovL29udG9sb2d5Lm5ocy51ay9hdXRob3JpbmcvZmhpckZISVJfV1JJVEUiLCJodHRwczovL29udG9sb2d5Lm5ocy51ay9hdXRob3JpbmcvZmhpckZISVJfUkVBRCIsImh0dHBzOi8vb250b2xvZ3kubmhzLnVrL2F1dGhvcmluZy9maGlyU1lORF9SRUFEIiwiQ29uc3VtZXIiLCJBdXRob3IiLCJQRVJNX3Jlc3RyaWN0ZWRfUkVBRCJdfQ.bLyGo8Y1WcO30nsYOf0SNCVG_Ym0UA5iUiiEQeIOT_fWqOSH-CIPCnFUzQyCW77dMYriboGJKNS_nMXqvpvXA-OqevsSLQaUbrOLRGRZ1Yp1pDhnLfb42f3G1xxp2LdKc033RvBUxjUtYdTV1_DUcfpRLUmQ-yk_VBEfGz1EBZMrdLPuYKxs2BjOEHKOCSSfJxnpiFWojwsF67rTZUUCBvIJy_IWhObHEHCPORmqcoHgHuuTXY_D4mNuRvwhnwOlVSAUpM7wcg1I0PLM9H7yutfIYwEfqxM5leHqNLlYbVhoGUT7Blftcdq0ye-JMjTODhkgRxhXN9QruzTybbkBDw";

           // BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);
          //  remoteTerminologyServiceValidationSupportOnto.addClientInterceptor(authInterceptor);

            validationSupportChain.addValidationSupport(remoteTerminologyServiceValidationSupport);
        } else {
            log.debug("In memory Terminology Support");
            validationSupportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(r4ctx));
        }
        validationSupportChain.addValidationSupport(new SnapshotGeneratingValidationSupport(r4ctx));

        if (validationIgPackage !=null) {
            PrePopulatedValidationSupport igValidationSupport= NPMConformanceParser.getPrePopulatedValidationSupport(r4ctx, validationIgPackage);
            validationSupportChain.addValidationSupport(igValidationSupport);

        }
        if (validation2IgPackage !=null) {
            PrePopulatedValidationSupport igValidationSupport = NPMConformanceParser.getPrePopulatedValidationSupport(r4ctx, validation2IgPackage);
            validationSupportChain.addValidationSupport(igValidationSupport);

        }
        if (validation3IgPackage !=null) {
            PrePopulatedValidationSupport igValidationSupport = NPMConformanceParser.getPrePopulatedValidationSupport(r4ctx, validation3IgPackage);
            validationSupportChain.addValidationSupport(igValidationSupport);
        }
        if (serverIgPackage !=null) {
            PrePopulatedValidationSupport igValidationSupport = NPMConformanceParser.getPrePopulatedValidationSupport(r4ctx, serverIgPackage);
            validationSupportChain.addValidationSupport(igValidationSupport);
        }

        /*
        IWorkerContext context = new HapiWorkerContext(r4ctx,validationSupportChain);
        defaultProfileValidationSupport.createSnapshots(context,validationSupportChain);
*/
        // We try the above validators first

        val.setValidateAgainstStandardSchema(FHIRServerProperties.getValidationSchemaFlag());

        val.setValidateAgainstStandardSchematron(FHIRServerProperties.getValidationSchemaFlag());

        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(r4ctx);
        val.registerValidatorModule(instanceValidator);

        instanceValidator.setValidationSupport(validationSupportChain);

        return instanceValidator;


    }


    @Bean
    public ServletRegistrationBean FHIRServerR4RegistrationBean(@Qualifier("r4ctx") FhirContext ctx, @Qualifier("serverIgPackage") NpmPackage serverIgPackage) {

        ServletRegistrationBean registration = new ServletRegistrationBean(new FHIRR4RestfulServer(context, ctx, serverIgPackage), "/R4/*");
        Map<String,String> params = new HashMap<>();
        params.put("FhirVersion","R4");
        params.put("ImplementationDescription","FHIR Validation Server");
        registration.setInitParameters(params);
        registration.setName("FhirR4Servlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean
    public ServletRegistrationBean FHIRServerR3RegistrationBean(@Qualifier("r3ctx") FhirContext ctx, @Qualifier("serverIgPackage") NpmPackage serverIgPackage) {

        ServletRegistrationBean registration = new ServletRegistrationBean(new FHIRR3RestfulServer(context, ctx, null), "/STU3/*");
        Map<String,String> params = new HashMap<>();
        params.put("FhirVersion","R4");
        params.put("ImplementationDescription","FHIR Conversion Server");
        registration.setInitParameters(params);
        registration.setName("FhirR3Servlet");
        registration.setLoadOnStartup(2);
        return registration;
    }

}
//CHECKSTYLE:ON
