package com.maharecruitment.gov.in.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public String handleAppException(AppException ex, Model model) {
        LOGGER.warn("Application exception handled: {}", ex.getMessage(), ex);

        model.addAttribute("status", 400);
        model.addAttribute("error", "Application Error");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("icon", "400.svg");

        return "error/custom-error";
    }
}
