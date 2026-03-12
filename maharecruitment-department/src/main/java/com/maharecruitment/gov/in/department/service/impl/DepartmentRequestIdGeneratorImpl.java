package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.department.entity.DepartmentRequestSequenceEntity;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.repository.DepartmentRequestSequenceRepository;
import com.maharecruitment.gov.in.department.service.DepartmentRequestIdGenerator;

@Service
public class DepartmentRequestIdGeneratorImpl implements DepartmentRequestIdGenerator {

    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_SEQUENCE_PER_DAY = 9_999;

    private final DepartmentRequestSequenceRepository requestSequenceRepository;

    public DepartmentRequestIdGeneratorImpl(DepartmentRequestSequenceRepository requestSequenceRepository) {
        this.requestSequenceRepository = requestSequenceRepository;
    }

    @Override
    @Transactional
    public String generate(String applicationType) {
        LocalDate requestDate = LocalDate.now();

        DepartmentRequestSequenceEntity sequenceEntity = requestSequenceRepository
                .findForUpdate(requestDate, applicationType)
                .orElseGet(() -> createNewSequenceRow(requestDate, applicationType));

        int nextSequence = sequenceEntity.getLastSequence() + 1;
        if (nextSequence > MAX_SEQUENCE_PER_DAY) {
            throw new DepartmentApplicationException(
                    "Daily request sequence limit reached for date " + requestDate + " and type " + applicationType
                            + ".");
        }

        sequenceEntity.setLastSequence(nextSequence);
        requestSequenceRepository.save(sequenceEntity);

        return "REQ-"
                + requestDate.format(REQUEST_DATE_FORMAT)
                + "-"
                + applicationType
                + String.format(Locale.ROOT, "%04d", nextSequence);
    }

    private DepartmentRequestSequenceEntity createNewSequenceRow(LocalDate requestDate, String typeCode) {
        DepartmentRequestSequenceEntity sequenceEntity = new DepartmentRequestSequenceEntity();
        sequenceEntity.setSequenceDate(requestDate);
        sequenceEntity.setRequestTypeCode(typeCode);
        sequenceEntity.setLastSequence(0);

        try {
            return requestSequenceRepository.saveAndFlush(sequenceEntity);
        } catch (DataIntegrityViolationException ex) {
            return requestSequenceRepository.findForUpdate(requestDate, typeCode)
                    .orElseThrow(() -> new DepartmentApplicationException(
                            "Unable to initialize request sequence for date " + requestDate + " and type "
                                    + typeCode + "."));
        }
    }

}
