package com.maharecruitment.gov.in.auth.service;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.util.AuthorityUtil;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = loadDomainUserByIdentifier(username);

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> AuthorityUtil.toAuthority(role.getName()))
                .filter(Objects::nonNull)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

    public User loadDomainUserByIdentifier(String identifier) {
        return findUserByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for identifier: " + identifier));
    }

    private java.util.Optional<User> findUserByIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return java.util.Optional.empty();
        }

        String normalized = identifier.trim();
        if (normalized.matches("^[0-9]{10,15}$")) {
            return userRepository.findByMobileNo(normalized);
        }

        return userRepository.findByEmailIgnoreCase(UserValidationUtil.normalizeEmail(normalized));
    }
}
