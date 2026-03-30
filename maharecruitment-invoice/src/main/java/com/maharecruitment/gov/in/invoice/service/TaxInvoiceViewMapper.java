package com.maharecruitment.gov.in.invoice.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceLineItemView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceEntity;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceLineItemEntity;

@Component
public class TaxInvoiceViewMapper {

    private static final DateTimeFormatter PERIOD_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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

        LocalDate requiredPeriodStartDate = entity.getDeptRefDate() != null
                ? entity.getDeptRefDate()
                : entity.getTiDate();

        List<TaxInvoiceLineItemView> lineItems = entity.getLineItems() == null
                ? List.of()
                : entity.getLineItems().stream()
                        .map(lineItem -> toLineItemView(lineItem, requiredPeriodStartDate))
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
        applyLegacySummary(view, lineItems, entity);
        view.setQrCodeDataUrl(qrCodeGenerator.generateDataUrl(view));
        return view;
    }

    public TaxInvoiceLineItemView toLineItemView(
            DepartmentTaxInvoiceLineItemEntity entity,
            LocalDate requiredPeriodStartDate) {
        if (entity == null) {
            return null;
        }

        return TaxInvoiceLineItemView.builder()
                .resourceRequirementId(entity.getDepartmentProjectResourceRequirementId())
                .lineNumber(entity.getLineNumber())
                .description(entity.getDescription())
                .requiredPeriodDisplay(buildRequiredPeriodDisplay(requiredPeriodStartDate, entity.getDurationInMonths()))
                .sacHsn(entity.getSacHsn())
                .quantity(entity.getQuantity())
                .durationInMonths(entity.getDurationInMonths())
                .ratePerMonth(entity.getRatePerMonth())
                .totalAmount(entity.getTotalAmount())
                .ratePerMonthDisplay(displayFormatter.formatAmount(entity.getRatePerMonth()))
                .totalAmountDisplay(displayFormatter.formatAmount(entity.getTotalAmount()))
                .build();
    }

    private String buildRequiredPeriodDisplay(LocalDate startDate, Integer durationInMonths) {
        if (startDate == null) {
            return durationInMonths == null ? "-" : durationInMonths + " month(s)";
        }
        if (durationInMonths == null || durationInMonths < 1) {
            return PERIOD_DATE_FORMATTER.format(startDate);
        }

        LocalDate endDate = startDate.plusMonths(durationInMonths.longValue()).minusDays(1);
        return PERIOD_DATE_FORMATTER.format(startDate) + " to " + PERIOD_DATE_FORMATTER.format(endDate);
    }

    private void applyLegacySummary(
            TaxInvoiceView view,
            List<TaxInvoiceLineItemView> lineItems,
            DepartmentTaxInvoiceEntity entity) {
        if (view == null) {
            return;
        }

        if (lineItems == null || lineItems.isEmpty()) {
            view.setLegacySacHsn("-");
            view.setLegacyQuantityTotal(0);
            view.setLegacyDurationDisplay("-");
            view.setLegacyRatePerMonthTotalDisplay(displayFormatter.formatAmount(BigDecimal.ZERO));
            view.setLegacyLineAmountTotalDisplay(displayFormatter.formatAmount(BigDecimal.ZERO));
            return;
        }

        BigDecimal monthlyRateTotal = lineItems.stream()
                .map(item -> {
                    BigDecimal rate = item.getRatePerMonth() == null ? BigDecimal.ZERO : item.getRatePerMonth();
                    Integer quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                    return rate.multiply(BigDecimal.valueOf(quantity.longValue()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantityTotal = lineItems.stream()
                .map(TaxInvoiceLineItemView::getQuantity)
                .filter(quantity -> quantity != null && quantity > 0)
                .mapToInt(Integer::intValue)
                .sum();

        LinkedHashSet<Integer> distinctDurations = lineItems.stream()
                .map(TaxInvoiceLineItemView::getDurationInMonths)
                .filter(duration -> duration != null && duration > 0)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        String legacyDurationDisplay;
        if (distinctDurations.isEmpty()) {
            legacyDurationDisplay = "-";
        } else if (distinctDurations.size() == 1) {
            legacyDurationDisplay = distinctDurations.iterator().next().toString();
        } else {
            legacyDurationDisplay = "Mixed";
        }

        String legacySacHsn = lineItems.stream()
                .map(TaxInvoiceLineItemView::getSacHsn)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("-");

        view.setLegacySacHsn(legacySacHsn);
        view.setLegacyQuantityTotal(quantityTotal);
        view.setLegacyDurationDisplay(legacyDurationDisplay);
        view.setLegacyRatePerMonthTotalDisplay(displayFormatter.formatAmount(monthlyRateTotal));
        view.setLegacyLineAmountTotalDisplay(displayFormatter.formatAmount(entity.getBaseAmount()));
    }
}
