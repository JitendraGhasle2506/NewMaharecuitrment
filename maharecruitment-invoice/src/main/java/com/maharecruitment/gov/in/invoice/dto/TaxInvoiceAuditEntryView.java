package com.maharecruitment.gov.in.invoice.dto;

import java.time.LocalDateTime;

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
public class TaxInvoiceAuditEntryView {

    private String actionLabel;
    private String actorName;
    private Long actorUserId;
    private String actorLoginId;
    private LocalDateTime actionTimestamp;
    private String details;
}
