package com.maharecruitment.gov.in.recruitment.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;

import jakarta.annotation.PostConstruct;

@Component
public class AgencySelectedCandidateMenuInitializer {

    private static final Logger log = LoggerFactory.getLogger(AgencySelectedCandidateMenuInitializer.class);

    private final MstMenuRepository mstMenuRepository;
    private final RoleRepository roleRepository;

    public AgencySelectedCandidateMenuInitializer(
            MstMenuRepository mstMenuRepository,
            RoleRepository roleRepository) {
        this.mstMenuRepository = mstMenuRepository;
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        Role agencyRole = roleRepository.findByNameIgnoreCase("ROLE_AGENCY")
                .or(() -> roleRepository.findByNameIgnoreCase("AGENCY"))
                .orElseGet(() -> createRoleIfMissing("ROLE_AGENCY"));

        upsertDirectMenu(
                "Selected Candidates",
                "/agency/selected-candidates",
                "fa fa-user-check",
                agencyRole);
    }

    private void upsertDirectMenu(
            String menuName,
            String url,
            String iconClass,
            Role roleToAssign) {
        MstMenu menu = mstMenuRepository.findByMenuNameEnglishIgnoreCaseWithRoles(menuName)
                .orElseGet(MstMenu::new);

        menu.setMenuNameEnglish(menuName);
        menu.setMenuNameMarathi(menuName);
        menu.setUrl(url);
        menu.setIcon(iconClass);
        menu.setIsSubMenu(1);
        menu.setIsActive("Y");

        Set<Role> roles = menu.getRoles();
        if (roles == null) {
            roles = new LinkedHashSet<>();
        }
        if (roleToAssign != null) {
            roles.add(roleToAssign);
        }
        menu.setRoles(roles);

        MstMenu saved = mstMenuRepository.save(menu);
        log.info("Agency selected candidate menu bootstrapped. menuId={}, menuName={}, url={}",
                saved.getMenuId(),
                saved.getMenuNameEnglish(),
                saved.getUrl());
    }

    private Role createRoleIfMissing(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Role saved = roleRepository.save(role);
        log.info("Role created for Agency selected candidate menu bootstrap: {}", saved.getName());
        return saved;
    }
}
