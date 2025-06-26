package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPicker(
    currentDateTime: LocalDateTime,
    defaultSelectedDay: Int,
    onDateSelected: (Int) -> Unit
) {
    val dayInMillis = 24 * 60 * 60 * 1000L
    val currentMillis =
        currentDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    val initialMillis = currentMillis - (defaultSelectedDay * dayInMillis)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selectedDateTime = Instant.fromEpochMilliseconds(utcTimeMillis)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                return selectedDateTime <= currentDateTime
            }
        }
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { timeMillis ->
            val selectedDate = Instant.fromEpochMilliseconds(timeMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val currentDate = currentDateTime.date
            val selectedDateOnly = selectedDate.date

            val daysDifference = currentDate.minus(selectedDateOnly).days

            onDateSelected(daysDifference)
        }
    }

    Column {
        DatePicker(
            state = datePickerState,
        )
    }
}