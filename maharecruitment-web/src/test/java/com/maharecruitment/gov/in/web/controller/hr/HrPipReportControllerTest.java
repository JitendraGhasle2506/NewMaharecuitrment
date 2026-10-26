package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.*;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.*;
import com.maharecruitment.gov.in.web.service.pip.HrPipReportService;

class HrPipReportControllerTest {
    @Configuration(proxyBeanMethods=false) @EnableMethodSecurity
    static class Config {
        @Bean HrPipReportService service(){return mock(HrPipReportService.class);}
        @Bean HrPipReportController controller(HrPipReportService service){return new HrPipReportController(service);}
    }
    private AnnotationConfigApplicationContext context;
    private HrPipReportService service;
    private MockMvc mvc;
    private final LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
    @BeforeEach void setup() throws Exception {
        context=new AnnotationConfigApplicationContext(Config.class);service=context.getBean(HrPipReportService.class);
        Map<String,String> templates=new HashMap<>();
        for(String template:List.of("hr/pip-overview","employee/pip-detail")) templates.put(template,
                Files.readString(Path.of("src/main/resources/templates/"+template+".html")).replaceAll("th:replace=\"[^\"]*\"",""));
        var engine=new SpringTemplateEngine(); engine.setTemplateResolver(new StringTemplateResolver(){
            @Override protected ITemplateResource computeTemplateResource(IEngineConfiguration config,String owner,String template,Map<String,Object> attributes){
                return new StringTemplateResource(templates.get(template));
            }
        });
        var resolver=new ThymeleafViewResolver();resolver.setTemplateEngine(engine);resolver.setCharacterEncoding("UTF-8");
        List<PlanView> plans=new ArrayList<>();
        for(int i=0;i<12;i++)plans.add(new PlanView(100L+i,200L+i,i==0?"Outside team <Employee>":"Employee "+i,"EMP"+(200+i),
                today.minusDays(10),today.plusDays(20),1,Status.values()[i%3]));
        when(service.overview(any(),any(),any(),anyInt())).thenReturn(new HrPipReportService.Overview(new PageImpl<>(plans,PageRequest.of(0,12),26),10,8,8));
        when(service.detail("hr@example.test",100L)).thenReturn(new Detail(plans.getFirst(),"Improve delivery",
                List.of(new ReviewView("Reporting Manager",today,3,"Progress <script>alert(1)</script>")),null,null,true,"Missed <deadlines>"));
        mvc=MockMvcBuilders.standaloneSetup(context.getBean(HrPipReportController.class)).setViewResolvers(resolver).build();
        login("ROLE_HR");
    }
    private void login(String role){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "hr@example.test",null,List.of(new SimpleGrantedAuthority(role))));}
    @AfterEach void close(){SecurityContextHolder.clearContext();context.close();}
    @Test void hrSeesAllStatusesFiltersCountsAndReadOnlyDetails() throws Exception {
        String overview=mvc.perform(get("/portal/hr/pip").contextPath("/portal").principal(()->"hr@example.test"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(overview).contains("Across all employees","Under review","Continue employment","Exit recorded","name=\"status\"",
                "Outside team &lt;Employee&gt;","/portal/hr/pip/100","page=1");
        String detail=mvc.perform(get("/portal/hr/pip/100").contextPath("/portal").principal(()->"hr@example.test"))
                .andExpect(status().isOk()).andExpect(model().attribute("hrReadOnly",true)).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("Read-only view","Missed &lt;deadlines&gt;","Progress &lt;script&gt;","href=\"/portal/hr/pip\"")
                .doesNotContain("<form","Save review","Save final outcome","<script>alert(1)</script>");
        Files.writeString(Path.of("target/hr-pip-overview-preview.html"),overview);
        Files.writeString(Path.of("target/hr-pip-detail-preview.html"),detail);
    }
    @Test void passesStatusAndSearchToTheOrganisationWideService() throws Exception {
        mvc.perform(get("/hr/pip").param("status","UNDER_REVIEW").param("q","Employee").param("page","2").principal(()->"hr@example.test"))
                .andExpect(status().isOk());
        verify(service).overview("hr@example.test",Status.UNDER_REVIEW,"Employee",2);
    }
    @Test void rejectsInvalidStatusBeforeQueryingService() throws Exception {
        mvc.perform(get("/hr/pip").param("status","NOT_A_STATUS").principal(()->"hr@example.test")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void managersAndEmployeesCannotReadAllCompanyPipPlans(){
        for(String role:List.of("ROLE_COO","ROLE_HOD","ROLE_MD","ROLE_CTO","ROLE_CFO","ROLE_EMPLOYEE","ROLE_ADMIN")){
            login(role);
            assertThatThrownBy(()->mvc.perform(get("/hr/pip").principal(()->"hr@example.test")))
                    .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->mvc.perform(get("/hr/pip/100").principal(()->"hr@example.test")))
                    .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
        verifyNoInteractions(service);
    }
    @Test void hrReportHasNoPostEndpoints() throws Exception {
        mvc.perform(post("/hr/pip").principal(()->"hr@example.test")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/hr/pip/100").principal(()->"hr@example.test")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}