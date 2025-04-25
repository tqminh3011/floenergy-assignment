package org.floenergy;

import org.floenergy.model.entity.MeterReading;

import java.sql.Timestamp;
import java.time.LocalDate;

public class MeterReadingFactory {
    public static MeterReading createMeterReading(
            String currentNmi,
            int intervalLengthInMins,
            int index,
            LocalDate startDate, double consumption) {

        int intervalTime = intervalLengthInMins * index;
        Timestamp intervalTimeStamp = Timestamp.valueOf(startDate.atStartOfDay().plusMinutes(intervalTime));

        return new MeterReading(currentNmi, intervalTimeStamp, consumption);
    }
}
