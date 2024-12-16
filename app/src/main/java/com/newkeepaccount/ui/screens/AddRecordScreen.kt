package com.keepaccount.newkeepaccount.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keepaccount.newkeepaccount.data.AccountRecord
import com.keepaccount.newkeepaccount.data.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    record: AccountRecord? = null,
    onSave: (AccountRecord) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf(record?.amount?.toString() ?: "") }
    var quantity by remember { mutableStateOf(record?.quantity?.toString() ?: "1") }
    var item by remember { mutableStateOf(record?.item ?: "") }
    var selectedType by remember { mutableStateOf(record?.type ?: TransactionType.EXPENSE) }
    var selectedDate by remember { mutableStateOf(record?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = AccountRecord.fromDate(selectedDate)
    )

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 标题
        Text(
            text = if (record == null) "添加记录" else "编辑记录",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 收支类型选择
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = {
                            Text(if (type == TransactionType.EXPENSE) "支出" else "收入")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 日期选择
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedDate.format(dateFormatter))
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "选择日期"
                )
            }
        }

        // 金额和数量输入
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "金额",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amount = newValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.weight(0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "数量",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || (newValue.matches(Regex("^[1-9]\\d*$")) && newValue.length <= 3)) {
                                quantity = newValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        // 事项输入
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "事项",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    placeholder = { Text("默认为杂费") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        // 显示总金额和按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 总金额显示
            val totalAmount = (amount.toDoubleOrNull() ?: 0.0) * (quantity.toIntOrNull() ?: 1)
            if (totalAmount > 0) {
                Text(
                    text = "总额: ¥${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 保存按钮
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@Button
                    val quantityValue = quantity.toIntOrNull() ?: 1
                    val itemValue = if (item.isBlank()) "杂费" else item.trim()
                    
                    val updatedRecord = AccountRecord(
                        id = record?.id ?: 0,
                        amount = amountValue,
                        quantity = quantityValue,
                        item = itemValue,
                        type = selectedType,
                        timestamp = AccountRecord.fromDate(selectedDate)
                    )
                    onSave(updatedRecord)
                },
                enabled = amount.isNotBlank() && amount.toDoubleOrNull() != null,
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (record == null) "添加" else "保存",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(millis),
                                ZoneId.systemDefault()
                            ).toLocalDate()
                            selectedDate = newDate
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
} 