package com.minifb.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

  static final String CSP =
      "default-src 'self'; img-src 'self' blob: data:; object-src 'none'; frame-ancestors 'none'";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // API dùng Bearer; endpoint dùng cookie (/auth/refresh, /auth/logout) dựa vào
        // SameSite=Strict + chỉ nhận POST.
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN))
                    .frameOptions(f -> f.deny()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/media/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/error")
                    .permitAll()
                    // Chỉ tồn tại khi springdoc bật (profile dev)
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(11);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (!props.allowedOrigins().isEmpty()) {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(props.allowedOrigins());
      config.addAllowedMethod("*");
      config.addAllowedHeader("*");
      config.setAllowCredentials(true);
      source.registerCorsConfiguration("/api/**", config);
    }
    return source;
  }
}
