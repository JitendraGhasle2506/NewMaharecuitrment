package com.maharecruitment.gov.in.web.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceAction;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceJsonRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceMarkJsonRequest;

class MobileAttendanceJsonImageMapperTest {

    private final MobileAttendanceJsonImageMapper mapper = new MobileAttendanceJsonImageMapper();

    @Test
    void mapsPlainBase64ToMultipartImage() throws Exception {
        byte[] bytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        MobileAttendanceJsonRequest request = request(
                Base64.getEncoder().encodeToString(bytes),
                "attendance.jpg",
                "image/jpeg");

        MultipartFile file = mapper.toMultipartFile(request);

        assertThat(file.getName()).isEqualTo("image");
        assertThat(file.getOriginalFilename()).isEqualTo("attendance.jpg");
        assertThat(file.getContentType()).isEqualTo("image/jpeg");
        assertThat(file.getBytes()).isEqualTo(bytes);
    }

    @Test
    void mapsDataUriToMultipartImage() throws Exception {
        byte[] bytes = "png-bytes".getBytes(StandardCharsets.UTF_8);
        MobileAttendanceJsonRequest request = request(
                "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes),
                null,
                null);

        MultipartFile file = mapper.toMultipartFile(request);

        assertThat(file.getOriginalFilename()).isEqualTo("attendance.png");
        assertThat(file.getContentType()).isEqualTo("image/png");
        assertThat(file.getBytes()).isEqualTo(bytes);
    }

    @Test
    void mapsMarkRequestToMultipartImage() throws Exception {
        byte[] bytes = "mark-image-bytes".getBytes(StandardCharsets.UTF_8);
        MobileAttendanceMarkJsonRequest request = markRequest(
                MobileAttendanceAction.CHECK_IN,
                Base64.getEncoder().encodeToString(bytes),
                null,
                null);

        MultipartFile file = mapper.toMultipartFile(request);

        assertThat(file.getOriginalFilename()).isEqualTo("attendance.jpg");
        assertThat(file.getContentType()).isEqualTo("image/jpeg");
        assertThat(file.getBytes()).isEqualTo(bytes);
    }

    @Test
    void rejectsInvalidBase64() {
        MobileAttendanceJsonRequest request = request("not-base64", "attendance.jpg", "image/jpeg");

        assertThatThrownBy(() -> mapper.toMultipartFile(request))
                .isInstanceOfSatisfying(MobileAttendanceException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("INVALID_IMAGE"));
    }

    private MobileAttendanceJsonRequest request(String imageBase64, String imageFileName, String contentType) {
        return new MobileAttendanceJsonRequest(
                101L,
                new BigDecimal("19.0760000"),
                new BigDecimal("72.8777000"),
                "Mumbai Office",
                imageBase64,
                imageFileName,
                contentType);
    }

    private MobileAttendanceMarkJsonRequest markRequest(
            MobileAttendanceAction attendanceAction,
            String imageBase64,
            String imageFileName,
            String contentType) {
        return new MobileAttendanceMarkJsonRequest(
                attendanceAction,
                101L,
                new BigDecimal("19.0760000"),
                new BigDecimal("72.8777000"),
                "Mumbai Office",
                imageBase64,
                imageFileName,
                contentType);
    }
}
