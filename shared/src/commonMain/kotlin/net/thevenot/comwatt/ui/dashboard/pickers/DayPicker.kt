package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPicker(
    currentDateTime: LocalDateTime,
    defaultSelectedDay: Int,
    onDateSelected: (Int) -> Unit
) {
    val currentDate = currentDateTime.date
    val initialMillis = currentDate
        .minus(defaultSelectedDay, DateTimeUnit.DAY)
        .toDatePickerMillis()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                isSelectableDatePickerMillis(utcTimeMillis, currentDate)
        }
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { timeMillis ->
            val selectedDate = datePickerMillisToLocalDate(timeMillis)

            onDateSelected(dayOffsetBetween(selectedDate, currentDate))
        }
    }

    Column {
        DatePicker(
            state = datePickerState,
        )
    }
}
