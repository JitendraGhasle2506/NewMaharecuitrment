package com.maharecruitment.gov.in.workorder.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderSequenceEntity;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderType;
import com.maharecruitment.gov.in.workorder.repository.WorkOrderSequenceRepository;

@Service
public class WorkOrderNumberGenerator {

    private final WorkOrderSequenceRepository workOrderSequenceRepository;

    public WorkOrderNumberGenerator(WorkOrderSequenceRepository workOrderSequenceRepository) {
        this.workOrderSequenceRepository = workOrderSequenceRepository;
    }

    @Transactional
    public String generate(WorkOrderType workOrderType, LocalDate workOrderDate) {
        WorkOrderType resolvedType = workOrderType == null ? WorkOrderType.NEW : workOrderType;
        String financialYear = resolveFinancialYear(workOrderDate);
        String prefix = resolvedType == WorkOrderType.EXTENSION ? "WO-EXT" : "WO";
        String sequenceKey = prefix + "-" + financialYear;

        WorkOrderSequenceEntity sequence = workOrderSequenceRepository.findBySequenceKeyForUpdate(sequenceKey)
                .orElseGet(() -> WorkOrderSequenceEntity.builder()
                        .sequenceKey(sequenceKey)
                        .lastSequence(0L)
                        .build());

        long nextSequence = (sequence.getLastSequence() == null ? 0L : sequence.getLastSequence()) + 1L;
        sequence.setLastSequence(nextSequence);
        workOrderSequenceRepository.save(sequence);

        return prefix + "/" + financialYear + "/" + String.format("%05d", nextSequence);
    }

    private String resolveFinancialYear(LocalDate workOrderDate) {
        LocalDate resolvedDate = workOrderDate == null ? LocalDate.now() : workOrderDate;
        int startYear = resolvedDate.getMonthValue() >= 4 ? resolvedDate.getYear() : resolvedDate.getYear() - 1;
        int endYear = (startYear + 1) % 100;
        return startYear + "-" + String.format("%02d", endYear);
    }
}
