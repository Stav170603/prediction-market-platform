package com.virtualmarket.polymarket.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors().and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/markets", "/api/markets/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/markets/*/history", "/api/markets/*/statistics").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/summary").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/events/stream").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trades/by-market/*").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/markets/admin/markets",
                                "/api/markets/resolve",
                                "/api/markets/*/cancel"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/trades/**", "/api/positions/**", "/api/wallets/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
