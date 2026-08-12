package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.master.service.DepartmentMstService;
import com.maharecruitment.gov.in.master.service.SubDepartmentService;
import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationForm;
import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.service.registration.DepartmentRegistrationPageService;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

class DepartmentRegistrationPageControllerTest {

    @Test
    void redirectsToSuccessAfterTransactionalRegistrationWorkflowCompletes() {
        DepartmentRegistrationPageService registrationService = mock(DepartmentRegistrationPageService.class);
        OtpVerificationService otpVerificationService = mock(OtpVerificationService.class);
        NotificationChannelProperties channelProperties = mock(NotificationChannelProperties.class);
        DepartmentRegistrationPageController controller = new DepartmentRegistrationPageController(
                mock(DepartmentMstService.class),
                mock(SubDepartmentService.class),
                registrationService,
                otpVerificationService,
                channelProperties,
                new OtpVerificationProperties(),
                true);

        DepartmentRegistrationForm form = validForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "registrationForm");
        Model model = mock(Model.class);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of());
        String view = controller.register(
                form,
                bindingResult,
                model,
                redirectAttributes,
                session,
                request);

        assertThat(view).isEqualTo("redirect:/login");
        verify(registrationService).register(form, session);
        verify(redirectAttributes).addAttribute("registered", "true");
    }

    @Test
    void mapsDuplicateGstErrorToVisibleSensitiveFieldWithoutReflectingPlaintext() {
        DepartmentMstService departmentService = mock(DepartmentMstService.class);
        SubDepartmentService subDepartmentService = mock(SubDepartmentService.class);
        DepartmentRegistrationPageService registrationService = mock(DepartmentRegistrationPageService.class);
        OtpVerificationService otpVerificationService = mock(OtpVerificationService.class);
        NotificationChannelProperties channelProperties = mock(NotificationChannelProperties.class);
        DepartmentRegistrationPageController controller = new DepartmentRegistrationPageController(
                departmentService,
                subDepartmentService,
                registrationService,
                otpVerificationService,
                channelProperties,
                new OtpVerificationProperties(),
                true);

        DepartmentRegistrationForm form = validForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "registrationForm");
        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of());
        when(departmentService.getAll(Pageable.unpaged())).thenReturn(Page.empty());
        when(subDepartmentService.getAll(1L, Pageable.unpaged())).thenReturn(Page.empty());
        doThrow(new IllegalArgumentException("A registration already exists for the provided GST number."))
                .when(registrationService)
                .register(form, session);

        String view = controller.register(
                form,
                bindingResult,
                mock(Model.class),
                mock(RedirectAttributes.class),
                session,
                request);

        assertThat(view).isEqualTo("register/department-registration");
        assertThat(bindingResult.getFieldError("gstNumberEncrypted")).isNotNull();
        assertThat(bindingResult.getFieldError("gstNumberEncrypted").getDefaultMessage())
                .isEqualTo("A registration already exists for the provided GST number.");
        assertThat(form.getGstNumberEncrypted()).isNull();
        assertThat(form.getPanNumberEncrypted()).isNull();
    }

    @Test
    void asyncValidationReturnsSmallErrorPayloadWithoutReloadingMasterData() {
        DepartmentMstService departmentService = mock(DepartmentMstService.class);
        SubDepartmentService subDepartmentService = mock(SubDepartmentService.class);
        DepartmentRegistrationPageService registrationService = mock(DepartmentRegistrationPageService.class);
        DepartmentRegistrationPageController controller = new DepartmentRegistrationPageController(
                departmentService,
                subDepartmentService,
                registrationService,
                mock(OtpVerificationService.class),
                mock(NotificationChannelProperties.class),
                new OtpVerificationProperties(),
                true);
        DepartmentRegistrationForm form = validForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "registrationForm");
        bindingResult.rejectValue("address", "registration.address", "Office address is required");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of());

        ResponseEntity<DepartmentRegistrationPageController.RegistrationSubmissionResponse> response =
                controller.registerAsync(form, bindingResult, mock(HttpSession.class), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().fieldErrors())
                .containsEntry("address", "Office address is required");
        verifyNoInteractions(departmentService, subDepartmentService, registrationService);
    }

    private DepartmentRegistrationForm validForm() {
        DepartmentRegistrationForm form = new DepartmentRegistrationForm();
        form.setDepartmentId(1L);
        form.setPrimaryMobile("9876543210");
        form.setPrimaryEmail("primary@example.test");
        form.setSecondaryMobile("9876543211");
        form.setSecondaryEmail("secondary@example.test");
        form.setGstFile(pdf("gstFile", "gst.pdf"));
        form.setPanFile(pdf("panFile", "pan.pdf"));
        form.setTanFile(pdf("tanFile", "tan.pdf"));
        return form;
    }

    private MockMultipartFile pdf(String field, String name) {
        return new MockMultipartFile(
                field,
                name,
                "application/pdf",
                "%PDF-test".getBytes(StandardCharsets.UTF_8));
    }
}
