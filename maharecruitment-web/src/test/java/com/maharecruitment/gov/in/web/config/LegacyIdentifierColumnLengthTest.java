package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceEntity;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;

import jakarta.persistence.Column;

class LegacyIdentifierColumnLengthTest {

    private static final int NON_SHRINKING_LENGTH = 255;

    @Test
    void legacyIdentifierColumnsDoNotAskHibernateToShrinkExistingData() throws Exception {
        assertLength(AgencyMaster.class, "panNumber");
        assertLength(AgencyMaster.class, "gstNumber");
        assertLength(AgencyMaster.class, "bankAccountNumber");

        assertLength(DepartmentTaxInvoiceEntity.class, "clientGstNumber");
        assertLength(DepartmentTaxInvoiceEntity.class, "cinNumber");
        assertLength(DepartmentTaxInvoiceEntity.class, "panNumber");
        assertLength(DepartmentTaxInvoiceEntity.class, "gstNumber");
        assertLength(DepartmentTaxInvoiceEntity.class, "accountNumber");

        assertLength(EmployeeEntity.class, "panNumber");
        assertLength(EmployeeEntity.class, "aadhaarNumber");
        assertLength(AgencyCandidatePreOnboardingEntity.class, "panNumber");
        assertLength(AgencyCandidatePreOnboardingEntity.class, "aadhaarNumber");

        assertLength(MahaItProfile.class, "cinNumber");
        assertLength(MahaItProfile.class, "panNumber");
        assertLength(MahaItProfile.class, "gstNumber");
        assertLength(MahaItProfile.class, "accountNumber");
    }

    private void assertLength(Class<?> entityClass, String fieldName) throws Exception {
        Column column = entityClass.getDeclaredField(fieldName).getAnnotation(Column.class);

        assertThat(column)
                .as("%s.%s @Column", entityClass.getSimpleName(), fieldName)
                .isNotNull();
        assertThat(column.length())
                .as("%s.%s length", entityClass.getSimpleName(), fieldName)
                .isEqualTo(NON_SHRINKING_LENGTH);
    }
}
