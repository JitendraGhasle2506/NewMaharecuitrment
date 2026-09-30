package com.maharecruitment.gov.in.web.dto.agency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class AgencyInterviewTimeSlotTest {

    @Test
    void exposesApprovedInterviewSlotsAndTheirStartTimes() {
        assertThat(AgencyInterviewTimeSlot.values())
                .extracting(AgencyInterviewTimeSlot::getDisplayValue)
                .containsExactly("10:00 AM - 11:30 AM", "2:00 PM - 5:00 PM");
        assertThat(AgencyInterviewTimeSlot.MORNING.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(AgencyInterviewTimeSlot.AFTERNOON.getStartTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void rejectsValuesOutsideApprovedSlots() {
        assertThat(AgencyInterviewTimeSlot.fromDisplayValue("10:00 AM - 11:30 AM"))
                .contains(AgencyInterviewTimeSlot.MORNING);
        assertThat(AgencyInterviewTimeSlot.fromDisplayValue("9:00 AM - 10:00 AM")).isEmpty();
    }
}
