package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.util.ContextPathUrlResolver;

class HomeControllerTest {

    private static final String CONTEXT_PATH = "/maharecruitment";

    private final NotificationChannelProperties notificationChannelProperties = new NotificationChannelProperties();
    private final HomeController controller = new HomeController(
            new OtpVerificationProperties(),
            notificationChannelProperties,
            new ContextPathUrlResolver());

    @Test
    void homeRedirectStripsContextPathBeforeReturningSpringRedirect() {
        MockHttpServletRequest request = request();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("homepageUrl", "/maharecruitment/admin/dashboard");

        String viewName = controller.home(request, session);

        assertThat(viewName).isEqualTo("redirect:/admin/dashboard");
    }

    @Test
    void homeRedirectStripsRepeatedContextPathBeforeReturningSpringRedirect() {
        MockHttpServletRequest request = request();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("homepageUrl", "/maharecruitment/maharecruitment/");

        String viewName = controller.home(request, session);

        assertThat(viewName).isEqualTo("redirect:/");
    }

    @Test
    void loginPageDefaultsToPasswordModeWhenOtpChannelsAreDisabled() {
        notificationChannelProperties.setEmailEnabled(false);
        notificationChannelProperties.setSmsEnabled(false);
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = controller.loginPage(model);

        assertThat(viewName).isEqualTo("login");
        assertThat(model.get("otpEmailEnabled")).isEqualTo(false);
        assertThat(model.get("otpSmsEnabled")).isEqualTo(false);
        assertThat(model.get("otpLoginEnabled")).isEqualTo(false);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", CONTEXT_PATH + "/home");
        request.setContextPath(CONTEXT_PATH);
        return request;
    }
}
