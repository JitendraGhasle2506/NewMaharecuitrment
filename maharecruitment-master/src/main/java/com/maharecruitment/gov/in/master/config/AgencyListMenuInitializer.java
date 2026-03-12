package com.maharecruitment.gov.in.master.config;

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
@Order(43)
public class AgencyListMenuInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AgencyListMenuInitializer.class);

    private final MstMenuRepository mstMenuRepository;
    private final RoleRepository roleRepository;

    public AgencyListMenuInitializer(
            MstMenuRepository mstMenuRepository,
            RoleRepository roleRepository) {
        this.mstMenuRepository = mstMenuRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void afterPropertiesSet() {
        Role adminRole = roleRepository.findByNameIgnoreCase("ROLE_ADMIN")
                .or(() -> roleRepository.findByNameIgnoreCase("ADMIN"))
                .orElseGet(() -> createRoleIfMissing("ROLE_ADMIN"));

        Role hrRole = roleRepository.findByNameIgnoreCase("ROLE_HR")
                .or(() -> roleRepository.findByNameIgnoreCase("HR"))
                .orElseGet(() -> createRoleIfMissing("ROLE_HR"));

        upsertDirectMenu(
                "Agency List",
                "/master/agencies",
                "fa fa-building",
                adminRole,
                hrRole);
    }

    private void upsertDirectMenu(
            String menuName,
            String url,
            String iconClass,
            Role... rolesToAssign) {
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
        if (rolesToAssign != null) {
            for (Role role : rolesToAssign) {
                if (role != null) {
                    roles.add(role);
                }
            }
        }
        menu.setRoles(roles);

        MstMenu saved = mstMenuRepository.save(menu);
        log.info("Agency list menu bootstrapped. menuId={}, menuName={}, url={}",
                saved.getMenuId(),
                saved.getMenuNameEnglish(),
                saved.getUrl());
    }

    private Role createRoleIfMissing(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Role saved = roleRepository.save(role);
        log.info("Role created for Agency List menu bootstrap: {}", saved.getName());
        return saved;
    }
}
