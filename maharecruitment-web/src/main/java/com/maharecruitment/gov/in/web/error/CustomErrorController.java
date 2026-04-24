package com.maharecruitment.gov.in.web.error;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {

        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusObj != null ? Integer.parseInt(statusObj.toString()) : 500;

        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        model.addAttribute("status", status);
        model.addAttribute("error", getTitle(status));
        model.addAttribute("message", message != null ? message : getDefaultMessage(status));
        model.addAttribute("icon", getIcon(status));

        return "error/custom-error";  // from web/templates/error/custom-error.html
    }

    private String getTitle(int code) {
        return switch (code) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Access Denied";
            case 404 -> "Page Not Found";
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
            case 500 -> "Server error occurred.";
            default -> "Unexpected error.";
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
