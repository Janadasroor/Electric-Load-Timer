
// API Service
package com.example.loadtimeresp.data.api


import androidx.core.view.RoundedCornerCompat
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface ApiService {

    @POST("/setSchedule")
    suspend fun setSchedule(
        @Query("startHour") startHour: Int,
        @Query("startMin") startMin: Int,
        @Query("endHour") endHour: Int,
        @Query("endMin") endMin: Int,
        @Query("startPeriod") startPeriod: String,
        @Query("endPeriod") endPeriod: String
    ): Response<Unit>
    @POST("/syncTime")
    suspend fun syncTime(
        @Query("hour") hour: Int,
        @Query("minute") minute: Int,
        @Query("second") second: Int
    ): Response<Unit>
    @GET("/getSchedule/json")
    suspend fun getSchedule(): Response<ScheduleResponse>
    @GET("/status")
    suspend fun getStatus(): Response<StatusResponse>
    @POST("/clearSchedule")
    suspend fun clearSchedule(): Response<Unit>
}
 @kotlinx.serialization.Serializable
data class StatusResponse(
    val ledState: Boolean,
    val scheduleValid: Boolean,
    val timeInitialized: Boolean,
    val currentHour: Int,
    val currentMinute: Int
)
@kotlinx.serialization.Serializable
data class ScheduleResponse(
    val valid: Boolean,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null,
    val startPeriod: String? = null,
    val endPeriod: String? = null,
    val timeInitialized: Boolean? = null,
    val currentHour: Int? = null,
    val currentMinute: Int? = null,
    val ledState: Boolean? = null
)
