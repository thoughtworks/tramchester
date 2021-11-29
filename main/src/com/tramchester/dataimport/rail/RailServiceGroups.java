package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.MutableServiceCalendar;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.time.DateRange;
import com.tramchester.repository.TransportDataContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RailServiceGroups {
    private static final Logger logger = LoggerFactory.getLogger(RailServiceGroups.class);

    private final TransportDataContainer container;
    private final ServiceGroups serviceGroups;
    private final Set<String> skippedSchedules; // services skipped due to train category etc.

    public RailServiceGroups(TransportDataContainer container) {
        this.container = container;
        skippedSchedules = new HashSet<>();
        serviceGroups = new ServiceGroups();
    }

    public void applyCancellation(BasicSchedule basicSchedule) {
        List<MutableService> existingServices = serviceGroups.servicesFor(basicSchedule.getUniqueTrainId());
        if (existingServices.isEmpty()) {
            if (!skippedSchedules.contains(basicSchedule.getUniqueTrainId())) {
                logger.warn("Could not apply cancellation, no existing records for " + basicSchedule);
            }
            return;
        }

        final Set<MutableService> cancellationApplies = filterByScheduleDates(basicSchedule, existingServices);

        if (cancellationApplies.isEmpty()) {
            logger.warn("Found matching services, but cancellation does not apply " + basicSchedule);
            return;
        }

        cancellationApplies.forEach(service -> {
                    MutableServiceCalendar calendar = service.getMutableCalendar();
                    addServiceExceptions(calendar, basicSchedule.getDateRange(), basicSchedule.getDaysOfWeek());
                });

    }

    @NotNull
    private Set<MutableService> filterByScheduleDates(BasicSchedule basicSchedule, List<MutableService> existingServices) {

        return existingServices.stream().
                filter(service -> service.getCalendar().overlapsDatesWith(basicSchedule.getDateRange())).
                collect(Collectors.toSet());
    }

    MutableService getOrCreateService(BasicSchedule schedule, boolean isOverlay) {
        String scheduleId = schedule.getUniqueTrainId();

        final IdFor<Service> serviceId = getServiceIdFor(schedule, isOverlay);
        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(schedule.getDateRange(), schedule.getDaysOfWeek());
        service.setCalendar(calendar);

        if (isOverlay) {
            List<MutableService> existingServices = serviceGroups.servicesFor(scheduleId);
            Set<MutableService> impactedServices = filterByScheduleDates(schedule, existingServices);
            if (impactedServices.isEmpty()) {
                logger.warn("Did not find servives for overlay: " + schedule);
            }
            impactedServices.forEach(impactedService -> {
                if (impactedService.getId().equals(serviceId)) {
                    // only happens if over dates exactly match existing service
                    impactedService.markCancelled();
                } else {
                    // mark overlay dates as no longer applying
                    MutableServiceCalendar impactedCalendar = impactedService.getMutableCalendar();
                    addServiceExceptions(impactedCalendar, schedule.getDateRange(), schedule.getDaysOfWeek());
                }
            });
        } else {
            container.addService(service);
            serviceGroups.addService(scheduleId, service);
        }

        return service;
    }

    private void addServiceExceptions(MutableServiceCalendar calendar, DateRange dateRange, Set<DayOfWeek> excludedDays) {
        LocalDate endDate = dateRange.getEndDate();
        LocalDate current = dateRange.getStartDate();
        while (!current.isAfter(endDate)) {
            if (excludedDays.contains(current.getDayOfWeek())) {
                calendar.excludeDate(current);
            }
            current = current.plusDays(1);
        }
    }

    public void recordSkip(BasicSchedule basicSchedule) {
        skippedSchedules.add(basicSchedule.getUniqueTrainId());
    }

    private IdFor<Service> getServiceIdFor(BasicSchedule schedule, boolean isOverlay) {
        final DateRange dateRange = schedule.getDateRange();
        final String startDate = dateRange.getStartDate().format(RailTimetableMapper.dateFormatter);
        final String endDate = dateRange.getEndDate().format(RailTimetableMapper.dateFormatter);
        String text = format("%s:%s:%s", schedule.getUniqueTrainId(), startDate, endDate);
        if (isOverlay) {
            text = text + "OVERLAY";
        }
        return StringIdFor.createId(text);
    }

    private static class ServiceGroups {
        private final Map<String, List<MutableService>> groups;

        private ServiceGroups() {
            groups = new HashMap<>();
        }

        public List<MutableService> servicesFor(String scheduleId) {
            if (!groups.containsKey(scheduleId)) {
                return Collections.emptyList();
            }
            return groups.get(scheduleId);
        }

        public void addService(String scheduleId, MutableService service) {
            if (!groups.containsKey(scheduleId)) {
                groups.put(scheduleId, new ArrayList<>());
            }
            groups.get(scheduleId).add(service);
        }
    }
}
