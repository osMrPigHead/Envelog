package cn.osmrpighead.envelog.ui.model

import cn.osmrpighead.envelog.data.time.TodayProvider
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import javax.inject.Inject

@ActivityRetainedScoped
class AppStateHolder @Inject constructor(
    todayProvider: TodayProvider,
    private val clock: Clock,
) {
    private val mutableYearMonth = MutableStateFlow(YearMonth.now(clock))
    private var isFollowingCurrentMonth = true

    val yearMonth: StateFlow<YearMonth> = mutableYearMonth.asStateFlow()
    val today: StateFlow<LocalDate> = todayProvider.today

    fun selectMonth(yearMonth: YearMonth) {
        isFollowingCurrentMonth = yearMonth == YearMonth.now(clock)
        mutableYearMonth.value = yearMonth
    }

    fun updateMonthIfFollowing() {
        if (isFollowingCurrentMonth) mutableYearMonth.value = YearMonth.now(clock)
    }
}