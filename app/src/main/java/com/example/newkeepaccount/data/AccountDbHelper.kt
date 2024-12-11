package com.example.newkeepaccount.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MonthlyStats(
    val yearMonth: YearMonth,
    val income: Double,
    val expense: Double,
    val balance: Double
)

class AccountDbHelper(context: Context) : 
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "account.db"
        private const val DATABASE_VERSION = 2

        private const val SQL_CREATE_ENTRIES = """
            CREATE TABLE account_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 1,
                item TEXT NOT NULL,
                type TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE account_records ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")
        }
    }

    fun insertRecord(record: AccountRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("amount", record.amount)
            put("quantity", record.quantity)
            put("item", record.item)
            put("type", record.type.name)
            put("timestamp", record.timestamp)
        }
        return db.insert("account_records", null, values)
    }

    fun updateRecord(record: AccountRecord): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("amount", record.amount)
            put("quantity", record.quantity)
            put("item", record.item)
            put("type", record.type.name)
            put("timestamp", record.timestamp)
        }
        return db.update(
            "account_records",
            values,
            "id = ?",
            arrayOf(record.id.toString())
        )
    }

    fun deleteRecord(id: Long): Int {
        val db = writableDatabase
        return db.delete(
            "account_records",
            "id = ?",
            arrayOf(id.toString())
        )
    }

    fun getAllRecords(): List<AccountRecord> {
        val records = mutableListOf<AccountRecord>()
        val db = readableDatabase
        val cursor = db.query(
            "account_records",
            null,
            null,
            null,
            null,
            null,
            "timestamp DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val record = AccountRecord(
                    id = getLong(getColumnIndexOrThrow("id")),
                    amount = getDouble(getColumnIndexOrThrow("amount")),
                    quantity = getInt(getColumnIndexOrThrow("quantity")),
                    item = getString(getColumnIndexOrThrow("item")),
                    type = TransactionType.valueOf(getString(getColumnIndexOrThrow("type"))),
                    timestamp = getLong(getColumnIndexOrThrow("timestamp"))
                )
                records.add(record)
            }
        }
        cursor.close()
        return records
    }

    // 获取总体统计
    fun getTotalStats(): Triple<Double, Double, Double> {
        val db = readableDatabase
        var totalIncome = 0.0
        var totalExpense = 0.0

        // 获取总收入
        db.query(
            "account_records",
            arrayOf("SUM(amount * quantity) as total"),
            "type = ?",
            arrayOf(TransactionType.INCOME.name),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                totalIncome = cursor.getDouble(0)
            }
        }

        // 获取总支出
        db.query(
            "account_records",
            arrayOf("SUM(amount * quantity) as total"),
            "type = ?",
            arrayOf(TransactionType.EXPENSE.name),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                totalExpense = cursor.getDouble(0)
            }
        }

        return Triple(totalIncome, totalExpense, totalIncome - totalExpense)
    }

    // 获取所有月份的统计
    fun getAllMonthlyStats(): List<MonthlyStats> {
        val stats = mutableListOf<MonthlyStats>()
        val monthsSet = mutableSetOf<YearMonth>()
        
        // 获取所有记录的月份
        val db = readableDatabase
        db.query(
            "account_records",
            arrayOf("timestamp"),
            null, null, null, null,
            "timestamp ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val timestamp = cursor.getLong(0)
                val date = LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
                )
                monthsSet.add(YearMonth.from(date))
            }
        }

        // 对每个月份进行统计
        monthsSet.sortedDescending().forEach { yearMonth ->
            val monthStart = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val monthEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            var monthlyIncome = 0.0
            var monthlyExpense = 0.0

            // 获取月收入
            db.query(
                "account_records",
                arrayOf("SUM(amount * quantity) as total"),
                "type = ? AND timestamp >= ? AND timestamp < ?",
                arrayOf(TransactionType.INCOME.name, monthStart.toString(), monthEnd.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    monthlyIncome = cursor.getDouble(0)
                }
            }

            // 获取月支出
            db.query(
                "account_records",
                arrayOf("SUM(amount * quantity) as total"),
                "type = ? AND timestamp >= ? AND timestamp < ?",
                arrayOf(TransactionType.EXPENSE.name, monthStart.toString(), monthEnd.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    monthlyExpense = cursor.getDouble(0)
                }
            }

            stats.add(MonthlyStats(
                yearMonth = yearMonth,
                income = monthlyIncome,
                expense = monthlyExpense,
                balance = monthlyIncome - monthlyExpense
            ))
        }

        return stats
    }
} 