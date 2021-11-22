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

public class BasicSchedule implements RailTimetableRecord {

    public enum TransactionType {
        N, // new
        D, // delete
        R // Revise
    }

    private final TransactionType transactionType;

    public BasicSchedule(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public static BasicSchedule parse(String line) {
        String transactionTypeRaw = RecordHelper.extract(line, 3, 3);
        TransactionType transactionType = TransactionType.valueOf(transactionTypeRaw);
        return new BasicSchedule(transactionType);
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }




}
