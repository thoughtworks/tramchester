package com.tramchester.dataimport.rail.records;

// 1 Record Identity 2 1-2 With the constant value ‘BS’.
// 2 Transaction Type 1 3-3 ‘N’ = New. ‘D’ = Delete. ‘R’ = Revise.
// 3 Train UID 6 4-9 Unique train Identifier.
// 4 Date Runs From 6 10-15 yymmdd
// 5 Date Runs To 6 16-21 yymmdd
// 6 Days Run 7 22-28
// 7 Bank Holiday Running 1 29-29
// 8 Train Status 1 30-30
// 9 Train Category 2 31-32
// 10 Train Identity 4 33-36
// 11 Headcode 4 37-40
// 12 Course Indicator 1 41-41
// 13 Profit Centre Code/ Train Service Code 8 42-49
// 14 Business Sector 1 50-50 Now used to contain the portion suffix for RSID
// 15 Power Type 3 51-53
// 16 Timing Load 4 54-57
// 17 Speed 3 58-60
// 18 Operating Chars 6 61-66
// 19 Train Class 1 67-67
// 20 Sleepers 1 68-68
// 21 Reservations 1 69-69
// 22 Connect Indicator 1 70-70
// 23 Catering Code 4 71-74
// 24 Service Branding 4 75-78
// 25 Spare 1 79-79
// 26 STP indicator 1 80-80 ‘C’ = STP cancellation of permanent schedule.
//   ‘N’ = New STP schedule. ‘O’ = STP overlay of permanent schedule. ‘P’ = Permanent.
//   Read in association with the Transaction Type in Field 2

import com.tramchester.dataimport.rail.RailRecordType;
import com.tramchester.dataimport.rail.records.reference.ShortTermPlanIndicator;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class BasicSchedule implements RailTimetableRecord {
    private static final Logger logger = LoggerFactory.getLogger(BasicSchedule.class);

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final RailRecordTransactionType transactionType;
    private final String uniqueTrainId;
    private final EnumSet<DayOfWeek> daysOfWeek;
    private final ShortTermPlanIndicator stpIndicator;
    private final String trainIdentity;
    private final TrainStatus trainStatus;
    private final TrainCategory trainCategory;

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.BasicSchedule;
    }

    public BasicSchedule(RailRecordTransactionType transactionType, String uniqueTrainId, LocalDate startDate, LocalDate endDate,
                         EnumSet<DayOfWeek> daysOfWeek, ShortTermPlanIndicator stpIndicator, String headcode,
                         TrainStatus trainStatus, TrainCategory trainCategory) {
        this.transactionType = transactionType;
        this.uniqueTrainId = uniqueTrainId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysOfWeek = daysOfWeek;
        this.stpIndicator = stpIndicator;
        this.trainIdentity = headcode;
        this.trainStatus = trainStatus;
        this.trainCategory = trainCategory;
    }

    public static BasicSchedule parse(String text, ProvidesNow providesNow) {
        char transactionTypeRaw = text.charAt(2); //RecordHelper.extract(text, 3, 4);
        RailRecordTransactionType transactionType = RailRecordTransactionType.parse(transactionTypeRaw);
        String uniqueTrainId = RecordHelper.extract(text, 4, 9+1);
        String headcode = RecordHelper.extract(text, 33, 36+1);
        LocalDate startDate = RecordHelper.extractDate(text, 10, 15+1, providesNow);
        LocalDate endDate = RecordHelper.extractDate(text, 16, 21+1, providesNow);
        EnumSet<DayOfWeek> daysOfWeek = extractDays(text, 21);
        char stpIndicatorRaw = text.charAt(80-1);
        char trainStatusRaw = text.charAt(29);
        String trainCategoryRaw = RecordHelper.extract(text, 31, 32+1);
        TrainCategory trainCategory = TrainCategory.getFor(trainCategoryRaw);
        return new BasicSchedule(transactionType, uniqueTrainId, startDate, endDate, daysOfWeek,
                ShortTermPlanIndicator.getFor(stpIndicatorRaw), headcode,
                TrainStatus.getFor(trainStatusRaw), trainCategory);
    }

    private static EnumSet<DayOfWeek> extractDays(CharSequence line, int begin) {
//        String days = RecordHelper.extract(line, begin, end);
//        if (days.length()!=7) {
//            logger.error("Not enough days of the week");
//        }

        Set<DayOfWeek> result = new HashSet<>();
        for (int day = 0; day < 7; day++) {
            if (line.charAt(day+begin) == '1') {
                result.add(DayOfWeek.of(day+1));
            }
        }
        return EnumSet.copyOf(result);
    }

    public RailRecordTransactionType getTransactionType() {
        return transactionType;
    }

    public String getUniqueTrainId() {
        return uniqueTrainId;
    }
    
    public DateRange getDateRange() {
        return new DateRange(startDate, endDate);
    }

    public EnumSet<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public ShortTermPlanIndicator getSTPIndicator() {
        return stpIndicator;
    }

    public String getTrainIdentity() {
        return trainIdentity;
    }

    public TrainStatus getTrainStatus() {
        return trainStatus;
    }

    public TrainCategory getTrainCategory() {
        return trainCategory;
    }

    @Override
    public String toString() {
        return "BasicSchedule{" +
                "uniqueTrainId='" + uniqueTrainId + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", transactionType=" + transactionType +
                ", daysOfWeek=" + daysOfWeek +
                ", stpIndicator=" + stpIndicator +
                ", trainIdentity='" + trainIdentity + '\'' +
                ", trainStatus=" + trainStatus +
                ", trainCategory=" + trainCategory +
                '}';
    }
}
