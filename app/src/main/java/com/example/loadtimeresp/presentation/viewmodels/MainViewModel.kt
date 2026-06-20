package com.example.loadtimeresp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loadtimeresp.data.api.ScheduleResponse
import com.example.loadtimeresp.data.api.StatusResponse
import com.example.loadtimeresp.data.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _status = MutableStateFlow<StatusResponse?>(null)
    val status = _status.asStateFlow()

    private val _schedule = MutableStateFlow<ScheduleResponse?>(null)
    val schedule = _schedule.asStateFlow()

    private val _logList = MutableStateFlow<List<String>>(emptyList())
    val logList = _logList.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private fun addLog(msg: String) {
        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = formatter.format(java.util.Date())
        val formattedMsg = "[$timestamp] $msg"
        Timber.d(formattedMsg)
        _logList.value = _logList.value + formattedMsg
    }

    fun getStatus() {
        viewModelScope.launch {
            addLog("Requesting status...")
            try {
                val result = repository.getStatus()
                if (result.isSuccessful) {
                    _status.value = result.body()
                    _isConnected.value = true
                    addLog("Status updated: ${result.body()}")
                } else {
                    _isConnected.value = false
                    addLog("Status request failed: ${result.code()}")
                }
            } catch (e: Exception) {
                _isConnected.value = false
                addLog("Status request failed: ${e.message}")
            }
        }
    }

    fun getSchedule() {
        viewModelScope.launch {
            try {
                addLog("Requesting schedule...")
                val result = repository.getSchedule()
                if (result.isSuccessful) {
                    _schedule.value = result.body()
                    _isConnected.value = true
                    addLog("Schedule updated: ${result.body()}")
                } else {
                    _isConnected.value = false
                    addLog("Schedule request failed: ${result.code()}")
                }
            } catch (e: Exception) {
                _isConnected.value = false
                addLog("Schedule request failed: ${e.message}")
            }
        }
    }

    fun setSchedule(
        startHour: Int,
        startMin: Int,
        endHour: Int,
        endMin: Int,
        startPeriod: String,
        endPeriod: String
    ) {
        viewModelScope.launch {
            try {
                addLog("Sending schedule...")
                val result = repository.setSchedule(startHour, endHour, startMin, endMin, startPeriod, endPeriod)
                if (result.isSuccessful) {
                    _isConnected.value = true
                    addLog("Schedule set successfully")
                    getSchedule()
                } else {
                    _isConnected.value = false
                    addLog("Failed to set schedule: ${result.code()}")
                }
            } catch (e: Exception) {
                _isConnected.value = false
                addLog("Failed to set schedule: ${e.message}")
            }
        }
    }

    fun setBrightness(brightness: Int) {
        viewModelScope.launch {
            try {
                addLog("Setting brightness to $brightness...")
                val result = repository.setBrightness(brightness)
                if (result.isSuccessful) {
                    _isConnected.value = true
                    addLog("Brightness set successfully")
                    getStatus()
                } else {
                    _isConnected.value = false
                    addLog("Failed to set brightness: ${result.code()}")
                }
            } catch (e: Exception) {
                _isConnected.value = false
                addLog("Failed to set brightness: ${e.message}")
            }
        }
    }

    fun clearSchedule() {
        viewModelScope.launch {
            try {
                addLog("Clearing schedule...")
                val result = repository.clearSchedule()
                if (result.isSuccessful) {
                    _isConnected.value = true
                    addLog("Schedule cleared")
                    getSchedule()
                    getStatus()
                } else {
                    _isConnected.value = false
                    addLog("Failed to clear schedule: ${result.code()}")
                }
            } catch (e: Exception) {
                _isConnected.value = false
                addLog("Failed to clear schedule: ${e.message}")
            }
        }
    }

    fun syncTime(hour: Int, min: Int, sec: Int) {
        viewModelScope.launch {
            try{
                addLog("Syncing time...")
                val result = repository.syncTime(hour, min, sec)
                if (result.isSuccessful) {
                    _isConnected.value = true
                    addLog("Time synced")
                    getStatus()
                } else {
                    _isConnected.value = false
                    addLog("Failed to sync time: ${result.code()}")
                }
            }catch (e: Exception){
                _isConnected.value = false
                addLog("Failed to sync time: ${e.message}")
            }
        }
    }
}
