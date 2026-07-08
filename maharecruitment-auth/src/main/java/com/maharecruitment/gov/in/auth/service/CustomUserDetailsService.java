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
    private final AgencyAccountAccessService agencyAccountAccessService;

    public CustomUserDetailsService(
            UserRepository userRepository,
            AgencyAccountAccessService agencyAccountAccessService) {
        this.userRepository = userRepository;
        this.agencyAccountAccessService = agencyAccountAccessService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = loadDomainUserByIdentifier(username);
        agencyAccountAccessService.validateLoginAccess(user);

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> AuthorityUtil.toAuthority(role.getName()))
                .filter(Objects::nonNull)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!Boolean.TRUE.equals(user.getActive()))
                .build();
    }

    public User loadDomainUserByIdentifier(String identifier) {
        return findUserByIdentifier(identifier)
                .orElseThrow(() -> {
                    if (identifier != null && identifier.contains("@")) {
                        return new UsernameNotFoundException("Incorrect Email ID, please enter registered mail id");
                    } else if (identifier != null && identifier.matches("^[0-9]+$")) {
                        return new UsernameNotFoundException("Incorrect Mobile Number, please enter registered mobile number");
                    }
                    return new UsernameNotFoundException("Incorrect Username, please enter registered username");
                });
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
