package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.maharecruitment.gov.in.common.audit.AuditIpEntityListener;
import com.maharecruitment.gov.in.common.audit.AuditRequestContext;
import com.maharecruitment.gov.in.common.audit.IpAudited;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;

class AuditRequestContextFilterTest {
    @Test void everyAuditEntityCapturesTheSameRequestIpAndContextIsClearedOnFailure() {
        var properties = new TransportSecurityProperties();
        properties.setTrustForwardedHeaders(true);
        var filter = new AuditRequestContextFilter(properties);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "2001:db8::11, 10.0.0.2");
        List<IpAudited> auditRecords = List.of(
                new com.maharecruitment.gov.in.audit.entity.AuditEventEntity(),
                new com.maharecruitment.gov.in.auth.entity.LoginLogoutAuditHistory(),
                new com.maharecruitment.gov.in.asset.entity.AssetAuditLogEntity(),
                new com.maharecruitment.gov.in.master.entity.AgencyMasterAuditLog(),
                new com.maharecruitment.gov.in.master.entity.CommissionRateAuditLog(),
                new com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditLog(),
                new com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationAuditLogEntity(),
                new com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingAuditLogEntity(),
                new com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingAuditLogEntity(),
                new com.maharecruitment.gov.in.recruitment.entity.EmployeeTypeConversionAuditEntity());
        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, response) -> {
            var listener = new AuditIpEntityListener();
            for (var audit : auditRecords) {
                audit.setIpAddress("198.51.100.5");
                listener.capture(audit);
                assertThat(audit.getIpAddress()).isEqualTo("2001:db8::11");
            }
            throw new jakarta.servlet.ServletException("test failure");
        })).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(AuditRequestContext.ipAddress(null)).isEqualTo("SYSTEM");
    }

    @Test void ignoresForgedForwardedHeadersByDefault() throws Exception {
        var filter = new AuditRequestContextFilter(new TransportSecurityProperties());
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.8");
        request.addHeader("X-Forwarded-For", "198.51.100.5");
        filter.doFilter(request, new MockHttpServletResponse(), (req, response) ->
                assertThat(AuditRequestContext.ipAddress(null)).isEqualTo("192.0.2.8"));
        assertThat(AuditRequestContext.ipAddress(null)).isEqualTo("SYSTEM");
    }
}
