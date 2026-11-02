package com.company.cms.config;

import com.company.cms.security.JwtAuthFilter;
import com.company.cms.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.plot.ly; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; img-src 'self' data:; connect-src 'self'; frame-ancestors 'self';"
                ))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/auth/login",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/h2-console/**",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/",
                    "/*.html",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/vendor/**",
                    "/favicon.ico"
                ).permitAll()

                // User management (Admin only)
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Departments
                .requestMatchers(HttpMethod.GET, "/api/departments/**").authenticated()
                .requestMatchers("/api/departments/**").hasRole("ADMIN")

                // Employees
                .requestMatchers(HttpMethod.GET, "/api/employees/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/employees/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/employees/**").hasRole("ADMIN")

                // Projects
                .requestMatchers(HttpMethod.DELETE, "/api/projects/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/projects/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/projects/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/projects/**").authenticated()

                // Customers
                .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
                .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "MANAGER")

                // Products
                .requestMatchers(HttpMethod.GET, "/api/products/**").authenticated()
                .requestMatchers("/api/products/**").hasAnyRole("ADMIN", "MANAGER")

                // Sales Orders
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/orders/**").authenticated()

                // Feedback (Public intake + Admin/Manager moderation)
                .requestMatchers(HttpMethod.POST, "/api/feedback/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/feedback/public/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/feedback/**").hasRole("ADMIN")
                .requestMatchers("/api/feedback/**").hasAnyRole("ADMIN", "MANAGER")

                // Analytics
                .requestMatchers("/api/analytics/internal/**").permitAll()
                .requestMatchers("/api/analytics/etl/**").hasRole("ADMIN")
                .requestMatchers("/api/analytics/my-performance").authenticated()
                .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "MANAGER")

                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
