package net.thevenot.comwatt.model.type

import net.thevenot.comwatt.domain.model.TimeUnit

enum class TimeAgoUnit {
    HOUR, DAY, WEEK, MONTH, YEAR;

    companion object {
        fun fromTimeUnit(timeUnit: TimeUnit): TimeAgoUnit {
            return when (timeUnit) {
                TimeUnit.HOUR -> HOUR
                TimeUnit.DAY -> DAY
                TimeUnit.WEEK -> WEEK
                TimeUnit.MONTH -> MONTH
                TimeUnit.YEAR -> YEAR
            }
        }
    }
}