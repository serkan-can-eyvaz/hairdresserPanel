package com.example.barber.automation.service;

import com.example.barber.automation.dto.LoginRequest;
import com.example.barber.automation.dto.LoginResponse;
import com.example.barber.automation.entity.TenantUser;
import com.example.barber.automation.repository.TenantUserRepository;
import com.example.barber.automation.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication işlemleri için service sınıfı
 */
@Service
public class AuthService {

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Kullanıcı login işlemi
     */
    public LoginResponse login(LoginRequest loginRequest) {
        // Kullanıcıyı bul
        TenantUser user = tenantUserRepository.findByUsernameAndActive(loginRequest.getUsername(), true)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı veya aktif değil"));

        // Şifre kontrolü
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Geçersiz şifre");
        }

        // JWT token oluştur
        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().name(),
                user.getTenant() != null ? user.getTenant().getId() : null
        );

        // Response oluştur
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole().name(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getTenant() != null ? user.getTenant().getName() : null
        );
    }

    /**
     * Super admin kullanıcısı oluştur (ilk kurulum için)
     */
    public TenantUser createSuperAdmin(String username, String email, String password, String firstName, String lastName) {
        // Zaten super admin var mı kontrol et
        if (tenantUserRepository.existsByRole(TenantUser.UserRole.SUPER_ADMIN)) {
            throw new RuntimeException("Super admin kullanıcı zaten mevcut");
        }

        // Username kontrolü
        if (tenantUserRepository.existsByUsername(username)) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor");
        }

        // Email kontrolü
        if (tenantUserRepository.existsByEmail(email)) {
            throw new RuntimeException("Bu email adresi zaten kullanılıyor");
        }

        // Super admin oluştur
        TenantUser superAdmin = new TenantUser();
        superAdmin.setUsername(username);
        superAdmin.setEmail(email);
        superAdmin.setPassword(passwordEncoder.encode(password));
        superAdmin.setFirstName(firstName);
        superAdmin.setLastName(lastName);
        superAdmin.setRole(TenantUser.UserRole.SUPER_ADMIN);
        superAdmin.setActive(true);
        // Super admin tenant'a bağlı değil
        superAdmin.setTenant(null);

        return tenantUserRepository.save(superAdmin);
    }

    /**
     * Mevcut kullanıcının bilgilerini al
     */
    public TenantUser getCurrentUser(String username) {
        return tenantUserRepository.findByUsernameAndActive(username, true)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}
