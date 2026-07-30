package com.maharecruitment.gov.in.master.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;

class AgencyMasterAuditLogTest {

    @Test
    void detailsUsesPostgresTextColumnInsteadOfJpaLob() throws NoSuchFieldException {
        Field details = AgencyMasterAuditLog.class.getDeclaredField("details");

        Column column = details.getAnnotation(Column.class);

        assertThat(details.getAnnotation(Lob.class)).isNull();
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("details");
        assertThat(column.columnDefinition()).isEqualTo("TEXT");
    }
}
