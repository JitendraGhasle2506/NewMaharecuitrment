package com.maharecruitment.gov.in.web.service.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceMarkJsonRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceJsonRequest;

@Component
public class MobileAttendanceJsonImageMapper {

    private final MobileBase64ImageMapper imageMapper;

    public MobileAttendanceJsonImageMapper(MobileBase64ImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    public MultipartFile toMultipartFile(MobileAttendanceJsonRequest request) {
        if (request == null || !StringUtils.hasText(request.imageBase64())) {
            throw invalidImage("Image data is required.");
        }

        return toMultipartFile(request.imageBase64(), request.imageFileName(), request.imageContentType());
    }

    public MultipartFile toMultipartFile(MobileAttendanceMarkJsonRequest request) {
        if (request == null || !StringUtils.hasText(request.imageBase64())) {
            throw invalidImage("Image data is required.");
        }

        return toMultipartFile(request.imageBase64(), request.imageFileName(), request.imageContentType());
    }

    private MultipartFile toMultipartFile(String imageBase64, String imageFileName, String imageContentType) {
        try {
            return imageMapper.toMultipartFile(imageBase64, imageFileName, imageContentType, "image", "attendance");
        } catch (MobileBase64ImageMapper.InvalidBase64ImageException ex) {
            throw invalidImage(ex.getMessage());
        }
    }

    private MobileAttendanceException invalidImage(String message) {
        return new MobileAttendanceException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", message);
    }
}
