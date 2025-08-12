package net.thevenot.comwatt.utils

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DateFormatterTest {

    @Test
    fun testGetDayName_allDaysOfWeek() {
        val allDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        allDays.forEach { dayOfWeek ->
            val dayName = DateFormatter.getDayName(dayOfWeek)

            // Assert that we get a non-empty string
            assertNotNull(dayName, "Day name should not be null for $dayOfWeek")
            assertTrue(dayName.isNotBlank(), "Day name should not be blank for $dayOfWeek")

            // Assert minimum length (should be at least 3 characters for any language)
            assertTrue(
                dayName.length >= 3,
                "Day name should be at least 3 characters for $dayOfWeek, got: '$dayName'"
            )
        }
    }

    @Test
    fun testGetShortDayName_allDaysOfWeek() {
        val allDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        allDays.forEach { dayOfWeek ->
            val shortDayName = DateFormatter.getShortDayName(dayOfWeek)

            // Assert that we get a non-empty string
            assertNotNull(shortDayName, "Short day name should not be null for $dayOfWeek")
            assertTrue(
                shortDayName.isNotBlank(),
                "Short day name should not be blank for $dayOfWeek"
            )

            // Assert reasonable length (1-4 characters for most languages)
            assertTrue(
                shortDayName.length in 1..4,
                "Short day name should be 1-4 characters for $dayOfWeek, got: '$shortDayName'"
            )
        }
    }

    @Test
    fun testShortDayName_isShorterThanOrEqualToFullName() {
        val allDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        allDays.forEach { dayOfWeek ->
            val fullName = DateFormatter.getDayName(dayOfWeek)
            val shortName = DateFormatter.getShortDayName(dayOfWeek)

            assertTrue(
                shortName.length <= fullName.length,
                "Short name should be shorter than or equal to full name for $dayOfWeek. " +
                        "Full: '$fullName' (${fullName.length}), Short: '$shortName' (${shortName.length})"
            )
        }
    }

    @Test
    fun testDayNames_areConsistent() {
        // Test that the same day always returns the same name
        val testDay = DayOfWeek.MONDAY

        val name1 = DateFormatter.getDayName(testDay)
        val name2 = DateFormatter.getDayName(testDay)
        val shortName1 = DateFormatter.getShortDayName(testDay)
        val shortName2 = DateFormatter.getShortDayName(testDay)

        assertEquals(name1, name2, "Full day name should be consistent across calls")
        assertEquals(shortName1, shortName2, "Short day name should be consistent across calls")
    }

    @Test
    fun testAllDayNames_areUnique() {
        val allDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        // Test full day names are unique
        val fullNames = allDays.map { DateFormatter.getDayName(it) }
        val uniqueFullNames = fullNames.toSet()
        assertEquals(
            allDays.size,
            uniqueFullNames.size,
            "All full day names should be unique. Got: $fullNames"
        )

        // Test short day names are unique
        val shortNames = allDays.map { DateFormatter.getShortDayName(it) }
        val uniqueShortNames = shortNames.toSet()
        assertEquals(
            allDays.size,
            uniqueShortNames.size,
            "All short day names should be unique. Got: $shortNames"
        )
    }

    @Test
    fun testSpecificDaysForExpectedContent() {
        // We can't test exact strings due to localization, but we can test
        // that certain days don't get mixed up (like Monday vs Sunday)
        val mondayFull = DateFormatter.getDayName(DayOfWeek.MONDAY)
        val sundayFull = DateFormatter.getDayName(DayOfWeek.SUNDAY)
        val mondayShort = DateFormatter.getShortDayName(DayOfWeek.MONDAY)
        val sundayShort = DateFormatter.getShortDayName(DayOfWeek.SUNDAY)

        // These should be different
        assertTrue(mondayFull != sundayFull, "Monday and Sunday full names should be different")
        assertTrue(mondayShort != sundayShort, "Monday and Sunday short names should be different")
    }
}
