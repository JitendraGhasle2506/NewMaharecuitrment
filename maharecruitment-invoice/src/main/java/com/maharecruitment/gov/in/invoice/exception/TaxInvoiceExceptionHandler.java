package com.maharecruitment.gov.in.invoice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.maharecruitment.gov.in.invoice.controller.TaxInvoiceController;

import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice(basePackageClasses = TaxInvoiceController.class, annotations = Controller.class)
public class TaxInvoiceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TaxInvoiceExceptionHandler.class);

    @ExceptionHandler(TaxInvoiceException.class)
    public String handleTaxInvoiceException(
            TaxInvoiceException ex,
            HttpServletResponse response,
            Model model) {
        HttpStatus status = resolveStatus(ex);

        log.warn("Tax invoice request failed. status={} message={}", status.value(), ex.getMessage(), ex);

        response.setStatus(status.value());
        model.addAttribute("status", status.value());
        model.addAttribute("error", resolveTitle(status));
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("icon", resolveIcon(status));

        return "error/custom-error";
    }

    private HttpStatus resolveStatus(TaxInvoiceException ex) {
        if (ex instanceof TaxInvoiceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof TaxInvoiceNotReadyException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String resolveTitle(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Tax Invoice Not Found";
            case CONFLICT -> "Tax Invoice Preview Not Ready";
            case BAD_REQUEST -> "Tax Invoice Data Error";
            default -> "Tax Invoice Error";
        };
    }

    private String resolveIcon(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "404.svg";
            case CONFLICT, BAD_REQUEST -> "400.svg";
            default -> "500.svg";
        };
    }
}
