package com.keepaccount.newkeepaccount.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class DateRange(val label: String) {
    ALL("全部"),
    THIS_MONTH("本月"),
    LAST_MONTH("上月"),
    THIS_YEAR("今年");

    fun getDateRange(): Pair<Long, Long> {
        val now = LocalDate.now()
        val (start, end) = when (this) {
            ALL -> Pair(
                LocalDate.of(2000, 1, 1),
                now.plusDays(1)
            )
            THIS_MONTH -> Pair(
                YearMonth.now().atDay(1),
                YearMonth.now().plusMonths(1).atDay(1)
            )
            LAST_MONTH -> Pair(
                YearMonth.now().minusMonths(1).atDay(1),
                YearMonth.now().atDay(1)
            )
            THIS_YEAR -> Pair(
                now.withDayOfYear(1),
                now.withDayOfYear(1).plusYears(1)
            )
        }
        return Pair(
            start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            end.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }
}

enum class TypeFilter(val label: String) {
    ALL("全部"),
    INCOME_ONLY("仅收入"),
    EXPENSE_ONLY("仅支出")
}

enum class SortOption(val label: String) {
    DATE_DESC("日期降序"),
    DATE_ASC("日期升序"),
    AMOUNT_DESC("金额降序"),
    AMOUNT_ASC("金额升序")
} 