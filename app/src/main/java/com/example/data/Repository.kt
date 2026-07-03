package com.example.data

import kotlinx.coroutines.flow.Flow

class FleetRepository(private val database: AppDatabase) {
    val lineHallEntries: Flow<List<LineHallEntry>> = database.lineHallDao().getAllEntries()
    val outStationEntries: Flow<List<OutStationEntry>> = database.outStationDao().getAllEntries()
    val allPartnerAttendance: Flow<List<PartnerAttendance>> = database.partnerAttendanceDao().getAllAttendance()

    fun getPartnerAttendanceByDate(date: String): Flow<List<PartnerAttendance>> {
        return database.partnerAttendanceDao().getAttendanceByDate(date)
    }

    suspend fun insertLineHallEntry(entry: LineHallEntry): Long {
        return database.lineHallDao().insert(entry)
    }

    suspend fun deleteLineHallEntry(entry: LineHallEntry) {
        database.lineHallDao().delete(entry)
    }

    suspend fun clearLineHallEntries() {
        database.lineHallDao().deleteAll()
    }

    suspend fun insertOutStationEntry(entry: OutStationEntry): Long {
        return database.outStationDao().insert(entry)
    }

    suspend fun deleteOutStationEntry(entry: OutStationEntry) {
        database.outStationDao().delete(entry)
    }

    suspend fun clearOutStationEntries() {
        database.outStationDao().deleteAll()
    }

    suspend fun insertPartnerAttendance(attendance: PartnerAttendance) {
        database.partnerAttendanceDao().insert(attendance)
    }

    suspend fun insertAllPartnerAttendance(list: List<PartnerAttendance>) {
        database.partnerAttendanceDao().insertAll(list)
    }

    suspend fun deletePartnerAttendance(attendance: PartnerAttendance) {
        database.partnerAttendanceDao().delete(attendance)
    }

    suspend fun clearPartnerAttendance() {
        database.partnerAttendanceDao().deleteAll()
    }
}
