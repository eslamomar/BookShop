package com.store.onlinebookstore.config;

import com.store.onlinebookstore.authentication.CustomAuthenticationProvider;
import com.store.onlinebookstore.authentication.CustomWebAuthenticationDetailsSource;
import com.store.onlinebookstore.repository.CustomerRepository;
import com.store.onlinebookstore.security.CustomAuthenticationFailureHandler;
import com.store.onlinebookstore.security.CustomAuthenticationSuccessHandler;
import com.store.onlinebookstore.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SpringSecurity {

    private final CustomUserDetailsService userDetailsService;
    private final CustomerRepository customerRepository;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomWebAuthenticationDetailsSource authenticationDetailsSource;

    public SpringSecurity(CustomUserDetailsService userDetailsService,
                          CustomerRepository customerRepository,
                          CustomAuthenticationSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler,
                          CustomWebAuthenticationDetailsSource authenticationDetailsSource) {
        this.userDetailsService = userDetailsService;
        this.customerRepository = customerRepository;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CustomAuthenticationProvider authenticationProvider() {
        CustomAuthenticationProvider provider = new CustomAuthenticationProvider(customerRepository);
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(authenticationProvider());

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/register/save",
                                "/qrcode", "/access-denied", "/2fa").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                        .requestMatchers("/admin/**", "/admin-panel",
                                "/books/add", "/books/edit/**", "/books/save", "/books/delete/**",
                                "/customers").hasRole("ADMIN")
                        .requestMatchers("/cart/**", "/checkout/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/account/**").authenticated()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .authenticationDetailsSource(authenticationDetailsSource)
                        .successHandler(successHandler)
                        .failureHandler(failureHandler) // Add failure handler here
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true")
                        .sessionRegistry(sessionRegistry())
                        .and()
                        .sessionFixation().migrateSession()
                        .invalidSessionUrl("/login?invalid=true")
                )

                .requiresChannel(channel ->
                        channel.anyRequest().requiresSecure()
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}