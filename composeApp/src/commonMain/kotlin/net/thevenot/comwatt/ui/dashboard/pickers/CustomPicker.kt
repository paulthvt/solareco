package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_date_time_picker_cancel_button
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_date_time_picker_confirm_button
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_date_time_picker_select_date_icon
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_date_time_picker_select_time_icon
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_end_cannot_be_in_future_error
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_end_title
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_start_cannot_be_in_future_error
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_start_must_be_before_end_error
import comwatt.composeapp.generated.resources.custom_range_dialog_picker_start_title
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.utils.formatHourMinutes
import net.thevenot.comwatt.utils.formatYearMonthDay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPicker(
    currentDateTime: LocalDateTime,
    defaultStartDateTime: Instant,
    defaultEndDateTime: Instant,
    onRangeSelected: (CustomSelectedRange) -> Unit
) {
    val timeZone = TimeZone.currentSystemDefault()
    var startDateTime by remember { mutableStateOf(defaultStartDateTime.toLocalDateTime(timeZone)) }
    var endDateTime by remember { mutableStateOf(defaultEndDateTime.toLocalDateTime(timeZone)) }

    var startDateTimeError by remember { mutableStateOf<String?>(null) }
    var endDateTimeError by remember { mutableStateOf<String?>(null) }

    val isStartInFuture by remember(startDateTime, currentDateTime) {
        mutableStateOf(isInFuture(startDateTime, currentDateTime))
    }
    val isEndInFuture by remember(endDateTime, currentDateTime) {
        mutableStateOf(isInFuture(endDateTime, currentDateTime))
    }
    val isRangeOrderInvalid by remember(startDateTime, endDateTime) {
        mutableStateOf(startDateTime > endDateTime)
    }

    val isRangeValid by remember(isStartInFuture, isEndInFuture, isRangeOrderInvalid) {
        mutableStateOf(!isStartInFuture && !isEndInFuture && !isRangeOrderInvalid)
    }
    val startInFutureErrorMessage =
        stringResource(Res.string.custom_range_dialog_picker_start_cannot_be_in_future_error)
    val endInFutureErrorMessage =
        stringResource(Res.string.custom_range_dialog_picker_end_cannot_be_in_future_error)

    LaunchedEffect(Unit) {
        validateDateTime(
            dateTime = startDateTime,
            currentDateTime = currentDateTime,
            errorMessage = startInFutureErrorMessage
        ) { startDateTimeError = it }
        validateDateTime(
            dateTime = endDateTime,
            currentDateTime = currentDateTime,
            errorMessage = endInFutureErrorMessage
        ) { endDateTimeError = it }

        onRangeSelected(
            CustomSelectedRange(
                start = startDateTime.toInstant(timeZone),
                end = endDateTime.toInstant(timeZone),
                isRangeValid = isRangeValid
            )
        )
    }

    LaunchedEffect(isRangeValid, startDateTime, endDateTime) {
        onRangeSelected(
            CustomSelectedRange(
                start = startDateTime.toInstant(timeZone),
                end = endDateTime.toInstant(timeZone),
                isRangeValid = isRangeValid
            )
        )
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
    ) {
        DateTimeRow(
            title = stringResource(Res.string.custom_range_dialog_picker_start_title),
            dateTime = startDateTime,
            errorMessage = startDateTimeError,
            onDateClick = { showStartDatePicker = true },
            onTimeClick = { showStartTimePicker = true }
        )

        DateTimeRow(
            title = stringResource(Res.string.custom_range_dialog_picker_end_title),
            dateTime = endDateTime,
            errorMessage = endDateTimeError,
            onDateClick = { showEndDatePicker = true },
            onTimeClick = { showEndTimePicker = true }
        )

        if (isRangeOrderInvalid && startDateTimeError == null && endDateTimeError == null) {
            Text(
                text = stringResource(Res.string.custom_range_dialog_picker_start_must_be_before_end_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = AppTheme.dimens.paddingSmall)
            )
        }
    }

    if (showStartDatePicker) {
        DatePickerDialogComponent(
            initialDate = startDateTime,
            currentDateTime = currentDateTime,
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { newDate ->
                val newDateTime = LocalDateTime(
                    date = newDate,
                    time = startDateTime.time
                )
                startDateTime = newDateTime

                validateDateTime(
                    dateTime = newDateTime,
                    currentDateTime = currentDateTime,
                    errorMessage = startInFutureErrorMessage
                ) { startDateTimeError = it }
                validateDateTime(
                    dateTime = endDateTime,
                    currentDateTime = currentDateTime,
                    errorMessage = endInFutureErrorMessage
                ) { endDateTimeError = it }
            }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialogComponent(
            initialDate = endDateTime,
            currentDateTime = currentDateTime,
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { newDate ->
                val newDateTime = LocalDateTime(
                    date = newDate,
                    time = endDateTime.time
                )
                endDateTime = newDateTime

                validateDateTime(
                    dateTime = newDateTime,
                    currentDateTime = currentDateTime,
                    errorMessage = endInFutureErrorMessage
                ) { endDateTimeError = it }
            }
        )
    }

    if (showStartTimePicker) {
        TimePickerDialogComponent(
            initialTime = startDateTime.time,
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = { newTime ->
                val newDateTime = LocalDateTime(
                    date = startDateTime.date,
                    time = newTime
                )
                startDateTime = newDateTime

                validateDateTime(
                    dateTime = newDateTime,
                    currentDateTime = currentDateTime,
                    errorMessage = startInFutureErrorMessage
                ) { startDateTimeError = it }
                validateDateTime(
                    dateTime = endDateTime,
                    currentDateTime = currentDateTime,
                    errorMessage = endInFutureErrorMessage
                ) { endDateTimeError = it }
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialogComponent(
            initialTime = endDateTime.time,
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = { newTime ->
                val newDateTime = LocalDateTime(
                    date = endDateTime.date,
                    time = newTime
                )
                endDateTime = newDateTime

                validateDateTime(
                    dateTime = newDateTime,
                    currentDateTime = currentDateTime,
                    errorMessage = endInFutureErrorMessage
                ) { endDateTimeError = it }
            }
        )
    }
}

@Composable
private fun DateTimeRow(
    title: String,
    dateTime: LocalDateTime,
    errorMessage: String?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Card(
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimens.paddingNormal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = AppTheme.dimens.paddingSmall)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = AppTheme.dimens.paddingSmall)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall),
                        modifier = Modifier
                            .clickable { onDateClick() }
                            .padding(AppTheme.dimens.paddingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = stringResource(Res.string.custom_range_dialog_picker_date_time_picker_select_date_icon)
                        )
                        Text(
                            text = dateTime.date.formatYearMonthDay(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall),
                        modifier = Modifier
                            .clickable { onTimeClick() }
                            .padding(AppTheme.dimens.paddingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = stringResource(Res.string.custom_range_dialog_picker_date_time_picker_select_time_icon)
                        )
                        Text(
                            text = dateTime.time.formatHourMinutes(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = AppTheme.dimens.paddingSmall)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogComponent(
    initialDate: LocalDateTime,
    currentDateTime: LocalDateTime,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selectedDateTime = Instant.fromEpochMilliseconds(utcTimeMillis)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                return selectedDateTime <= currentDateTime
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDate =
                            instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        onDateSelected(localDate)
                    }
                    onDismissRequest()
                }
            ) {
                Text(stringResource(Res.string.custom_range_dialog_picker_date_time_picker_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.custom_range_dialog_picker_date_time_picker_cancel_button))
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogComponent(
    initialTime: LocalTime,
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val newTime = LocalTime(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                    onTimeSelected(newTime)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(Res.string.custom_range_dialog_picker_date_time_picker_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.custom_range_dialog_picker_date_time_picker_cancel_button))
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(
                    state = timePickerState,
                    layoutType = TimePickerLayoutType.Vertical
                )
            }
        }
    )
}

@Preview
@Composable
fun CustomPickerPreview() {
    val now = Clock.System.now()
    val endTime = now + 2.days

    ComwattTheme {
        CustomPicker(
            currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault()),
            defaultStartDateTime = now,
            defaultEndDateTime = endTime,
            onRangeSelected = { _ -> }
        )
    }
}

/**
 * Validates that a selected date/time is not in the future
 *
 * @param dateTime The date/time to validate
 * @param currentDateTime The current date/time to compare against
 * @param errorMessage The error message to return if validation fails
 * @param onResult Callback with error message if validation fails, null otherwise
 */
private fun validateDateTime(
    dateTime: LocalDateTime,
    currentDateTime: LocalDateTime,
    errorMessage: String,
    onResult: (String?) -> Unit
) {
    if (dateTime > currentDateTime) {
        onResult(errorMessage)
    } else {
        onResult(null)
    }
}

private fun isInFuture(dateTime: LocalDateTime, currentDateTime: LocalDateTime): Boolean {
    return dateTime > currentDateTime
}

data class CustomSelectedRange(
    val start: Instant,
    val end: Instant,
    val isRangeValid: Boolean
)