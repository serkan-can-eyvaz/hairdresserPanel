package com.example.barber.automation.config;

import com.example.barber.automation.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security konfigürasyonu
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF'yi devre dışı bırak (JWT kullanıyoruz)
            .csrf(csrf -> csrf.disable())
            
            // CORS ayarları
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Session yönetimi - Stateless (JWT kullanıyoruz)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (login, health check, webhook vb.)
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/whatsapp/webhook/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Admin endpoints - Sadece SUPER_ADMIN erişebilir
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                
                // Tenant endpoints - SUPER_ADMIN ve TENANT_ADMIN erişebilir
                .requestMatchers("/tenant/**").hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN")
                
                // Appointment endpoints - Tüm authenticated kullanıcılar
                .requestMatchers("/appointments/**").authenticated()
                .requestMatchers("/slots/**").authenticated()
                
                // Diğer tüm istekler authentication gerektirir
                .anyRequest().authenticated()
            )
            
            // JWT authentication filter'ını ekle
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Frontend origin'lerini ekle
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",  // React dev server
            "http://localhost:5173",  // Vite dev server
            "https://your-domain.com" // Production domain
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
