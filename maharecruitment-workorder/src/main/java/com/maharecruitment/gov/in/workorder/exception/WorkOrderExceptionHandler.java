package com.maharecruitment.gov.in.workorder.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.maharecruitment.gov.in.workorder.controller.HrWorkOrderController;

import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice(basePackageClasses = HrWorkOrderController.class, annotations = Controller.class)
public class WorkOrderExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderExceptionHandler.class);

    @ExceptionHandler(WorkOrderException.class)
    public String handleWorkOrderException(
            WorkOrderException ex,
            HttpServletResponse response,
            Model model) {
        HttpStatus status = resolveStatus(ex);

        log.warn("Work-order request failed. status={} message={}", status.value(), ex.getMessage(), ex);

        response.setStatus(status.value());
        model.addAttribute("status", status.value());
        model.addAttribute("error", resolveTitle(status));
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("icon", resolveIcon(status));
        return "error/custom-error";
    }

    private HttpStatus resolveStatus(WorkOrderException ex) {
        if (ex instanceof WorkOrderNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof WorkOrderValidationException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String resolveTitle(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "Work Order Not Found";
        }
        if (status == HttpStatus.CONFLICT) {
            return "Work Order Validation Error";
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return "Work Order Data Error";
        }
        return "Work Order Error";
    }

    private String resolveIcon(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "404.svg";
        }
        if (status == HttpStatus.CONFLICT || status == HttpStatus.BAD_REQUEST) {
            return "400.svg";
        }
        return "500.svg";
    }
}
