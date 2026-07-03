package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface LineHallDao {
    @Query("SELECT * FROM line_hall_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<LineHallEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LineHallEntry): Long

    @Delete
    suspend fun delete(entry: LineHallEntry)

    @Query("DELETE FROM line_hall_entries")
    suspend fun deleteAll()
}

@Dao
interface OutStationDao {
    @Query("SELECT * FROM out_station_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<OutStationEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: OutStationEntry): Long

    @Delete
    suspend fun delete(entry: OutStationEntry)

    @Query("DELETE FROM out_station_entries")
    suspend fun deleteAll()
}

@Dao
interface PartnerAttendanceDao {
    @Query("SELECT * FROM partner_attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<PartnerAttendance>>

    @Query("SELECT * FROM partner_attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<PartnerAttendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: PartnerAttendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendanceList: List<PartnerAttendance>)

    @Delete
    suspend fun delete(attendance: PartnerAttendance)

    @Query("DELETE FROM partner_attendance")
    suspend fun deleteAll()
}

@Database(
    entities = [
        LineHallEntry::class,
        OutStationEntry::class,
        PartnerAttendance::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lineHallDao(): LineHallDao
    abstract fun outStationDao(): OutStationDao
    abstract fun partnerAttendanceDao(): PartnerAttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fleet_attendance_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
