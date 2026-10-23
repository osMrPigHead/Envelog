package cn.osmrpighead.envelog.ui.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth

class EnvelogViewModel : ViewModel() {
    private val mutableYearMonth = MutableStateFlow(YearMonth.now())
    private val mutableToday = MutableStateFlow(LocalDate.now())

    val yearMonth: StateFlow<YearMonth> = mutableYearMonth.asStateFlow()
    val today: StateFlow<LocalDate> = mutableToday.asStateFlow()
}