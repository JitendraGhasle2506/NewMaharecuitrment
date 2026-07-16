package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class HttpMethodPolicyFilterTest {

    private final HttpMethodPolicyFilter filter = new HttpMethodPolicyFilter(false);

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD", "POST", "PUT", "DELETE", "PATCH" })
    void supportedMethodsContinueThroughTheChain(String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = { "TRACE", "TRACK", "DEBUG", "CONNECT", "OPTIONS", "BREW" })
    void unsupportedMethodsReturnStandardized405(String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"status\":false,\"message\":\"HTTP method " + method + " is not allowed.\"}");
        verifyNoInteractions(chain);
    }

    @Test
    void optionsCanBeEnabledOnlyForAnExplicitCorsDeployment() throws Exception {
        HttpMethodPolicyFilter corsFilter = new HttpMethodPolicyFilter(true);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        corsFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
