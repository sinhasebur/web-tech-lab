package com.example.profile;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public UserDetailsService userDetailsService(AccountRepository accounts) {
        return username -> {
            String passwordHash = accounts.findPasswordHash(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

            return User.withUsername(username).password(passwordHash).roles("USER").build();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/index.html", "/styles.css", "/app.js", "/csrf").permitAll()
                    .requestMatchers(HttpMethod.POST, "/register").permitAll()
                    .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Spring Security checks the login form and saves the login in the session.
                .formLogin(login -> login
                    .loginProcessingUrl("/login")
                    .successHandler((request, response, authentication) ->
                            response.setStatus(HttpStatus.NO_CONTENT.value()))
                    .failureHandler((request, response, error) ->
                            response.setStatus(HttpStatus.UNAUTHORIZED.value()))
                    .permitAll())
                .logout(logout -> logout.logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));

        return http.build();
    }
}
