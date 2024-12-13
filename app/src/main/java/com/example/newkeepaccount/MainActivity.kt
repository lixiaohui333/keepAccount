package com.keepaccount.newkeepaccount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepaccount.newkeepaccount.data.*
import com.keepaccount.newkeepaccount.ui.screens.AddRecordScreen
import com.keepaccount.newkeepaccount.ui.theme.NewKeepAccountTheme
import com.keepaccount.newkeepaccount.utils.ShareUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun MonthlyStatsDialog(
    stats: List<MonthlyStats>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "月度统计",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = stats,
                    key = { it.yearMonth.toString() }
                ) { monthStats ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "${monthStats.yearMonth.year}年${monthStats.yearMonth.monthValue}月",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "收入: ¥${String.format("%.2f", monthStats.income)}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "支出: ¥${String.format("%.2f", monthStats.expense)}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = "结余: ¥${String.format("%.2f", monthStats.balance)}",
                                color = if (monthStats.balance >= 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun FilterSortBar(
    selectedDateRange: DateRange,
    selectedTypeFilter: TypeFilter,
    selectedSortOption: SortOption,
    onDateRangeSelected: (DateRange) -> Unit,
    onTypeFilterSelected: (TypeFilter) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 日期范围选择
        var showDateRangeMenu by remember { mutableStateOf(false) }
        Box {
            TextButton(
                onClick = { showDateRangeMenu = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(selectedDateRange.label)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "选择日期范围"
                )
            }
            DropdownMenu(
                expanded = showDateRangeMenu,
                onDismissRequest = { showDateRangeMenu = false }
            ) {
                DateRange.values().forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.label) },
                        onClick = {
                            onDateRangeSelected(range)
                            showDateRangeMenu = false
                        }
                    )
                }
            }
        }

        // 类型筛选
        var showTypeMenu by remember { mutableStateOf(false) }
        Box {
            TextButton(
                onClick = { showTypeMenu = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(selectedTypeFilter.label)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "选择类型筛选"
                )
            }
            DropdownMenu(
                expanded = showTypeMenu,
                onDismissRequest = { showTypeMenu = false }
            ) {
                TypeFilter.values().forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.label) },
                        onClick = {
                            onTypeFilterSelected(filter)
                            showTypeMenu = false
                        }
                    )
                }
            }
        }

        // 排序选项
        var showSortMenu by remember { mutableStateOf(false) }
        Box {
            TextButton(
                onClick = { showSortMenu = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(selectedSortOption.label)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "选择排序方式"
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSortOptionSelected(option)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var dbHelper: AccountDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = AccountDbHelper(this)
        
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var records by remember { mutableStateOf(listOf<AccountRecord>()) }
            var showAddForm by remember { mutableStateOf(false) }
            var selectedRecord by remember { mutableStateOf<AccountRecord?>(null) }
            var showDeleteDialog by remember { mutableStateOf(false) }
            var totalStats by remember { mutableStateOf(Triple(0.0, 0.0, 0.0)) }
            var monthlyStats by remember { mutableStateOf(emptyList<MonthlyStats>()) }
            var showMonthlyStats by remember { mutableStateOf(false) }
            
            var selectedDateRange by remember { mutableStateOf(DateRange.THIS_MONTH) }
            var selectedTypeFilter by remember { mutableStateOf(TypeFilter.ALL) }
            var selectedSortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
            
            // 加载数据和统计信息
            LaunchedEffect(selectedDateRange, selectedTypeFilter, selectedSortOption) {
                records = dbHelper.getFilteredAndSortedRecords(
                    selectedDateRange,
                    selectedTypeFilter,
                    selectedSortOption
                )
                totalStats = dbHelper.getFilteredStats(selectedDateRange)
                monthlyStats = dbHelper.getAllMonthlyStats()
            }
            
            NewKeepAccountTheme(
                onShareClick = {
                    scope.launch {
                        val imageUri = ShareUtils.generateBillImage(
                            context,
                            records,
                            totalStats.first,
                            totalStats.second
                        )
                        imageUri?.let { uri ->
                            ShareUtils.shareBillImage(context, uri)
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 筛选和排序栏
                        FilterSortBar(
                            selectedDateRange = selectedDateRange,
                            selectedTypeFilter = selectedTypeFilter,
                            selectedSortOption = selectedSortOption,
                            onDateRangeSelected = { selectedDateRange = it },
                            onTypeFilterSelected = { selectedTypeFilter = it },
                            onSortOptionSelected = { selectedSortOption = it }
                        )

                        // 总体统计卡片
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable { showMonthlyStats = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedDateRange.label + "统计",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "点击查看月度统计 >",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("收入")
                                        Text(
                                            "¥${String.format("%.2f", totalStats.first)}",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column {
                                        Text("支出")
                                        Text(
                                            "¥${String.format("%.2f", totalStats.second)}",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Column {
                                        Text("结余")
                                        Text(
                                            "¥${String.format("%.2f", totalStats.third)}",
                                            color = if (totalStats.third >= 0) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        // 记录列表
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(
                                items = records,
                                key = { it.id }
                            ) { record ->
                                RecordItem(
                                    record = record,
                                    onEdit = {
                                        selectedRecord = record
                                        showAddForm = true
                                    },
                                    onDelete = {
                                        selectedRecord = record
                                        showDeleteDialog = true
                                    }
                                )
                                Divider()
                            }
                        }

                        // 添加/编辑记录表单
                        AnimatedVisibility(
                            visible = showAddForm,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box {
                                    IconButton(
                                        onClick = { 
                                            showAddForm = false
                                            selectedRecord = null
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "关闭"
                                        )
                                    }
                                    
                                    AddRecordScreen(
                                        record = selectedRecord,
                                        onSave = { record ->
                                            if (selectedRecord != null) {
                                                dbHelper.updateRecord(record)
                                            } else {
                                                dbHelper.insertRecord(record)
                                            }
                                            records = dbHelper.getAllRecords()
                                            totalStats = dbHelper.getTotalStats()
                                            monthlyStats = dbHelper.getAllMonthlyStats()
                                            showAddForm = false
                                            selectedRecord = null
                                        },
                                        onCancel = { 
                                            showAddForm = false
                                            selectedRecord = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 悬浮添加按钮
                    if (!showAddForm) {
                        FloatingActionButton(
                            onClick = { showAddForm = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加记录")
                        }
                    }

                    // 月度统计对话框
                    if (showMonthlyStats) {
                        MonthlyStatsDialog(
                            stats = monthlyStats,
                            onDismiss = { showMonthlyStats = false }
                        )
                    }

                    // 删除确认对话框
                    if (showDeleteDialog && selectedRecord != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("确认删除") },
                            text = { Text("确定要删除这条记录吗？") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        selectedRecord?.let { record ->
                                            dbHelper.deleteRecord(record.id)
                                            records = dbHelper.getAllRecords()
                                            totalStats = dbHelper.getTotalStats()
                                            monthlyStats = dbHelper.getAllMonthlyStats()
                                        }
                                        showDeleteDialog = false
                                        selectedRecord = null
                                    }
                                ) {
                                    Text("删除")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}

@Composable
fun RecordItem(
    record: AccountRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：事项和时间
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.item,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatDate(record.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 中间：金额和数量
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${if (record.type == TransactionType.EXPENSE) "-" else "+"}¥${String.format("%.2f", record.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (record.type == TransactionType.EXPENSE) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                )
                if (record.quantity > 1) {
                    Text(
                        text = "x${record.quantity} = ¥${String.format("%.2f", record.amount * record.quantity)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 右侧：操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormatter = DateTimeFormatter.ofPattern("MM月dd日")
    val date = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    ).toLocalDate()
    return date.format(dateFormatter)
}