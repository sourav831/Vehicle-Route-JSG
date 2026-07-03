package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FleetRepository
import com.example.data.LineHallEntry
import com.example.data.OutStationEntry
import com.example.data.PartnerAttendance
import com.example.data.GeminiOcrService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = FleetRepository(database)

    // Current Active Screen state
    private val _currentScreen = MutableStateFlow("LOGIN")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Current Logged-in User state
    private val _loggedInUser = MutableStateFlow<String?>(null)
    val loggedInUser: StateFlow<String?> = _loggedInUser.asStateFlow()

    // Database flow bindings
    val lineHallEntries: StateFlow<List<LineHallEntry>> = repository.lineHallEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outStationEntries: StateFlow<List<OutStationEntry>> = repository.outStationEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPartnerAttendance: StateFlow<List<PartnerAttendance>> = repository.allPartnerAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Channel Partner metadata - 13 partners as requested
    val channelPartners = listOf(
        PartnerInfo("WF145", "Sagar Roadways, Bhubaneswar"),
        PartnerInfo("WF337", "Kalinga Logistics, Jharsuguda"),
        PartnerInfo("WF498", "Utkal Cargo Agency, Jharsuguda"),
        PartnerInfo("WF965", "Jagannath Traders, Brajrajnagar"),
        PartnerInfo("WF983", "Mahanadi Distributors, Belpahar"),
        PartnerInfo("WF1021", "Sundargarh Carrier, Sundargarh"),
        PartnerInfo("WF1051", "Bamra Express Freight, Bamra"),
        PartnerInfo("WF108", "Kuchinda Supply Chain, Kuchinda"),
        PartnerInfo("WF095", "Western Odisha Transports, Jharsuguda"),
        PartnerInfo("WF192", "Puri Roadlines, Bhubaneswar"),
        PartnerInfo("WF387", "Patnaik & Sons, Jharsuguda"),
        PartnerInfo("WF391", "Samal Agency, Bandhabahal"),
        PartnerInfo("WF399", "Biju Transport Co., Bhubaneswar") // 13th partner added
    )

    // Current selected attendance date (defaults to today)
    private val _selectedAttendanceDate = MutableStateFlow(getCurrentDateString())
    val selectedAttendanceDate: StateFlow<String> = _selectedAttendanceDate.asStateFlow()

    // Attendance records for the selected date
    private val _attendanceForSelectedDate = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val attendanceForSelectedDate: StateFlow<Map<String, Boolean>> = _attendanceForSelectedDate.asStateFlow()

    // OCR Operation State
    private val _ocrLoading = MutableStateFlow(false)
    val ocrLoading: StateFlow<Boolean> = _ocrLoading.asStateFlow()

    private val _ocrResultText = MutableStateFlow<String?>(null)
    val ocrResultText: StateFlow<String?> = _ocrResultText.asStateFlow()

    init {
        // Observe database attendance records to update our UI attendance grid when date changes
        viewModelScope.launch {
            selectedAttendanceDate.collect { date ->
                repository.getPartnerAttendanceByDate(date).collect { list ->
                    val map = channelPartners.associate { it.code to false }.toMutableMap()
                    list.forEach { record ->
                        map[record.partnerCode] = record.isPresent
                    }
                    _attendanceForSelectedDate.value = map
                }
            }
        }
    }

    // --- Authentication ---
    fun login(usernameOrEmail: String) {
        if (usernameOrEmail.isNotBlank()) {
            _loggedInUser.value = usernameOrEmail.trim()
            _currentScreen.value = "DASHBOARD"
        }
    }

    fun logout() {
        _loggedInUser.value = null
        _currentScreen.value = "LOGIN"
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    // --- Attendance Operations ---
    fun setAttendanceDate(date: String) {
        _selectedAttendanceDate.value = date
    }

    fun toggleAttendance(partnerCode: String) {
        val currentMap = _attendanceForSelectedDate.value.toMutableMap()
        val currentVal = currentMap[partnerCode] ?: false
        currentMap[partnerCode] = !currentVal
        _attendanceForSelectedDate.value = currentMap

        // Save immediately to DB
        viewModelScope.launch {
            val record = PartnerAttendance(
                date = _selectedAttendanceDate.value,
                partnerCode = partnerCode,
                isPresent = !currentVal,
                timestamp = System.currentTimeMillis(),
                operatorUser = _loggedInUser.value ?: "Guest"
            )
            repository.insertPartnerAttendance(record)
        }
    }

    fun markAllAttendance(isPresent: Boolean) {
        val date = _selectedAttendanceDate.value
        val user = _loggedInUser.value ?: "Guest"
        viewModelScope.launch {
            val list = channelPartners.map { partner ->
                PartnerAttendance(
                    date = date,
                    partnerCode = partner.code,
                    isPresent = isPresent,
                    timestamp = System.currentTimeMillis(),
                    operatorUser = user
                )
            }
            repository.insertAllPartnerAttendance(list)
        }
    }

    // --- Morning Line Hall OCR ---
    fun processLineHallCapture(bitmap: Bitmap, route: String) {
        viewModelScope.launch {
            _ocrLoading.value = true
            _ocrResultText.value = "Analyzing license plate via Gemini AI..."
            try {
                val ocrResult = GeminiOcrService.performOcr(bitmap)
                _ocrResultText.value = ocrResult

                // Convert bitmap to base64 to save in local DB
                val photoBase64 = bitmap.toBase64String()

                val entry = LineHallEntry(
                    route = route,
                    vehicleNumber = ocrResult,
                    timestamp = System.currentTimeMillis(),
                    photoUri = photoBase64,
                    operatorUser = _loggedInUser.value ?: "Staff"
                )
                repository.insertLineHallEntry(entry)
                Log.d("AppViewModel", "Successfully saved Line Hall entry.")
            } catch (e: Exception) {
                Log.e("AppViewModel", "OCR failed", e)
                _ocrResultText.value = "OCR failed: ${e.localizedMessage}"
            } finally {
                _ocrLoading.value = false
            }
        }
    }

    // --- Out-Station Module Operations ---
    fun saveOutStationEntry(
        bitmap: Bitmap,
        section: String,
        isEntry: Boolean,
        vehicleNo: String
    ) {
        viewModelScope.launch {
            _ocrLoading.value = true
            _ocrResultText.value = "Processing image..."
            try {
                // If they did not provide a vehicle number, let's OCR it!
                val finalVehicleNo = if (vehicleNo.trim().isBlank()) {
                    _ocrResultText.value = "Analyzing license plate via Gemini AI..."
                    GeminiOcrService.performOcr(bitmap)
                } else {
                    vehicleNo.trim()
                }

                _ocrResultText.value = finalVehicleNo

                // Convert bitmap to base64
                val photoBase64 = bitmap.toBase64String()

                val entry = OutStationEntry(
                    route = "Jharsuguda to Bhubaneswar",
                    section = section,
                    isEntry = isEntry,
                    timestamp = System.currentTimeMillis(),
                    photoUri = photoBase64,
                    vehicleNumber = finalVehicleNo,
                    operatorUser = _loggedInUser.value ?: "Staff"
                )
                repository.insertOutStationEntry(entry)
                Log.d("AppViewModel", "Successfully saved Out-Station entry.")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to save out-station log", e)
                _ocrResultText.value = "Failed: ${e.localizedMessage}"
            } finally {
                _ocrLoading.value = false
            }
        }
    }

    // --- Admin Dashboard Utilities ---
    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLineHallEntries()
            repository.clearOutStationEntries()
            repository.clearPartnerAttendance()
        }
    }

    fun deleteLineHall(entry: LineHallEntry) {
        viewModelScope.launch {
            repository.deleteLineHallEntry(entry)
        }
    }

    fun deleteOutStation(entry: OutStationEntry) {
        viewModelScope.launch {
            repository.deleteOutStationEntry(entry)
        }
    }

    fun deletePartnerAttendance(attendance: PartnerAttendance) {
        viewModelScope.launch {
            repository.deletePartnerAttendance(attendance)
        }
    }

    // Utility Base64 converters
    private fun Bitmap.toBase64String(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress heavily to keep DB size compact
        val scaled = scaleDown(this, 400f)
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun scaleDown(realImage: Bitmap, maxImageSize: Float): Bitmap {
        val ratio = Math.min(
            maxImageSize / realImage.width,
            maxImageSize / realImage.height
        )
        if (ratio >= 1.0) return realImage
        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)
        return Bitmap.createScaledBitmap(realImage, width, height, true)
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedString = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: Exception) {
            null
        }
    }

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

data class PartnerInfo(
    val code: String,
    val name: String
)
