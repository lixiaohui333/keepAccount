package com.example.newkeepaccount.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class TransactionType {
    EXPENSE, INCOME
}

data class AccountRecord(
    var id: Long = 0,
    var amount: Double = 0.0,
    var quantity: Int = 1,
    var item: String = "",
    var type: TransactionType = TransactionType.EXPENSE,
    var timestamp: Long = System.currentTimeMillis()
) {
    val date: LocalDate
        get() = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        ).toLocalDate()

    companion object {
        fun fromDate(date: LocalDate): Long {
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
} 