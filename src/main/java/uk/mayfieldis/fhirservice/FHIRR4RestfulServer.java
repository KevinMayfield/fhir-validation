package uk.mayfieldis.fhirservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.util.VersionUtil;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import uk.mayfieldis.fhirservice.provider.TaskProvider;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.hapifhir.interceptor.oauth2.OAuth2Interceptor;
import uk.mayfieldis.hapifhir.provider.ServerPlainProvider;
import uk.mayfieldis.hapifhir.provider.ServerProcessMessageProvider;
import uk.mayfieldis.hapifhir.provider.ServerValidationProvider;
import uk.mayfieldis.hapifhir.provider.IGConformanceProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class FHIRR4RestfulServer extends RestfulServer {

	private static final long serialVersionUID = 1L;
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FHIRR4RestfulServer.class);

	private ApplicationContext applicationContext;

	private NpmPackage serverIgPackage;

	private FhirContext ctx;


	public FHIRR4RestfulServer(ApplicationContext context,
							   FhirContext ctx,
							   NpmPackage serverIgPackage) {
		this.applicationContext = context;
		this.serverIgPackage = serverIgPackage;
		this.ctx = ctx;
	}

    @Override
	public void addHeadersToResponse(HttpServletResponse theHttpResponse) {
		theHttpResponse.addHeader("X-Powered-By", "HAPI FHIR " + VersionUtil.getVersion() + " RESTful Server (INTEROPen Care Connect STU3)");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		super.initialize();
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));


		setFhirContext(ctx);

		String serverBase =
				FHIRServerProperties.getServerBase();
	     if (serverBase != null && !serverBase.isEmpty()) {
            setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBase));
        }


		List<IResourceProvider> resourceProviders = new ArrayList<>();
		// add resource providers here
		setResourceProviders(resourceProviders);

		List<Object> plainProviders=new ArrayList<Object>();

		plainProviders.add(applicationContext.getBean(ServerValidationProvider.class));
		plainProviders.add(applicationContext.getBean(ServerPlainProvider.class));
		plainProviders.add(applicationContext.getBean(ServerProcessMessageProvider.class));
		plainProviders.add(applicationContext.getBean(TaskProvider.class));

		registerProviders(plainProviders);

		IGConformanceProvider confProvider = new IGConformanceProvider(this,  serverIgPackage, ctx);
		setServerConformanceProvider(confProvider);

		setServerName(FHIRServerProperties.getServerName());
		setServerVersion(FHIRServerProperties.getSoftwareVersion());
		setImplementationDescription(FHIRServerProperties.getServerName());

		if (FHIRServerProperties.getSecurityOAuth2()) {
			try {
				OAuth2Interceptor oAuth2Interceptor = new OAuth2Interceptor(applicationContext);
				getInterceptorService().registerInterceptor(oAuth2Interceptor);
			} catch (Exception ex) {
				log.error(ex.getMessage());
				throw new ServletException(ex.getMessage());
			}

		}


		if (FHIRServerProperties.getCorsEnabled()) {

			/// Consider moving this to SpringSecurityConfiguration

			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedHeader(HttpHeaders.ORIGIN);
			config.addAllowedHeader(HttpHeaders.ACCEPT);
			config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
			config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
			config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
			config.addAllowedHeader("x-fhir-starter");
			config.addAllowedHeader("X-Requested-With");
			config.addAllowedHeader("Prefer");
			String allAllowedCORSOrigins = FHIRServerProperties.getCorsAllowedOrigin();
			Arrays.stream(allAllowedCORSOrigins.split(",")).forEach(o -> {
			// KGM 	config.addAllowedOrigin(o);
			});
			//config.addAllowedOrigin(FHIRServerProperties.getCorsAllowedOrigin());

			config.addExposedHeader("Location");
			config.addExposedHeader("Content-Location");
			config.setAllowedMethods(
					Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
			config.setAllowCredentials(FHIRServerProperties.getCorsAllowedCredentials());

			// Create the interceptor and register it
			CorsInterceptor interceptor = new CorsInterceptor(config);
			registerInterceptor(interceptor);
		}


		LoggingInterceptor
				loggingInterceptor = new LoggingInterceptor();
		loggingInterceptor.setLoggerName(FHIRServerProperties.getLoggerName());
		loggingInterceptor.setMessageFormat(FHIRServerProperties.getLoggerFormat());
		loggingInterceptor.setErrorMessageFormat(FHIRServerProperties.getLoggerErrorFormat());
		loggingInterceptor.setLogExceptions(FHIRServerProperties.getLoggerLogExceptions());
		this.registerInterceptor(loggingInterceptor);


		FifoMemoryPagingProvider pp = new FifoMemoryPagingProvider(10);
		pp.setDefaultPageSize(10);
		pp.setMaximumPageSize(100);
		setPagingProvider(pp);

		setDefaultPrettyPrint(true);
		setDefaultResponseEncoding(EncodingEnum.JSON);

	}




}
