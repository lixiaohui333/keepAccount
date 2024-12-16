package com.keepaccount.newkeepaccount.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.keepaccount.newkeepaccount.data.AccountRecord
import com.keepaccount.newkeepaccount.data.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ShareUtils {
    private val COLOR_PRIMARY = Color.parseColor("#1A237E")  // 主色
    private val COLOR_INCOME = Color.parseColor("#4CAF50")   // 收入颜色
    private val COLOR_EXPENSE = Color.parseColor("#F44336")  // 支出颜色
    private val COLOR_BACKGROUND = Color.parseColor("#E8EAF6") // 背景色
    private val COLOR_CARD = Color.WHITE                     // 卡片背景色
    private const val MAX_RECORDS = 15                       // 最大记录数
    private const val CARD_HEIGHT = 100f                     // 记录卡片高度
    private const val CARD_PADDING = 12f                     // 卡片内边距

    fun generateBillImage(
        context: Context,
        records: List<AccountRecord>,
        totalIncome: Double,
        totalExpense: Double
    ): Uri? {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 48f
            color = COLOR_PRIMARY
        }

        // 设置背景色
        canvas.drawColor(COLOR_BACKGROUND)

        // 绘制顶部卡片背景
        val headerRect = RectF(24f, 24f, width - 24f, 480f)
        paint.apply {
            color = COLOR_CARD
            setShadowLayer(8f, 0f, 2f, Color.argb(40, 0, 0, 0))
        }
        canvas.drawRoundRect(headerRect, 24f, 24f, paint)
        paint.clearShadowLayer()

        // 绘制标题
        paint.apply {
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            color = COLOR_PRIMARY
        }
        canvas.drawText("简约记账", 60f, 100f, paint)
        
        // 绘制日期
        paint.apply {
            textSize = 36f
            typeface = Typeface.DEFAULT
            color = Color.GRAY
        }
        val dateFormat = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
        val currentDate = LocalDateTime.now().format(dateFormat)
        canvas.drawText("生成日期：$currentDate", 60f, 160f, paint)

        // 绘制总收支
        val statsRect = RectF(60f, 200f, width - 60f, 440f)
        paint.apply {
            color = COLOR_CARD
            setShadowLayer(6f, 0f, 2f, Color.argb(30, 0, 0, 0))
        }
        canvas.drawRoundRect(statsRect, 16f, 16f, paint)
        paint.clearShadowLayer()

        paint.apply {
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
        }

        // 绘制总收入
        paint.color = COLOR_INCOME
        val incomeText = "总收入"
        val incomeAmount = "¥%.2f".format(totalIncome)
        canvas.drawText(incomeText, 100f, 280f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(incomeAmount, width - 100f, 280f, paint)

        // 绘制总支出
        paint.color = COLOR_EXPENSE
        paint.textAlign = Paint.Align.LEFT
        val expenseText = "总支出"
        val expenseAmount = "¥%.2f".format(totalExpense)
        canvas.drawText(expenseText, 100f, 340f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(expenseAmount, width - 100f, 340f, paint)

        // 绘制结余
        val balance = totalIncome - totalExpense
        paint.color = if (balance >= 0) COLOR_INCOME else COLOR_EXPENSE
        paint.textAlign = Paint.Align.LEFT
        val balanceText = "结余"
        val balanceAmount = "¥%.2f".format(balance)
        canvas.drawText(balanceText, 100f, 400f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(balanceAmount, width - 100f, 400f, paint)

        paint.textAlign = Paint.Align.LEFT

        // 绘制记录列表
        var y = 520f
        records.take(MAX_RECORDS).forEach { record ->
            // 绘制记录卡片背景
            val cardTop = y
            val cardBottom = y + CARD_HEIGHT
            val recordRect = RectF(24f, cardTop, width - 24f, cardBottom)
            paint.apply {
                color = COLOR_CARD
                setShadowLayer(6f, 0f, 2f, Color.argb(30, 0, 0, 0))
            }
            canvas.drawRoundRect(recordRect, 16f, 16f, paint)
            paint.clearShadowLayer()

            val centerY = (cardTop + cardBottom) / 2

            // 绘制日期
            paint.apply {
                textSize = 34f
                color = Color.GRAY
                typeface = Typeface.DEFAULT
            }
            val date = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(record.timestamp),
                ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("MM-dd"))
            canvas.drawText(date, 60f, centerY + 12f, paint)

            // 绘制事项
            paint.apply {
                textSize = 38f
                color = COLOR_PRIMARY
            }
            canvas.drawText(record.item, 180f, centerY + 12f, paint)

            // 绘制金额
            paint.apply {
                textSize = 38f
                color = if (record.type == TransactionType.INCOME) COLOR_INCOME else COLOR_EXPENSE
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val amountText = if (record.type == TransactionType.INCOME) {
                "+¥%.2f".format(record.amount)
            } else {
                "-¥%.2f".format(record.amount)
            }
            canvas.drawText(amountText, width - 60f, centerY + 12f, paint)

            // 绘制数量
            if (record.quantity > 1) {
                paint.apply {
                    textSize = 32f
                    color = Color.GRAY
                    typeface = Typeface.DEFAULT
                }
                val quantityText = "x${record.quantity}"
                val amountWidth = paint.measureText(amountText)
                canvas.drawText(quantityText, width - 80f - amountWidth, centerY + 12f, paint)
            }

            paint.textAlign = Paint.Align.LEFT
            y += CARD_HEIGHT + CARD_PADDING
        }

        // 如果记录超过限制，添加统计信息
        if (records.size > MAX_RECORDS) {
            val remainingRecords = records.drop(MAX_RECORDS)
            val remainingIncome = remainingRecords.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val remainingExpense = remainingRecords.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            
            val summaryRect = RectF(24f, y, width - 24f, y + 140f)
            paint.apply {
                color = COLOR_CARD
                setShadowLayer(6f, 0f, 2f, Color.argb(30, 0, 0, 0))
            }
            canvas.drawRoundRect(summaryRect, 16f, 16f, paint)
            paint.clearShadowLayer()

            paint.apply {
                textSize = 34f
                color = Color.GRAY
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("还有 ${records.size - MAX_RECORDS} 条记录未显示", 60f, y + 45f, paint)

            paint.apply {
                textSize = 38f
                typeface = Typeface.DEFAULT_BOLD
            }
            paint.color = COLOR_INCOME
            canvas.drawText("收入 ¥%.2f".format(remainingIncome), 60f, y + 100f, paint)
            paint.color = COLOR_EXPENSE
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("支出 ¥%.2f".format(remainingExpense), width - 60f, y + 100f, paint)
        }

        // 保存图片到缓存
        return try {
            val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val imageFile = File(imagesDir, "bill_${System.currentTimeMillis()}.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }

    fun shareBillImage(context: Context, imageUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享账单"))
    }
} 