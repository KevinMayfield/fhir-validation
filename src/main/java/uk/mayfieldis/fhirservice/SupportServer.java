package uk.mayfieldis.fhirservice;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.rest.client.api.IGenericClient;
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
    public ServerFHIRValidation getValidation(FhirValidator val,@Qualifier("r4ctx") FhirContext ctx, @Qualifier("serverIgPackage") NpmPackage serverIgPackage) throws Exception {
        return new ServerFHIRValidation(val,ctx,serverIgPackage);
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
            validationSupportChain.addValidationSupport(new RemoteTerminologyServiceValidationSupportOnto(r4ctx));
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
