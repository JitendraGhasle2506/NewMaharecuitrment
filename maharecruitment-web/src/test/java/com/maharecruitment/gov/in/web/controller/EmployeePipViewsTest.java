package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.*;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.*;

class EmployeePipViewsTest {
    @Test void rendersRealFormsFeedbackAndClosedOutcomeWithoutExposingUnsafeMarkup() throws Exception {
        Map<String,String> sources=new HashMap<>();
        for(String name:List.of("pip-board","pip-detail")) sources.put("employee/"+name,
                Files.readString(Path.of("src/main/resources/templates/employee/"+name+".html")).replaceAll("th:replace=\"[^\"]*\"",""));
        var engine=new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver(){
            @Override protected ITemplateResource computeTemplateResource(IEngineConfiguration config,String owner,String template,Map<String,Object> attributes){
                return new StringTemplateResource(sources.get(template));
            }
        });
        var resolver=new ThymeleafViewResolver(); resolver.setTemplateEngine(engine); resolver.setCharacterEncoding("UTF-8");
        var service=mock(EmployeePipService.class);
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
        var plan=new PlanView(100L,2L,"Team <Member>","EMP002",today.minusDays(10),today.plusDays(20),1,Status.UNDER_REVIEW);
        when(service.board("manager@example.test","",0)).thenReturn(new Board(
                List.of(new EmployeeOption(2L,"Team <Member>","EMP002")),new PageImpl<>(List.of(plan))));
        when(service.detail("manager@example.test",100L)).thenReturn(new Detail(plan,"Improve delivery",
                List.of(new ReviewView("Manager",today,3,"Progress <script>alert(1)</script>")),null,null,true,"Missed <deadlines>"));
        var closed=new PlanView(101L,2L,"Team Member","EMP002",today.minusDays(10),today.plusDays(20),1,Status.EXIT_RECORDED);
        when(service.detail("manager@example.test",101L)).thenReturn(new Detail(closed,"Improve delivery",List.of(),"No improvement",today,false,null));
        var mvc=MockMvcBuilders.standaloneSetup(new EmployeePipController(service)).setViewResolvers(resolver).build();
        String board=mvc.perform(get("/portal/reporting-manager/pip").contextPath("/portal").principal(()->"manager@example.test"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(board).contains("Team &lt;Member&gt;","1 month","2 months","Reason for PIP","name=\"reason\"","Under review","select2","action=\"/portal/reporting-manager/pip\"");
        String detail=mvc.perform(get("/portal/reporting-manager/pip/100").contextPath("/portal").principal(()->"manager@example.test"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("Missed &lt;deadlines&gt;", "Reason for PIP", "Progress &lt;script&gt;", "name=\"confirmed\"", "id=\"reviewFeedback\"", "id=\"decisionFeedback\"", "continue normal", "record exit")
                .doesNotContain("<script>alert(1)</script>","th:field");
        String closedHtml=mvc.perform(get("/reporting-manager/pip/101").principal(()->"manager@example.test"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(closedHtml).contains("Exit recorded","No improvement","Last working date","Not recorded for this existing plan.").doesNotContain("id=\"pipDecisionForm\"","Save review");
        String errors=mvc.perform(post("/reporting-manager/pip/100/reviews").principal(()->"manager@example.test"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(errors).contains("alert-danger");
        Files.writeString(Path.of("target/pip-board-preview.html"),board);
        Files.writeString(Path.of("target/pip-detail-preview.html"),detail);
        Files.writeString(Path.of("target/pip-closed-preview.html"),closedHtml);
    }
}