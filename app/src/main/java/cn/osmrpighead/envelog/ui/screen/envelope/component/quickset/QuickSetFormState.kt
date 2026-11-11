package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.countDaysTo
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlanner
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetSchedule
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

internal sealed interface QuickSetValidation {
    data object IntervalInvalid : QuickSetValidation
    data object AmountNonPositive : QuickSetValidation
    data object CannotGenerate : QuickSetValidation
}

internal sealed interface QuickSetPreview {
    data class Daily(
        val intervalDays: Int,
        val amount: Long,
        val days: Long,
        val totalAmount: Long,
    ) : QuickSetPreview

    data class Weekly(
        val startDayOfWeek: DayOfWeek,
        val endDayOfWeek: DayOfWeek,
        val amount: Long,
        val days: Long,
        val totalAmount: Long,
    ) : QuickSetPreview

    data class MonthlyTotal(
        val totalAmount: Long,
        val days: Long,
    ) : QuickSetPreview
}

internal class QuickSetFormState(
    val yearMonth: YearMonth,
    initialTargetSpan: ClosedDateSpan = yearMonth.toHalfOpenDateSpan().toClosed(),
) {
    var mode by mutableStateOf(QuickSetMode.Daily)
        private set

    var intervalFieldValue by mutableStateOf(TextFieldValue("1", TextRange(1)))
        private set

    var dailyAmount by mutableLongStateOf(0L)
        private set

    var weeklyStartDayOfWeek by mutableStateOf(DayOfWeek.MONDAY)
        private set

    var weeklyEndDayOfWeek by mutableStateOf(DayOfWeek.SUNDAY)
        private set

    var weeklyAmount by mutableLongStateOf(0L)
        private set

    var monthlyAmount by mutableLongStateOf(0L)
        private set

    var targetSpan by mutableStateOf(initialTargetSpan)
        private set

    val intervalDays: Int
        get() = intervalFieldValue.text.toIntOrNull() ?: 0

    val maxIntervalDays: Int
        get() = (targetSpan.startDate countDaysTo targetSpan.endDate).toInt() + 1

    val dailyAmountConsumer = AmountConsumer(
        scale = 0,
        roundingMode = RoundingMode.HALF_UP,
        submit = { dailyAmount = it },
        cancel = {},
    )

    val weeklyAmountConsumer = AmountConsumer(
        scale = 0,
        roundingMode = RoundingMode.HALF_UP,
        submit = { weeklyAmount = it },
        cancel = {},
    )

    val monthlyAmountConsumer = AmountConsumer(
        scale = 0,
        roundingMode = RoundingMode.HALF_UP,
        submit = { monthlyAmount = it },
        cancel = {},
    )

    val dailyAmountFieldId = Any()
    val weeklyAmountFieldId = Any()
    val monthlyAmountFieldId = Any()

    val selectedAmountConsumer: AmountConsumer
        get() = when (mode) {
            QuickSetMode.Daily -> dailyAmountConsumer
            QuickSetMode.Weekly -> weeklyAmountConsumer
            QuickSetMode.MonthlyTotal -> monthlyAmountConsumer
        }

    val config: QuickSetConfig
        get() = when (mode) {
            QuickSetMode.Daily -> QuickSetConfig.Daily(intervalDays, dailyAmount)
            QuickSetMode.Weekly -> QuickSetConfig.Weekly(
                weeklyStartDayOfWeek,
                weeklyEndDayOfWeek,
                weeklyAmount,
            )

            QuickSetMode.MonthlyTotal -> QuickSetConfig.MonthlyTotal(monthlyAmount)
        }

    private val scheduleState = derivedStateOf {
        QuickSetBudgetPlanner.schedule(config, targetSpan, yearMonth)
    }

    val schedule: QuickSetSchedule? get() = scheduleState.value

    val intervalValid: Boolean
        get() = mode != QuickSetMode.Daily || intervalDays in 1..maxIntervalDays

    val amountValid: Boolean
        get() = selectedAmountConsumer.validationError == null && config.amount > 0

    val canApply: Boolean
        get() = intervalValid && amountValid && schedule != null

    val validation: QuickSetValidation?
        get() = when {
            mode == QuickSetMode.Daily && !intervalValid ->
                QuickSetValidation.IntervalInvalid

            selectedAmountConsumer.validationError is AmountValidationError.NonPositive ->
                QuickSetValidation.AmountNonPositive

            config.amount > 0 && schedule == null ->
                QuickSetValidation.CannotGenerate

            else -> null
        }

    val preview: QuickSetPreview?
        get() {
            val schedule = schedule ?: return null
            if (config.amount <= 0) return null
            val days = schedule.segments.sumOf { it.span.length }
            return when (mode) {
                QuickSetMode.Daily -> QuickSetPreview.Daily(
                    intervalDays = intervalDays,
                    amount = config.amount,
                    days = days,
                    totalAmount = schedule.totalAmount,
                )

                QuickSetMode.Weekly -> QuickSetPreview.Weekly(
                    startDayOfWeek = weeklyStartDayOfWeek,
                    endDayOfWeek = weeklyEndDayOfWeek,
                    amount = config.amount,
                    days = days,
                    totalAmount = schedule.totalAmount,
                )

                QuickSetMode.MonthlyTotal -> QuickSetPreview.MonthlyTotal(
                    totalAmount = schedule.totalAmount,
                    days = days,
                )
            }
        }

    fun selectMode(newMode: QuickSetMode) {
        mode = newMode
    }

    fun updateIntervalField(value: TextFieldValue) {
        intervalFieldValue = value
    }

    fun updateWeeklyRange(startDayOfWeek: DayOfWeek, endDayOfWeek: DayOfWeek) {
        weeklyStartDayOfWeek = startDayOfWeek
        weeklyEndDayOfWeek = endDayOfWeek
    }

    fun updateTargetSpan(span: ClosedDateSpan) {
        targetSpan = span
        if (intervalDays > maxIntervalDays) {
            val text = maxIntervalDays.toString()
            intervalFieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }

    companion object {
        val saver: Saver<QuickSetFormState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.yearMonth.year,
                    state.yearMonth.monthValue,
                    state.mode.name,
                    state.intervalFieldValue.text,
                    state.intervalFieldValue.selection.start,
                    state.intervalFieldValue.selection.end,
                    state.dailyAmount,
                    state.weeklyStartDayOfWeek.name,
                    state.weeklyEndDayOfWeek.name,
                    state.weeklyAmount,
                    state.monthlyAmount,
                    state.targetSpan.startDate.toEpochDay(),
                    state.targetSpan.endDate.toEpochDay(),
                )
            },
            restore = { values ->
                QuickSetFormState(
                    yearMonth = YearMonth.of(values[0] as Int, values[1] as Int),
                    initialTargetSpan = ClosedDateSpan(
                        startDate = LocalDate.ofEpochDay(values[11] as Long),
                        endDate = LocalDate.ofEpochDay(values[12] as Long),
                    ),
                ).apply {
                    mode = QuickSetMode.valueOf(values[2] as String)
                    val text = values[3] as String
                    val start = (values[4] as Int).coerceIn(0, text.length)
                    val end = (values[5] as Int).coerceIn(0, text.length)
                    intervalFieldValue = TextFieldValue(text, TextRange(start, end))
                    dailyAmount = values[6] as Long
                    weeklyStartDayOfWeek = DayOfWeek.valueOf(values[7] as String)
                    weeklyEndDayOfWeek = DayOfWeek.valueOf(values[8] as String)
                    weeklyAmount = values[9] as Long
                    monthlyAmount = values[10] as Long
                }
            },
        )
    }
}
