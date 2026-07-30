package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentRequestSequenceEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentRequestSequenceRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentRequestIdGenerator;

@Service
public class RecruitmentRequestIdGeneratorImpl implements RecruitmentRequestIdGenerator {

    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_SEQUENCE_PER_DAY = 9_999;

    private final RecruitmentRequestSequenceRepository requestSequenceRepository;

    public RecruitmentRequestIdGeneratorImpl(RecruitmentRequestSequenceRepository requestSequenceRepository) {
        this.requestSequenceRepository = requestSequenceRepository;
    }

    @Override
    @Transactional
    public String generate(String requestTypeCode) {
        LocalDate requestDate = LocalDate.now();
        String normalizedTypeCode = normalizeTypeCode(requestTypeCode);

        RecruitmentRequestSequenceEntity sequenceEntity = requestSequenceRepository
                .findForUpdate(requestDate, normalizedTypeCode)
                .orElseGet(() -> createNewSequenceRow(requestDate, normalizedTypeCode));

        int nextSequence = sequenceEntity.getLastSequence() + 1;
        if (nextSequence > MAX_SEQUENCE_PER_DAY) {
            throw new RecruitmentNotificationException(
                    "Daily recruitment request sequence limit reached for date "
                            + requestDate
                            + " and type "
                            + normalizedTypeCode
                            + ".");
        }

        sequenceEntity.setLastSequence(nextSequence);
        requestSequenceRepository.save(sequenceEntity);

        return "REQ-"
                + requestDate.format(REQUEST_DATE_FORMAT)
                + "-"
                + normalizedTypeCode
                + String.format(Locale.ROOT, "%04d", nextSequence);
    }

    private RecruitmentRequestSequenceEntity createNewSequenceRow(LocalDate requestDate, String typeCode) {
        RecruitmentRequestSequenceEntity sequenceEntity = new RecruitmentRequestSequenceEntity();
        sequenceEntity.setSequenceDate(requestDate);
        sequenceEntity.setRequestTypeCode(typeCode);
        sequenceEntity.setLastSequence(0);

        try {
            return requestSequenceRepository.saveAndFlush(sequenceEntity);
        } catch (DataIntegrityViolationException ex) {
            return requestSequenceRepository.findForUpdate(requestDate, typeCode)
                    .orElseThrow(() -> new RecruitmentNotificationException(
                            "Unable to initialize recruitment request sequence for date "
                                    + requestDate
                                    + " and type "
                                    + typeCode
                                    + "."));
        }
    }

    private String normalizeTypeCode(String requestTypeCode) {
        if (requestTypeCode == null || requestTypeCode.isBlank()) {
            throw new RecruitmentNotificationException("Recruitment request type is required.");
        }
        String normalized = requestTypeCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 1) {
            throw new RecruitmentNotificationException("Recruitment request type must be a single character.");
        }
        return normalized;
    }
}
