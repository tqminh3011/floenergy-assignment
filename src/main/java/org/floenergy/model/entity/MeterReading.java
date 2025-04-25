package org.floenergy.model.entity;

import java.sql.Timestamp;

public record MeterReading(
        String nmi,
        Timestamp timestamp,
        double consumption
){}
