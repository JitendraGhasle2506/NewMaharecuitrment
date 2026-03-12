package com.maharecruitment.gov.in.department.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;

@Component
@Order(42)
public class DepartmentCandidateShortlistingMenuInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DepartmentCandidateShortlistingMenuInitializer.class);

    private final MstMenuRepository mstMenuRepository;
    private final RoleRepository roleRepository;

    public DepartmentCandidateShortlistingMenuInitializer(
            MstMenuRepository mstMenuRepository,
            RoleRepository roleRepository) {
        this.mstMenuRepository = mstMenuRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void afterPropertiesSet() {
        Role departmentRole = roleRepository.findByNameIgnoreCase("ROLE_DEPARTMENT")
                .or(() -> roleRepository.findByNameIgnoreCase("DEPARTMENT"))
                .orElseGet(() -> createRoleIfMissing("ROLE_DEPARTMENT"));

        upsertDirectMenu(
                "Project Name",
                "/department/candidate-shortlisting/projects",
                "fa fa-list-check",
                departmentRole);
    }

    private void upsertDirectMenu(
            String menuName,
            String url,
            String iconClass,
            Role roleToAssign) {
        MstMenu menu = mstMenuRepository.findByMenuNameEnglishIgnoreCaseWithRoles(menuName)
                .or(() -> mstMenuRepository.findByMenuNameEnglishIgnoreCase(menuName))
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
        log.info("Department shortlisting menu bootstrapped. menuId={}, menuName={}, url={}",
                saved.getMenuId(),
                saved.getMenuNameEnglish(),
                saved.getUrl());
    }

    private Role createRoleIfMissing(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Role saved = roleRepository.save(role);
        log.info("Role created for Department menu bootstrap: {}", saved.getName());
        return saved;
    }
}
