package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService;

class EmployeePipControllerTest {
    @Configuration(proxyBeanMethods=false) @EnableMethodSecurity
    static class Config {
        @Bean EmployeePipService service(){return mock(EmployeePipService.class);}
        @Bean EmployeePipController controller(EmployeePipService service){return new EmployeePipController(service);}
    }
    private AnnotationConfigApplicationContext context;
    private MockMvc mvc;
    private EmployeePipService service;
    private final String today=LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
    @BeforeEach void setup(){
        context=new AnnotationConfigApplicationContext(Config.class); service=context.getBean(EmployeePipService.class);
        when(service.board("manager@example.test","",0)).thenReturn(new EmployeePipService.Board(List.of(),Page.empty()));
        when(service.start(any(),any(),any(),anyInt(),any(),any())).thenReturn(100L);
        mvc=MockMvcBuilders.standaloneSetup(context.getBean(EmployeePipController.class)).build(); login("ROLE_COO");
    }
    private void login(String role){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "manager@example.test",null,List.of(new SimpleGrantedAuthority(role))));}
    @AfterEach void close(){SecurityContextHolder.clearContext();context.close();}
    @Test void hrCannotOpenOrSubmitTheManagerOnlyPipScreen(){
        login("ROLE_HR");
        assertThatThrownBy(()->mvc.perform(get("/reporting-manager/pip").principal(()->"manager@example.test")))
                .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->mvc.perform(post("/reporting-manager/pip").principal(()->"manager@example.test")
                .param("employeeId","2").param("startDate",today).param("months","1").param("objectives","Improve delivery").param("reason","Missed deadlines")))
                .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(service,never()).start(any(),any(),any(),anyInt(),any(),any());
    }
    @Test void authorisedManagerCanStartPlan(){
        assertThatCode(()->mvc.perform(post("/reporting-manager/pip").principal(()->"manager@example.test")
                .param("employeeId","2").param("startDate",today).param("months","2").param("objectives","Improve delivery").param("reason","Missed deadlines"))
                .andExpect(redirectedUrl("/reporting-manager/pip/100"))).doesNotThrowAnyException();
    }
    @Test void invalidDurationAndMissingObjectivesAreRejectedBeforeService() throws Exception {
        mvc.perform(post("/reporting-manager/pip").principal(()->"manager@example.test")
                .param("employeeId","2").param("startDate",today).param("months","3"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("startForm","months","objectives","reason"));
        verify(service,never()).start(any(),any(),any(),anyInt(),any(),any());
    }
    @Test void finalDecisionRequiresExplicitConfirmation() throws Exception {
        mvc.perform(post("/reporting-manager/pip/100/decision").principal(()->"manager@example.test")
                .param("decision","CONTINUE").param("feedback","Meets expectations"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("decisionForm","confirmed"));
        verify(service,never()).decide(any(),any(),any(),any(),any());
    }}