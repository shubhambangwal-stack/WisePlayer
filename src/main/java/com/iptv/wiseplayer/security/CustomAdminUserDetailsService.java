package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.stereotype.Service;

@Service
public class CustomAdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;

    public CustomAdminUserDetailsService(AdminRepository adminRepository,
            SuperAdminRepository superAdminRepository) {
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Check SuperAdmin table (Plain-text password)
        return superAdminRepository.findByUsername(username)
                .map(sa -> User.withUsername(sa.getUsername())
                        .password("{noop}" + sa.getPassword()) // {noop} tells Spring it's a plain text comparison for
                                                               // SuperAdmins
                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                        .build())
                .orElseGet(() -> {
                    // 2. Check Admin table (Hashed password)
                    Admin admin = adminRepository.findByUsername(username)
                            .orElseThrow(
                                    () -> new UsernameNotFoundException("Admin not found with username: " + username));

                    return User.withUsername(admin.getUsername())
                            .password(admin.getPasswordHash()) // Password here is already hashed with BCrypt
                            .authorities(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))
                            .disabled(!admin.isActive())
                            .build();
                });
    }
}
