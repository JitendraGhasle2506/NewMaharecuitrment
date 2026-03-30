package com.maharecruitment.gov.in.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.UserAffiliationView;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.entity.UserAgencyMappingEntity;
import com.maharecruitment.gov.in.auth.entity.UserDepartmentMappingEntity;
import com.maharecruitment.gov.in.auth.entity.UserProfileEntity;
import com.maharecruitment.gov.in.auth.repository.UserAgencyMappingRepository;
import com.maharecruitment.gov.in.auth.repository.UserDepartmentMappingRepository;
import com.maharecruitment.gov.in.auth.repository.UserProfileRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;

@Service
@Transactional
public class UserAffiliationServiceImpl implements UserAffiliationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserDepartmentMappingRepository userDepartmentMappingRepository;
    private final UserAgencyMappingRepository userAgencyMappingRepository;

    public UserAffiliationServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserDepartmentMappingRepository userDepartmentMappingRepository,
            UserAgencyMappingRepository userAgencyMappingRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userDepartmentMappingRepository = userDepartmentMappingRepository;
        this.userAgencyMappingRepository = userAgencyMappingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User loadUserByEmail(String email) {
        String normalizedEmail = UserValidationUtil.normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + normalizedEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAffiliationView getAffiliationByEmail(String email) {
        return getAffiliation(loadUserByEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAffiliationView getAffiliation(User user) {
        UserProfileEntity profile = user != null && user.getId() != null
                ? userProfileRepository.findById(user.getId()).orElse(null)
                : null;
        DepartmentRegistrationEntity departmentRegistration = resolvePrimaryDepartmentRegistration(user);
        Long agencyId = resolvePrimaryAgencyId(user);

        List<String> roleNames = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(role -> role.getName())
                        .filter(Objects::nonNull)
                        .sorted(Comparator.naturalOrder())
                        .toList();

        return UserAffiliationView.builder()
                .userId(user.getId())
                .name(profile != null ? profile.getFullName() : user.getName())
                .email(user.getEmail())
                .mobileNo(profile != null ? profile.getMobileNo() : user.getMobileNo())
                .departmentRegistrationId(departmentRegistration != null
                        ? departmentRegistration.getDepartmentRegistrationId()
                        : null)
                .departmentId(departmentRegistration != null ? departmentRegistration.getDepartmentId() : null)
                .subDepartmentId(departmentRegistration != null ? departmentRegistration.getSubDeptId() : null)
                .departmentName(departmentRegistration != null ? departmentRegistration.getDepartmentName() : null)
                .agencyId(agencyId)
                .roleNames(roleNames)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentRegistrationEntity resolvePrimaryDepartmentRegistration(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }

        return userDepartmentMappingRepository
                .findTopByUser_IdAndActiveTrueOrderByPrimaryMappingDescUserDepartmentMappingIdAsc(user.getId())
                .map(UserDepartmentMappingEntity::getDepartmentRegistration)
                .orElse(user.getDepartmentRegistrationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolvePrimaryAgencyId(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }

        return userAgencyMappingRepository
                .findTopByUser_IdAndActiveTrueOrderByPrimaryMappingDescUserAgencyMappingIdAsc(user.getId())
                .map(UserAgencyMappingEntity::getAgencyId)
                .orElse(null);
    }

    @Override
    public void synchronizeUserProfile(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Saved user is required to synchronize profile.");
        }
        String actor = user.getEmail();
        LocalDateTime now = LocalDateTime.now();

        UserProfileEntity profile = userProfileRepository.findById(user.getId()).orElseGet(() -> {
            UserProfileEntity entity = new UserProfileEntity();
            entity.setUser(user);
            entity.setCreatedBy(actor);
            entity.setCreatedDate(now);
            return entity;
        });

        profile.setUser(user);
        profile.setFullName(user.getName());
        profile.setMobileNo(user.getMobileNo());
        profile.setUpdatedBy(actor);
        profile.setUpdatedDate(now);
        if (profile.getCreatedDate() == null) {
            profile.setCreatedDate(now);
        }
        userProfileRepository.save(profile);
    }

    @Override
    public void synchronizePrimaryDepartment(User user, DepartmentRegistrationEntity departmentRegistration) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Saved user is required to synchronize department mapping.");
        }

        LocalDateTime now = LocalDateTime.now();
        String actor = user.getEmail();
        List<UserDepartmentMappingEntity> mappings = userDepartmentMappingRepository
                .findByUser_IdOrderByUserDepartmentMappingIdAsc(user.getId());

        UserDepartmentMappingEntity target = null;
        if (departmentRegistration != null && departmentRegistration.getDepartmentRegistrationId() != null) {
            target = userDepartmentMappingRepository.findByUser_IdAndDepartmentRegistration_DepartmentRegistrationId(
                    user.getId(),
                    departmentRegistration.getDepartmentRegistrationId()).orElse(null);
        }

        for (UserDepartmentMappingEntity mapping : mappings) {
            boolean wasActive = Boolean.TRUE.equals(mapping.getActive());
            boolean matchesTarget = target != null
                    && Objects.equals(mapping.getUserDepartmentMappingId(), target.getUserDepartmentMappingId());
            mapping.setPrimaryMapping(matchesTarget);
            mapping.setActive(matchesTarget);
            mapping.setUpdatedBy(actor);
            mapping.setUpdatedDate(now);
            if (matchesTarget) {
                if (mapping.getEffectiveFrom() == null) {
                    mapping.setEffectiveFrom(now);
                }
                mapping.setEffectiveTo(null);
            } else if (wasActive && mapping.getEffectiveTo() == null) {
                mapping.setEffectiveTo(now);
            }
        }

        if (departmentRegistration != null && departmentRegistration.getDepartmentRegistrationId() != null && target == null) {
            target = new UserDepartmentMappingEntity();
            target.setUser(user);
            target.setDepartmentRegistration(departmentRegistration);
            target.setPrimaryMapping(Boolean.TRUE);
            target.setActive(Boolean.TRUE);
            target.setEffectiveFrom(now);
            target.setCreatedBy(actor);
            target.setCreatedDate(now);
            target.setUpdatedBy(actor);
            target.setUpdatedDate(now);
            mappings.add(target);
        }

        user.setDepartmentRegistrationId(departmentRegistration);
        userRepository.save(user);
        userDepartmentMappingRepository.saveAll(mappings);
    }

    @Override
    public void synchronizePrimaryAgency(User user, Long agencyId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Saved user is required to synchronize agency mapping.");
        }

        LocalDateTime now = LocalDateTime.now();
        String actor = user.getEmail();
        List<UserAgencyMappingEntity> mappings = userAgencyMappingRepository.findByUser_IdOrderByUserAgencyMappingIdAsc(
                user.getId());

        UserAgencyMappingEntity target = agencyId == null
                ? null
                : userAgencyMappingRepository.findByUser_IdAndAgencyId(user.getId(), agencyId).orElse(null);

        for (UserAgencyMappingEntity mapping : mappings) {
            boolean wasActive = Boolean.TRUE.equals(mapping.getActive());
            boolean matchesTarget = target != null
                    && Objects.equals(mapping.getUserAgencyMappingId(), target.getUserAgencyMappingId());
            mapping.setPrimaryMapping(matchesTarget);
            mapping.setActive(matchesTarget);
            mapping.setUpdatedBy(actor);
            mapping.setUpdatedDate(now);
            if (matchesTarget) {
                if (mapping.getEffectiveFrom() == null) {
                    mapping.setEffectiveFrom(now);
                }
                mapping.setEffectiveTo(null);
            } else if (wasActive && mapping.getEffectiveTo() == null) {
                mapping.setEffectiveTo(now);
            }
        }

        if (agencyId != null && target == null) {
            target = new UserAgencyMappingEntity();
            target.setUser(user);
            target.setAgencyId(agencyId);
            target.setPrimaryMapping(Boolean.TRUE);
            target.setActive(Boolean.TRUE);
            target.setEffectiveFrom(now);
            target.setCreatedBy(actor);
            target.setCreatedDate(now);
            target.setUpdatedBy(actor);
            target.setUpdatedDate(now);
            mappings.add(target);
        }

        userAgencyMappingRepository.saveAll(mappings);
    }
}
