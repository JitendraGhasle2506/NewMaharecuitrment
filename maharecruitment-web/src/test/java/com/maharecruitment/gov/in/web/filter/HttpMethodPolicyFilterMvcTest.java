package com.maharecruitment.gov.in.web.filter;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

class HttpMethodPolicyFilterMvcTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ExplicitMethodController())
            .addFilters(new HttpMethodPolicyFilter(false))
            .build();

    @ParameterizedTest
    @ValueSource(strings = { "GET", "POST", "PUT", "DELETE", "PATCH" })
    void explicitlyMappedApplicationMethodsStillWork(String method) throws Exception {
        mvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(method), URI.create("/method-policy")))
                .andExpect(status().isOk())
                .andExpect(content().string(method));
    }

    @ParameterizedTest
    @ValueSource(strings = { "TRACE", "TRACK", "DEBUG", "CONNECT", "OPTIONS", "BREW" })
    void disallowedAndInvalidMethodsAreRejectedBeforeControllerDispatch(String method) throws Exception {
        mvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(method), URI.create("/method-policy")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        "{\"status\":false,\"message\":\"HTTP method " + method + " is not allowed.\"}"));
    }

    @RestController
    static class ExplicitMethodController {
        @GetMapping("/method-policy") String get() { return "GET"; }
        @PostMapping("/method-policy") String post() { return "POST"; }
        @PutMapping("/method-policy") String put() { return "PUT"; }
        @DeleteMapping("/method-policy") String delete() { return "DELETE"; }
        @PatchMapping("/method-policy") String patch() { return "PATCH"; }
    }
}
