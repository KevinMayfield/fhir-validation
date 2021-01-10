package uk.mayfieldis.fhirservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception {



/*
// www.baeldung.com/spring-security-oauth-resource-server
        http
                .authorizeRequests(authz -> authz
                        .antMatchers(HttpMethod.GET, "/services/**").hasAuthority("SCOPE_read")
                        .antMatchers(HttpMethod.POST, "/services").hasAuthority("SCOPE_write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        */


        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS,"/**").permitAll()//allow CORS option calls
                .antMatchers("/services/**").permitAll()
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


}
