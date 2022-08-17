package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.time.DateRange;
import com.tramchester.repository.WriteableTransportData;
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

    private final WriteableTransportData container;
    private final ServiceGroups serviceGroups;
    private final Set<String> skippedSchedules; // services skipped due to train category etc.
    private final Set<String> unmatchedCancellations;

    public RailServiceGroups(WriteableTransportData container) {
        this.container = container;
        skippedSchedules = new HashSet<>();
        serviceGroups = new ServiceGroups();
        unmatchedCancellations = new HashSet<>();
    }

    public void applyCancellation(BasicSchedule basicSchedule) {
        final String uniqueTrainId = basicSchedule.getUniqueTrainId();
        List<MutableService> existingServices = serviceGroups.servicesFor(uniqueTrainId);
        if (existingServices.isEmpty()) {
            if (!skippedSchedules.contains(uniqueTrainId)) {
                unmatchedCancellations.add(uniqueTrainId);
                //logger.warn("Cancel: No existing records for " + uniqueTrainId);
            }
            return;
        }

        final Set<MutableService> cancellationApplies = filterByScheduleDates(basicSchedule, existingServices);

        if (cancellationApplies.isEmpty()) {
            logger.warn(format("Cancel: matched no services for %s, date range %s %s",
                    uniqueTrainId, basicSchedule.getDateRange(), basicSchedule.getDaysOfWeek()));
            return;
        }

        cancellationApplies.forEach(service -> {
                    MutableServiceCalendar calendar = service.getMutableCalendar();
                    addServiceExceptions(calendar, basicSchedule.getDateRange(), basicSchedule.getDaysOfWeek());
                });

    }

    @NotNull
    private Set<MutableService> filterByScheduleDates(BasicSchedule basicSchedule, List<MutableService> existingServices) {

        DateRange dateRange = basicSchedule.getDateRange();

        return existingServices.stream().
                filter(service -> dateRange.overlapsWith(service.getCalendar().getDateRange())).
                //filter(service -> service.getCalendar().overlapsDatesWith(basicSchedule.getDateRange())).
                collect(Collectors.toSet());
    }

    MutableService getOrCreateService(BasicSchedule schedule, boolean isOverlay) {
        String uniqueTrainId = schedule.getUniqueTrainId();

        final IdFor<Service> serviceId = getServiceIdFor(schedule, isOverlay);

        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(schedule.getDateRange(), schedule.getDaysOfWeek());
        service.setCalendar(calendar);

        if (isOverlay) {
            List<MutableService> existingServices = serviceGroups.servicesFor(uniqueTrainId);
            if (existingServices.isEmpty()) {
                logger.info("Overlap: No existing services found for " + uniqueTrainId);
            }
            Set<MutableService> impactedServices = filterByScheduleDates(schedule, existingServices);
            if (impactedServices.isEmpty() && !existingServices.isEmpty()) {
                logger.info(format("Overlap: No existing services overlapped on date range (%s) for %s ",
                        schedule.getDateRange(), uniqueTrainId));
            }
            impactedServices.forEach(impactedService -> {
                final IdFor<Service> impactedServiceId = impactedService.getId();
                if (impactedServiceId.equals(serviceId)) {
                    // only happens if over dates exactly match existing service
                    logger.info(format("Overlap: Marking existing service %s as cancelled", impactedServiceId));
                    impactedService.markCancelled();
                } else {
                    // mark overlay dates as no longer applying
                    logger.debug(format("Overlap: Marking existing service %s as cancelled for %s %s",
                            impactedServiceId, schedule.getDateRange(), schedule.getDaysOfWeek()));
                    MutableServiceCalendar impactedCalendar = impactedService.getMutableCalendar();
                    addServiceExceptions(impactedCalendar, schedule.getDateRange(), schedule.getDaysOfWeek());
                }
            });
        }

        container.addService(service);
        serviceGroups.addService(uniqueTrainId, service);

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
        
//        String text = format("%s:%s:%s", schedule.getUniqueTrainId(), startDate, endDate);
        String text = schedule.getUniqueTrainId() +":"+ startDate +":"+ endDate;
        if (isOverlay) {
            text = text + "OVERLAY";
        }
        return StringIdFor.createId(text);
    }

    public void reportUnmatchedCancellations() {
        if (unmatchedCancellations.isEmpty()) {
            return;
        }
        StringBuilder ids = new StringBuilder();
        unmatchedCancellations.forEach(item -> ids.append(" ").append(item));
        logger.warn("The following " + unmatchedCancellations.size() +
                " cancellations records were unmatched (unique train ids) " + ids);
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
