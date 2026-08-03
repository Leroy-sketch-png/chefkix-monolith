package com.chefkix.culinary.features.challenge.model;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

public final class ChallengeWeekKey {

    private ChallengeWeekKey() {
    }

    public static String from(LocalDate date) {
        int weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("WEEKLY-%d-W%02d", weekBasedYear, week);
    }
}
