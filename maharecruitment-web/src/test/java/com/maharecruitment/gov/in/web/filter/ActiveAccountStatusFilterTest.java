package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;

class ActiveAccountStatusFilterTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActiveAccountStatusFilter filter = new ActiveAccountStatusFilter(users);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private void login() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("employee@example.test", null, List.of()));
    }

    @Test void invalidatesExistingBrowserSessionAfterDeactivation() throws Exception {
        login();
        var request = new MockHttpServletRequest("GET", "/app/employee/dashboard");
        request.setContextPath("/app");
        var session = request.getSession();
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertThat(response.getRedirectedUrl()).isEqualTo("/app/login?disabled");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThatThrownBy(() -> session.getAttribute("anything")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(chain);
    }

    @Test void rejectsExistingMobileBearerAuthenticationAfterDeactivation() throws Exception {
        login();
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/mobile/attendance/check-in"), response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ACCOUNT_DISABLED");
        verifyNoInteractions(chain);
    }

    @Test void allowsActiveAccountAndLeavesAuthenticationIntact() throws Exception {
        login();
        when(users.existsByEmailIgnoreCaseAndActiveTrue("employee@example.test")).thenReturn(true);
        var request = new MockHttpServletRequest("GET", "/employee/dashboard");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test void doesNotQueryDatabaseForUnauthenticatedRequests() throws Exception {
        var request = new MockHttpServletRequest("GET", "/login");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verifyNoInteractions(users);
        verify(chain).doFilter(request, response);
    }
}
