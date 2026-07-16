package com.maharecruitment.gov.in.web.error;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.maharecruitment.gov.in.web.dto.mobile.MobileApiError;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping(path = "/error", method = {
            RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.DELETE, RequestMethod.PATCH
    })
    public Object handleError(HttpServletRequest request, HttpServletResponse response, Model model) {

        int status = resolveStatus(request);
        String message = resolveMessage(request, status);
        response.setStatus(status);

        if (isMobileApiRequest(request)) {
            return ResponseEntity.status(status).body(MobileApiError.of(getMobileErrorCode(status), message));
        }

        model.addAttribute("status", status);
        model.addAttribute("error", getTitle(status));
        model.addAttribute("message", message);
        model.addAttribute("icon", getIcon(status));

        return "error/custom-error";  // from web/templates/error/custom-error.html
    }

    private int resolveStatus(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusObj == null) {
            return 500;
        }
        try {
            return Integer.parseInt(statusObj.toString());
        } catch (NumberFormatException ex) {
            return 500;
        }
    }

    private String resolveMessage(HttpServletRequest request, int status) {
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message instanceof String text && !text.isBlank()) {
            return text;
        }
        return getDefaultMessage(status);
    }

    private boolean isMobileApiRequest(HttpServletRequest request) {
        Object errorUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String requestUri = errorUri != null ? errorUri.toString() : request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        return requestUri.startsWith("/api/mobile");
    }

    private String getTitle(int code) {
        return switch (code) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Access Denied";
            case 404 -> "Page Not Found";
            case 405 -> "Method Not Allowed";
            case 415 -> "Unsupported Media Type";
            case 500 -> "Internal Server Error";
            default -> "Unexpected Error";
        };
    }

    private String getDefaultMessage(int code) {
        return switch (code) {
            case 400 -> "Your request cannot be processed.";
            case 401 -> "You must log in.";
            case 403 -> "Access Denied.";
            case 404 -> "Resource not found.";
            case 405 -> "Request method is not supported for this endpoint.";
            case 415 -> "Content type is not supported for this endpoint.";
            case 500 -> "Server error occurred.";
            default -> "Unexpected error.";
        };
    }

    private String getMobileErrorCode(int code) {
        return switch (code) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 500 -> "INTERNAL_SERVER_ERROR";
            default -> "UNEXPECTED_ERROR";
        };
    }

    private String getIcon(int code) {
        return switch (code) {
            case 400 -> "400.svg";
            case 401 -> "401.svg";
            case 403 -> "403.svg";
            case 404 -> "404.svg";
            case 500 -> "500.svg";
            default -> "error.svg";
        };
    }
}
