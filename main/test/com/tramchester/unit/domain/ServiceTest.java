package com.tramchester.unit.domain;

import com.tramchester.domain.Service;
import com.tramchester.domain.ServiceCalendar;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {

    @Test
    void shouldNoticeNoDatesSet() {
        Service service = new Service("svcXXX");
        assertFalse(service.hasCalendar());

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        ServiceCalendar serviceCalendar = new ServiceCalendar(startDate, endDate, TestEnv.allDays());

        service.setCalendar(serviceCalendar);

        assertTrue(service.hasCalendar());

    }

}