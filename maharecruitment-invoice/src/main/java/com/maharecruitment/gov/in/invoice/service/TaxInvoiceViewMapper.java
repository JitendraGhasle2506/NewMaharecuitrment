package com.maharecruitment.gov.in.invoice.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceLineItemView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceEntity;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceLineItemEntity;

@Component
public class TaxInvoiceViewMapper {

    private final IndianCurrencyToWordsConverter currencyToWordsConverter;
    private final TaxInvoiceDisplayFormatter displayFormatter;
    private final TaxInvoiceQrCodeGenerator qrCodeGenerator;

    public TaxInvoiceViewMapper(IndianCurrencyToWordsConverter currencyToWordsConverter,
            TaxInvoiceDisplayFormatter displayFormatter,
            TaxInvoiceQrCodeGenerator qrCodeGenerator) {
        this.currencyToWordsConverter = currencyToWordsConverter;
        this.displayFormatter = displayFormatter;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    public TaxInvoiceView toView(DepartmentTaxInvoiceEntity entity) {
        if (entity == null) {
            return null;
        }

        List<TaxInvoiceLineItemView> lineItems = entity.getLineItems() == null
                ? List.of()
                : entity.getLineItems().stream()
                        .map(this::toLineItemView)
                        .toList();

        String amountInWords = StringUtils.hasText(entity.getAmountInWords())
                ? entity.getAmountInWords()
                : currencyToWordsConverter.convert(entity.getTotalAmount());

        TaxInvoiceView view = TaxInvoiceView.builder()
                .departmentTaxInvoiceId(entity.getDepartmentTaxInvoiceId())
                .departmentProjectApplicationId(entity.getDepartmentProjectApplicationId())
                .departmentRegistrationId(entity.getDepartmentRegistrationId())
                .requestId(entity.getRequestId())
                .deptRefNumber(entity.getRequestId())
                .tiDate(entity.getTiDate())
                .deptRefDate(entity.getDeptRefDate())
                .tiNumber(entity.getTiNumber())
                .projectName(entity.getProjectName())
                .projectCode(entity.getProjectCode())
                .pmName(entity.getPmName())
                .billedTo(entity.getBilledTo())
                .billingAddress(entity.getBillingAddress())
                .clientGstinAvailable(Boolean.TRUE.equals(entity.getClientGstinAvailable()))
                .clientGstNumber(entity.getClientGstNumber())
                .placeOfSupply(entity.getPlaceOfSupply())
                .baseAmount(entity.getBaseAmount())
                .cgstRate(entity.getCgstRate())
                .cgstAmount(entity.getCgstAmount())
                .sgstRate(entity.getSgstRate())
                .sgstAmount(entity.getSgstAmount())
                .taxAmount(entity.getTaxAmount())
                .totalAmount(entity.getTotalAmount())
                .companyName(entity.getCompanyName())
                .companyAddress(entity.getCompanyAddress())
                .cinNumber(entity.getCinNumber())
                .panNumber(entity.getPanNumber())
                .gstNumber(entity.getGstNumber())
                .bankName(entity.getBankName())
                .branchName(entity.getBranchName())
                .accountHolderName(entity.getAccountHolderName())
                .accountNumber(entity.getAccountNumber())
                .ifscCode(entity.getIfscCode())
                .amountInWords(amountInWords)
                .lineItems(new ArrayList<>(lineItems))
                .build();

        view.setBaseAmountDisplay(displayFormatter.formatAmount(entity.getBaseAmount()));
        view.setCgstRateDisplay(displayFormatter.formatRate(entity.getCgstRate()));
        view.setCgstAmountDisplay(displayFormatter.formatAmount(entity.getCgstAmount()));
        view.setSgstRateDisplay(displayFormatter.formatRate(entity.getSgstRate()));
        view.setSgstAmountDisplay(displayFormatter.formatAmount(entity.getSgstAmount()));
        view.setTaxAmountDisplay(displayFormatter.formatAmount(entity.getTaxAmount()));
        view.setTotalAmountDisplay(displayFormatter.formatAmount(entity.getTotalAmount()));
        view.setQrCodeDataUrl(qrCodeGenerator.generateDataUrl(view));
        return view;
    }

    public TaxInvoiceLineItemView toLineItemView(DepartmentTaxInvoiceLineItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return TaxInvoiceLineItemView.builder()
                .resourceRequirementId(entity.getDepartmentProjectResourceRequirementId())
                .lineNumber(entity.getLineNumber())
                .description(entity.getDescription())
                .sacHsn(entity.getSacHsn())
                .quantity(entity.getQuantity())
                .durationInMonths(entity.getDurationInMonths())
                .ratePerMonth(entity.getRatePerMonth())
                .totalAmount(entity.getTotalAmount())
                .build();
    }
}
