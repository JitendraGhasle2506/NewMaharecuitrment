package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileBase64ImageMapper;
import com.maharecruitment.gov.in.web.service.mobile.MobileProfileService;

@ExtendWith(MockitoExtension.class)
class MobileProfileControllerTest {

    @Mock
    private MobileProfileService mobileProfileService;

    @Mock
    private MobileBase64ImageMapper mobileBase64ImageMapper;

    @Test
    void updatePhotoAcceptsMultipartFaceDataAlias() {
        MobileProfileController controller = new MobileProfileController(
                mobileProfileService,
                mobileBase64ImageMapper);
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "profile.jpg",
                "image/jpeg",
                new byte[] { 1, 2, 3 });
        MobileProfileResponse response = new MobileProfileResponse(
                true,
                "Profile photo updated successfully.",
                10L,
                101L,
                "EMP101",
                "Test Employee",
                "employee@example.com",
                "9876543210",
                "updated-photo",
                "[0.123, 0.456]",
                null,
                null,
                null,
                null);

        when(mobileProfileService.updatePhoto(101L, photo, " [0.123, 0.456] ")).thenReturn(response);

        ResponseEntity<MobileProfileResponse> result = controller.updatePhoto(
                101L,
                photo,
                null,
                " [0.123, 0.456] ",
                null);

        assertThat(result.getBody()).isSameAs(response);
        verify(mobileProfileService).updatePhoto(101L, photo, " [0.123, 0.456] ");
    }
}
