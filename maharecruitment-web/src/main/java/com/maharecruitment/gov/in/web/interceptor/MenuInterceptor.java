package com.maharecruitment.gov.in.web.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.MstSubMenu;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.service.MstMenuService;
import com.maharecruitment.gov.in.auth.service.MstSubMenuService;
import com.maharecruitment.gov.in.auth.service.RoleService;
import com.maharecruitment.gov.in.auth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class MenuInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuInterceptor.class);
    private static final String MENUS_KEY = "menus";
    private static final String SUB_MENUS_KEY = "submenus";
    private static final String HOMEPAGE_URL_KEY = "homepageUrl";

    private final MstMenuService mstMenuService;
    private final MstSubMenuService mstSubMenuService;
    private final UserService userService;
    private final RoleService roleService;

    public MenuInterceptor(
            MstMenuService mstMenuService,
            MstSubMenuService mstSubMenuService,
            UserService userService,
            RoleService roleService) {
        this.mstMenuService = mstMenuService;
        this.mstSubMenuService = mstSubMenuService;
        this.userService = userService;
        this.roleService = roleService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> !authority.isBlank())
                .toList();

        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return true;
        }

        Map<String, String> roleTargetUrlMap = CommonConstant.getDashboardUrls();
        List<Long> roleIds = new ArrayList<>();

        for (String roleName : roles) {
            Role role = roleService.getByName(roleName);
            if (role != null) {
                roleIds.add(role.getId());
            }
            if (session.getAttribute(HOMEPAGE_URL_KEY) == null && roleTargetUrlMap.containsKey(roleName)) {
                session.setAttribute(HOMEPAGE_URL_KEY, roleTargetUrlMap.get(roleName));
            }
        }

        if (session.getAttribute(HOMEPAGE_URL_KEY) == null) {
            session.setAttribute(HOMEPAGE_URL_KEY, "/home");
        }

        // Keep the user lookup to align with existing module contract and future per-user menu logic.
        userService.findUserByEmail(userDetails.getUsername());

        List<MstMenu> menus = mstMenuService.findMenusByRoleIds(roleIds);
        session.setAttribute(MENUS_KEY, menus);
        if (roles != null && !roles.isEmpty() && menus.isEmpty()) {
            LOGGER.warn("No DB menus found for authenticated user {} with roles {}", userDetails.getUsername(), roles);
        }

        List<Long> menuIds = menus.stream()
                .map(MstMenu::getMenuId)
                .filter(Objects::nonNull)
                .toList();

        List<MstSubMenu> subMenus = mstSubMenuService.getSubMenusByMenuIds(menuIds);
        session.setAttribute(SUB_MENUS_KEY, subMenus);

        return true;
    }
}
