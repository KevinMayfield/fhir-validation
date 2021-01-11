package uk.mayfieldis.fhirservice;

import com.google.common.collect.ImmutableList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception {



/*
// www.baeldung.com/spring-security-oauth-resource-server

 */
        http.cors();

        http
                .authorizeRequests(authz -> authz
                        .antMatchers(HttpMethod.GET, "/services/**").hasAuthority("SCOPE_patient/*.*")
                        .antMatchers(HttpMethod.POST, "/services").hasAuthority("SCOPE_patient/*.*")
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        http
                //.csrf().disable()
                .authorizeRequests()
                .antMatchers("/error").permitAll()
                .antMatchers("/R4/**").permitAll()
                .anyRequest().authenticated();
      /*
        http
                .authorizeRequests()
                .antMatchers("/").permitAll().and().csrf().disable();

        http
                .authorizeRequests()
                .antMatchers("/error").permitAll()
                .antMatchers("/jolokia/**").hasRole("ACTUATOR")
                .antMatchers("/hawtio/**").hasRole("ACTUATOR")
                .and().httpBasic();
*/
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

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
            config.addAllowedOrigin(o);
        });
        config.addAllowedOrigin(FHIRServerProperties.getCorsAllowedOrigin());

        config.addExposedHeader("Location");
        config.addExposedHeader("Content-Location");
        config.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        config.setAllowCredentials(FHIRServerProperties.getCorsAllowedCredentials());

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


}
