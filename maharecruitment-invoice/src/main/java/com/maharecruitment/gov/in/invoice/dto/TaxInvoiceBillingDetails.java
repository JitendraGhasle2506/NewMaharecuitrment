package com.maharecruitment.gov.in.invoice.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaxInvoiceBillingDetails {
    @Pattern(regexp = "(?:[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z])?",
            message = "Enter a valid 15-character GST number, or leave it blank for an unregistered client.")
    private String clientGstNumber;

    @NotBlank(message = "Place Of Supply is required.")
    @Size(max = 100, message = "Place Of Supply must be at most 100 characters.")
    private String placeOfSupply;

    @NotBlank(message = "Request Id is required.")
    @Size(max = 100, message = "Request Id must be at most 100 characters.")
    private String requestId;

    @NotNull(message = "Work Order Date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workOrderDate;

    @NotBlank(message = "To recipient / department is required.")
    @Size(max = 255, message = "To recipient / department must be at most 255 characters.")
    private String billedTo;

    @NotBlank(message = "Billing address is required.")
    @Size(max = 2000, message = "Billing address must be at most 2000 characters.")
    private String billingAddress;

    public static TaxInvoiceBillingDetails from(TaxInvoiceView invoice) {
        TaxInvoiceBillingDetails details = new TaxInvoiceBillingDetails();
        details.setClientGstNumber(invoice.getClientGstNumber());
        details.setPlaceOfSupply(invoice.getPlaceOfSupply());
        details.setRequestId(invoice.getRequestId());
        details.setWorkOrderDate(invoice.getDeptRefDate());
        details.setBilledTo(invoice.getBilledTo());
        details.setBillingAddress(invoice.getBillingAddress());
        return details;
    }

    public void applyTo(TaxInvoiceView invoice) {
        String gst = clientGstNumber == null || clientGstNumber.isBlank() ? null : clientGstNumber.trim();
        invoice.setClientGstNumber(gst);
        invoice.setClientGstinAvailable(gst != null);
        invoice.setPlaceOfSupply(placeOfSupply.trim());
        invoice.setRequestId(requestId.trim());
        invoice.setDeptRefNumber(requestId.trim());
        invoice.setDeptRefDate(workOrderDate);
        invoice.setBilledTo(billedTo.trim());
        invoice.setBillingAddress(billingAddress.trim());
    }
}
