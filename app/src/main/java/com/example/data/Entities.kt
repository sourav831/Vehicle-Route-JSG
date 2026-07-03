package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line_hall_entries")
data class LineHallEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val route: String = "Bhubaneswar to Jharsuguda",
    val vehicleNumber: String,
    val timestamp: Long,
    val photoUri: String,
    val operatorUser: String
)

@Entity(tableName = "out_station_entries")
data class OutStationEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val route: String = "Jharsuguda to Bhubaneswar",
    val section: String, // Brajrajnagar, Belpahar, Bandhabahal, Sundargarh, Kuchinda, Bamra
    val isEntry: Boolean, // true for Entry, false for Exit
    val timestamp: Long,
    val photoUri: String,
    val vehicleNumber: String,
    val operatorUser: String
)

@Entity(tableName = "partner_attendance", primaryKeys = ["date", "partnerCode"])
data class PartnerAttendance(
    val date: String, // YYYY-MM-DD
    val partnerCode: String,
    val isPresent: Boolean,
    val timestamp: Long,
    val operatorUser: String
)
