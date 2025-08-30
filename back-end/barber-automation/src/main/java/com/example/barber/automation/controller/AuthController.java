package com.example.barber.automation.controller;

import com.example.barber.automation.dto.LoginRequest;
import com.example.barber.automation.dto.LoginResponse;
import com.example.barber.automation.entity.TenantUser;
import com.example.barber.automation.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication işlemleri için controller
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Kimlik doğrulama işlemleri")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Kullanıcı login işlemi
     */
    @PostMapping("/login")
    @Operation(summary = "Kullanıcı girişi", description = "Kullanıcı adı ve şifre ile sisteme giriş yapar")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Giriş başarısız: " + e.getMessage());
        }
    }

    /**
     * Super admin oluşturma (ilk kurulum için)
     */
    @PostMapping("/setup-admin")
    @Operation(summary = "Super admin oluştur", description = "İlk kurulum için super admin kullanıcısı oluşturur")
    public ResponseEntity<?> setupAdmin(@RequestParam String username,
                                      @RequestParam String email,
                                      @RequestParam String password,
                                      @RequestParam String firstName,
                                      @RequestParam String lastName) {
        try {
            TenantUser superAdmin = authService.createSuperAdmin(username, email, password, firstName, lastName);
            return ResponseEntity.ok("Super admin başarıyla oluşturuldu: " + superAdmin.getUsername());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Super admin oluşturulamadı: " + e.getMessage());
        }
    }

    /**
     * Mevcut kullanıcı bilgilerini al
     */
    @GetMapping("/me")
    @Operation(summary = "Kullanıcı bilgileri", description = "Giriş yapmış kullanıcının bilgilerini döner")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String username = authentication.getName();
            TenantUser user = authService.getCurrentUser(username);
            
            // Password'ü response'tan çıkar
            user.setPassword(null);
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Kullanıcı bilgileri alınamadı: " + e.getMessage());
        }
    }

    /**
     * Token doğrulama
     */
    @GetMapping("/validate")
    @Operation(summary = "Token doğrulama", description = "JWT token'ın geçerliliğini kontrol eder")
    public ResponseEntity<?> validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok("Token geçerli");
        } else {
            return ResponseEntity.status(401).body("Token geçersiz");
        }
    }
}
