package com.maharecruitment.gov.in.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxInvoiceView {

    private Long departmentTaxInvoiceId;
    private Long departmentProjectApplicationId;
    private Long departmentRegistrationId;
    private String requestId;
    private String deptRefNumber;
    private LocalDate tiDate;
    private LocalDate deptRefDate;
    private String tiNumber;
    private String projectName;
    private String projectCode;
    private String pmName;
    private String billedTo;
    private String billingAddress;
    private boolean clientGstinAvailable;
    private String clientGstNumber;
    private String placeOfSupply;
    private BigDecimal baseAmount;
    private BigDecimal cgstRate;
    private BigDecimal cgstAmount;
    private BigDecimal sgstRate;
    private BigDecimal sgstAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String companyName;
    private String companyAddress;
    private String cinNumber;
    private String panNumber;
    private String gstNumber;
    private String bankName;
    private String branchName;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String amountInWords;
    private String baseAmountDisplay;
    private String cgstRateDisplay;
    private String cgstAmountDisplay;
    private String sgstRateDisplay;
    private String sgstAmountDisplay;
    private String taxAmountDisplay;
    private String totalAmountDisplay;
    private String qrCodeDataUrl;

    @Builder.Default
    private List<TaxInvoiceLineItemView> lineItems = new ArrayList<>();
}
