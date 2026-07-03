package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LineHallEntry
import com.example.data.OutStationEntry
import com.example.data.PartnerAttendance

@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val lineHallLogs by viewModel.lineHallEntries.collectAsState()
    val outStationLogs by viewModel.outStationEntries.collectAsState()
    val attendanceLogs by viewModel.allPartnerAttendance.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Line Hall, 1: Out-Station, 2: Attendance
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    // CSV Exporters
    fun shareCSV(title: String, csvContent: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Report via:")
        context.startActivity(shareIntent)
    }

    fun exportLineHallToCSV(list: List<LineHallEntry>): String {
        val sb = StringBuilder()
        sb.append("ID,Route,Vehicle Number,Timestamp,Operator\n")
        list.forEach { entry ->
            sb.append("${entry.id},\"${entry.route}\",\"${entry.vehicleNumber}\",\"${viewModel.formatTimestamp(entry.timestamp)}\",\"${entry.operatorUser}\"\n")
        }
        return sb.toString()
    }

    fun exportOutStationToCSV(list: List<OutStationEntry>): String {
        val sb = StringBuilder()
        sb.append("ID,Route,Checkpoint Town,Type,Timestamp,Vehicle Number,Operator\n")
        list.forEach { entry ->
            val typeStr = if (entry.isEntry) "ENTRY" else "EXIT"
            sb.append("${entry.id},\"${entry.route}\",\"${entry.section}\",\"$typeStr\",\"${viewModel.formatTimestamp(entry.timestamp)}\",\"${entry.vehicleNumber}\",\"${entry.operatorUser}\"\n")
        }
        return sb.toString()
    }

    fun exportAttendanceToCSV(list: List<PartnerAttendance>): String {
        val sb = StringBuilder()
        sb.append("Date,Partner Code,Partner Name,Status,Timestamp,Operator\n")
        list.forEach { entry ->
            val statusStr = if (entry.isPresent) "PRESENT" else "ABSENT"
            val partnerName = viewModel.channelPartners.find { it.code == entry.partnerCode }?.name ?: "Unknown"
            sb.append("\"${entry.date}\",\"${entry.partnerCode}\",\"$partnerName\",\"$statusStr\",\"${viewModel.formatTimestamp(entry.timestamp)}\",\"${entry.operatorUser}\"\n")
        }
        return sb.toString()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("admin_dashboard_screen")
    ) {
        // Dashboard Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Central Admin Terminal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Access log records & dispatch exports",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Live stats overview row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Line Hall Count card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Morning Logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${lineHallLogs.size}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }

                // Out-Station Count card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Transit Logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${outStationLogs.size}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }

                // Attendance Log Count
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Partner Logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${attendanceLogs.size}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }
            }
        }

        // Search Bar and Export buttons
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search logs (by vehicle, code, section...)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("admin_search_bar")
            )
        }

        // Segment Tabs to select categories
        item {
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.testTag("admin_tab_linehall"),
                    text = { Text("Line Hall", fontSize = 12.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.testTag("admin_tab_outstation"),
                    text = { Text("Out-Station", fontSize = 12.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    modifier = Modifier.testTag("admin_tab_attendance"),
                    text = { Text("Attendance", fontSize = 12.sp, fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // CSV Export Trigger Button Card
        item {
            val reportTitle = when (activeTab) {
                0 -> "Line Hall Logistics"
                1 -> "Out-Station Transit Logs"
                else -> "Channel Partner Attendance"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Current Active Logs",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Convert $reportTitle logs to a standard CSV file.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = {
                            when (activeTab) {
                                0 -> {
                                    val csv = exportLineHallToCSV(lineHallLogs)
                                    shareCSV("Line_Hall_Log_Report.csv", csv)
                                }
                                1 -> {
                                    val csv = exportOutStationToCSV(outStationLogs)
                                    shareCSV("Out_Station_Log_Report.csv", csv)
                                }
                                2 -> {
                                    val csv = exportAttendanceToCSV(attendanceLogs)
                                    shareCSV("Partner_Attendance_Report.csv", csv)
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_export_csv"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 12.sp)
                    }
                }
            }
        }

        // Live Table Records Rendering
        if (activeTab == 0) {
            val filteredLineHall = lineHallLogs.filter {
                it.vehicleNumber.contains(searchQuery, ignoreCase = true) ||
                        it.operatorUser.contains(searchQuery, ignoreCase = true)
            }

            if (filteredLineHall.isEmpty()) {
                item { EmptyLogsView() }
            }

            items(filteredLineHall) { log ->
                LineHallEntryRow(log, viewModel)
            }
        } else if (activeTab == 1) {
            val filteredOutStation = outStationLogs.filter {
                it.vehicleNumber.contains(searchQuery, ignoreCase = true) ||
                        it.section.contains(searchQuery, ignoreCase = true) ||
                        it.operatorUser.contains(searchQuery, ignoreCase = true)
            }

            if (filteredOutStation.isEmpty()) {
                item { EmptyLogsView() }
            }

            items(filteredOutStation) { log ->
                OutStationLogEntryRow(log, viewModel)
            }
        } else {
            val filteredAttendance = attendanceLogs.filter {
                it.partnerCode.contains(searchQuery, ignoreCase = true) ||
                        it.date.contains(searchQuery, ignoreCase = true) ||
                        it.operatorUser.contains(searchQuery, ignoreCase = true)
            }

            if (filteredAttendance.isEmpty()) {
                item { EmptyLogsView() }
            }

            items(filteredAttendance) { log ->
                AdminAttendanceRow(log, viewModel)
            }
        }

        // Dangerous Actions Area
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reset Sandbox Environment",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Irreversibly delete all local fleet logs and attendance sheets.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("btn_clear_database"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Clear All")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Confirmation Alert
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Confirm Database Purge?") },
            text = { Text("This operation is permanent and cannot be undone. All fleet records, check-in timestamps, photo assets, and channel partner sheets will be wiped from local SQLite storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("dialog_confirm_clear")
                ) {
                    Text("PURGE ALL", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    modifier = Modifier.testTag("dialog_cancel_clear")
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun AdminAttendanceRow(
    log: PartnerAttendance,
    viewModel: AppViewModel
) {
    val partnerName = remember(log.partnerCode) {
        viewModel.channelPartners.find { it.code == log.partnerCode }?.name ?: "Unknown Partner"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("admin_attendance_row"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (log.isPresent) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentInd,
                        contentDescription = "Attendance indicator",
                        tint = if (log.isPresent) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = log.partnerCode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (log.isPresent) "PRESENT" else "ABSENT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (log.isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = partnerName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sheet: ${log.date} | Op: ${log.operatorUser}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(
                onClick = { viewModel.deletePartnerAttendance(log) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                modifier = Modifier.testTag("delete_admin_attendance_entry")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record"
                )
            }
        }
    }
}

@Composable
fun EmptyLogsView() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Text(
            text = "No matching log entries found for active filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}
