package com.example.loadtimeresp.data.repositories

import com.example.loadtimeresp.data.api.ApiService
import com.example.loadtimeresp.data.api.ScheduleResponse
import com.example.loadtimeresp.data.api.StatusResponse
import retrofit2.Response
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val api : ApiService
)
{

    suspend fun setSchedule( startHour: Int,
                             endHour: Int,
                             startMin:Int,
                             endMin:Int,
                             startPeriod:String,
                             endPeriod:String,
                             ): Response<Unit> {
        return api.setSchedule(
            startHour,
            startMin,
            endHour,
            endMin,
            startPeriod,
            endPeriod
        )
    }
    suspend fun syncTime(hour: Int,
                         minute: Int,
                         second:Int
    ): Response<Unit>{
        return  api.syncTime(hour ,minute,second)
    }

    suspend fun getSchedule(): Response<ScheduleResponse> {
        return api.getSchedule()
    }

    suspend fun getStatus(): Response<StatusResponse> {
        return api.getStatus()
    }
    suspend fun clearSchedule(): Response<Unit> {
        return api.clearSchedule()
    }
}