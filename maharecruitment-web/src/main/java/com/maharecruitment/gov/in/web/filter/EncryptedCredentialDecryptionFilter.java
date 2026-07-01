package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService;
import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService.CredentialDecryptionException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
public class EncryptedCredentialDecryptionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(EncryptedCredentialDecryptionFilter.class);

    private static final Set<String> SENSITIVE_PARAMETER_NAMES = Set.of(
            "password",
            "currentpassword",
            "newpassword",
            "confirmpassword");

    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String LOGIN_PROCESSING_PATH = "/doLogin";
    private static final String INVALID_CREDENTIAL_MESSAGE = "Unable to process encrypted credentials. Please refresh and try again.";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final CredentialEncryptionService credentialEncryptionService;

    public EncryptedCredentialDecryptionFilter(CredentialEncryptionService credentialEncryptionService) {
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!PROTECTED_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            filterChain.doFilter(request, response);
            return;
        }

        Map<String, String[]> parameterMap = request.getParameterMap();
        boolean containsEncryptedCredential = containsEncryptedCredential(parameterMap);
        if (!containsEncryptedCredential) {
            if (isLoginProcessingRequest(request) && containsSensitiveParameter(parameterMap)) {
                rejectPlaintextCredential(request, response);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        try {
            filterChain.doFilter(new DecryptedCredentialRequestWrapper(request, decryptParameters(parameterMap)), response);
        } catch (CredentialDecryptionException ex) {
            log.warn("Rejected invalid encrypted credential payload. method={} path={}",
                    request.getMethod(), normalizeRequestPath(request));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(INVALID_CREDENTIAL_MESSAGE);
        }
    }

    private Map<String, String[]> decryptParameters(Map<String, String[]> parameterMap) {
        Map<String, String[]> decryptedParameters = new LinkedHashMap<>();
        parameterMap.forEach((name, values) -> decryptedParameters.put(name, decryptValuesIfSensitive(name, values)));
        return decryptedParameters;
    }

    private String[] decryptValuesIfSensitive(String name, String[] values) {
        if (!isSensitiveParameter(name) || values == null) {
            return values;
        }

        String[] decryptedValues = values.clone();
        for (int i = 0; i < decryptedValues.length; i++) {
            if (credentialEncryptionService.isEncryptedCredential(decryptedValues[i])) {
                decryptedValues[i] = credentialEncryptionService.decryptCredential(decryptedValues[i]);
            }
        }
        return decryptedValues;
    }

    private boolean containsEncryptedCredential(Map<String, String[]> parameterMap) {
        return parameterMap.entrySet().stream()
                .filter(entry -> isSensitiveParameter(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(values -> values != null)
                .flatMap(values -> java.util.Arrays.stream(values))
                .anyMatch(credentialEncryptionService::isEncryptedCredential);
    }

    private boolean containsSensitiveParameter(Map<String, String[]> parameterMap) {
        return parameterMap.keySet().stream().anyMatch(this::isSensitiveParameter);
    }

    private boolean isSensitiveParameter(String name) {
        return StringUtils.hasText(name) && SENSITIVE_PARAMETER_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    private boolean isLoginProcessingRequest(HttpServletRequest request) {
        return pathMatcher.match(LOGIN_PROCESSING_PATH, normalizeRequestPath(request));
    }

    private void rejectPlaintextCredential(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("Rejected plaintext login credential payload. method={} path={}",
                request.getMethod(), normalizeRequestPath(request));
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Encrypted password is required for login.");
    }

    private String normalizeRequestPath(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        return StringUtils.hasText(requestPath) ? requestPath : "/";
    }

    private static final class DecryptedCredentialRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String[]> parameterMap;

        private DecryptedCredentialRequestWrapper(HttpServletRequest request, Map<String, String[]> parameterMap) {
            super(request);
            this.parameterMap = copyParameterMap(parameterMap);
        }

        @Override
        public String getParameter(String name) {
            String[] values = getParameterValues(name);
            return values == null || values.length == 0 ? null : values[0];
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return parameterMap;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(parameterMap.keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = parameterMap.get(name);
            return values == null ? null : values.clone();
        }

        private static Map<String, String[]> copyParameterMap(Map<String, String[]> source) {
            Map<String, String[]> copy = new LinkedHashMap<>();
            source.forEach((key, value) -> copy.put(key, value == null ? null : value.clone()));
            return Collections.unmodifiableMap(copy);
        }
    }
}
