/*
 * ═══════════════════════════════════════════════════════════════════════════
 * ESP8266 Load Timer - Smart IoT Scheduler
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Description:
 *   This firmware turns an ESP8266 into a WiFi-enabled timer that can control
 *   electrical loads (lights, fans, pumps, etc.) based on a user-defined schedule.
 *   
 * Features:
 *   - Creates WiFi Access Point for easy connection
 *   - RESTful API for Android app integration
 *   - Web-based control interface
 *   - Persistent schedule storage in EEPROM
 *   - Handles schedules that cross midnight
 *   - Time synchronization from client devices
 * 
 * Hardware:
 *   - ESP8266 (NodeMCU, Wemos D1 Mini, etc.)
 *   - Relay module connected to GPIO2 (D4)
 *   - Electrical load connected through relay
 * 
 * Author: Your Name
 * License: MIT
 * ═══════════════════════════════════════════════════════════════════════════
 */

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <EEPROM.h>

// ═══════════════════════════════════════════════════════════════════════════
// CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════

// WiFi Access Point credentials
// The ESP8266 will create a WiFi network with these credentials
const char* ap_ssid = \"ESP_HOTSPOT\";  // Network name (SSID)
const char* ap_pass = \"12345678\";     // Network password (min 8 characters)

// GPIO pin for controlling the relay/LED
// GPIO2 is the built-in LED on most ESP8266 boards (active LOW)
// For external relay, use D1, D2, D5, D6, D7, etc.
const int LED_PIN = 2;

// Web server instance running on port 80 (standard HTTP port)
ESP8266WebServer server(80);

// ═══════════════════════════════════════════════════════════════════════════
// DATA STRUCTURES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Schedule structure to store timer configuration
 * 
 * Note: Using char arrays instead of String objects to avoid
 * memory fragmentation and ensure EEPROM compatibility
 */
struct Schedule {
    int startHour;              // Start hour in 24-hour format (0-23)
    int startMinute;            // Start minute (0-59)
    char startPeriod[3];        // \"AM\" or \"PM\" for display purposes
    int endHour;                // End hour in 24-hour format (0-23)
    int endMinute;              // End minute (0-59)
    char endPeriod[3];          // \"AM\" or \"PM\" for display purposes
    bool isValid;               // Flag indicating if schedule is set
};

// ═══════════════════════════════════════════════════════════════════════════
// GLOBAL VARIABLES
// ═══════════════════════════════════════════════════════════════════════════

Schedule schedule;              // Current schedule configuration
bool ledState = false;          // Current state of the LED/relay (true = ON)

// Time tracking variables
// The ESP8266 doesn't have a real-time clock, so we track time
// using millis() and sync it from the client device
unsigned long lastMillis = 0;   // Last millis() value when time was updated
int currentHour = 0;            // Current hour (0-23)
int currentMinute = 0;          // Current minute (0-59)
int currentSecond = 0;          // Current second (0-59)
bool timeInitialized = false;   // Flag indicating if time has been synced

// EEPROM configuration
// EEPROM is used to persist the schedule across power cycles
const int EEPROM_ADDR = 0;                  // Starting address in EEPROM
const int EEPROM_SIZE = sizeof(Schedule);   // Size of data to store

// ═══════════════════════════════════════════════════════════════════════════
// SETUP - Runs once when ESP8266 boots up
// ═══════════════════════════════════════════════════════════════════════════

void setup() {
    // Initialize serial communication for debugging
    // Baud rate: 115200 (standard for ESP8266)
    Serial.begin(115200);
    
    // Configure LED pin as output and turn it OFF initially
    // Note: Built-in LED is active LOW (HIGH = OFF, LOW = ON)
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(LED_PIN, HIGH);  // Turn OFF

    // Initialize EEPROM with the required size
    // This allocates memory for persistent storage
    EEPROM.begin(EEPROM_SIZE);
    
    // Load previously saved schedule from EEPROM (if any)
    loadSchedule();

    // Start WiFi in Access Point mode
    // This creates a WiFi network that devices can connect to
    WiFi.softAP(ap_ssid, ap_pass);
    
    // Print connection information to serial monitor
    Serial.println(\"\\n=== AP Started ===\");
    Serial.print(\"SSID: \");
    Serial.println(ap_ssid);
    Serial.print(\"Password: \");
    Serial.println(ap_pass);
    Serial.print(\"AP IP: \");
    Serial.println(WiFi.softAPIP());  // Usually 192.168.4.1

    // ═══════════════════════════════════════════════════════════════════════
    // Register HTTP endpoints (routes)
    // ═══════════════════════════════════════════════════════════════════════
    
    // Web interface (HTML page)
    server.on(\"/\", HTTP_GET, handleRoot);
    
    // API endpoints for Android app
    server.on(\"/setSchedule\", HTTP_POST, handleSetSchedule);      // Set new schedule
    server.on(\"/getSchedule\", HTTP_GET, handleGetSchedule);       // Get schedule (HTML)
    server.on(\"/getSchedule/json\", HTTP_GET, handleGetScheduleJson);  // Get schedule (JSON)
    server.on(\"/clearSchedule\", HTTP_POST, handleClearSchedule);  // Clear schedule
    server.on(\"/status\", HTTP_GET, handleStatus);                 // Get device status
    server.on(\"/syncTime\", HTTP_POST, handleSyncTime);            // Sync time from client

    // Start the web server
    server.begin();
    Serial.println(\"HTTP server started\");
    Serial.println(\"Waiting for time sync from client...\");

    // Print loaded schedule if valid
    if (schedule.isValid) {
        Serial.println(\"Loaded schedule from EEPROM:\");
        printSchedule();
    } else {
        Serial.println(\"No valid schedule found in EEPROM\");
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN LOOP - Runs continuously
// ═══════════════════════════════════════════════════════════════════════════

void loop() {
    // Handle incoming HTTP requests
    server.handleClient();
    
    // Update the current time based on elapsed milliseconds
    updateCurrentTime();
    
    // Check if we should turn the load ON or OFF based on schedule
    checkSchedule();
    
    // Small delay to prevent overwhelming the CPU
    delay(20);
}

// ═══════════════════════════════════════════════════════════════════════════
// TIME MANAGEMENT FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Update current time based on millis()
 * 
 * Since ESP8266 doesn't have RTC, we track time by counting milliseconds
 * since the last time sync. This function updates hours, minutes, and seconds.
 * 
 * Limitations:
 *   - Accuracy depends on millis() which can drift slightly
 *   - Time resets on power cycle (needs re-sync)
 *   - Recommended to sync time daily for best accuracy
 */
void updateCurrentTime() {
    // Don't update if time hasn't been initialized yet
    if (!timeInitialized) return;

    unsigned long now = millis();  // Get current milliseconds since boot

    // Calculate time elapsed since last update
    unsigned long delta = now - lastMillis;

    // Only update if at least 1 second has passed
    // This prevents unnecessary calculations
    if (delta < 1000) return;

    // Update lastMillis for next iteration
    lastMillis = now;

    // Calculate how many seconds have passed
    // Integer division: 1500ms / 1000 = 1 second
    unsigned long deltaSeconds = delta / 1000;

    // Add elapsed seconds to current time
    currentSecond += deltaSeconds;

    // Handle second overflow (60 seconds = 1 minute)
    if (currentSecond >= 60) {
        currentMinute += currentSecond / 60;  // Add full minutes
        currentSecond %= 60;                   // Keep remainder seconds
    }

    // Handle minute overflow (60 minutes = 1 hour)
    if (currentMinute >= 60) {
        currentHour += currentMinute / 60;     // Add full hours
        currentMinute %= 60;                   // Keep remainder minutes
    }

    // Handle hour overflow (24 hours = new day)
    // Modulo 24 keeps hour in range 0-23
    currentHour %= 24;
}

/**
 * Handle time synchronization from client device
 * 
 * Endpoint: POST /syncTime
 * Parameters: hour, minute, second
 * Response: JSON
 * 
 * This is called by the Android app or web interface to set the ESP's time
 * to match the client device's time.
 */
void handleSyncTime() {
    // Check if all required parameters are present
    if (server.hasArg(\"hour\") && server.hasArg(\"minute\") && server.hasArg(\"second\")) {
        // Parse parameters from URL query string
        int hour = server.arg(\"hour\").toInt();
        int minute = server.arg(\"minute\").toInt();
        int second = server.arg(\"second\").toInt();

        // Validate time values are within acceptable ranges
        if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60 && second >= 0 && second < 60) {
            // Set the current time
            currentHour = hour;
            currentMinute = minute;
            currentSecond = second;

            // CRITICAL: Initialize lastMillis to current millis()
            // This is the reference point for future time calculations
            lastMillis = millis();
            timeInitialized = true;

            // Log to serial monitor
            Serial.print(\"Time synced: \");
            Serial.print(currentHour);
            Serial.print(\":\");
            if (currentMinute < 10) Serial.print(\"0\");  // Leading zero
            Serial.print(currentMinute);
            Serial.print(\":\");
            if (currentSecond < 10) Serial.print(\"0\");  // Leading zero
            Serial.println(currentSecond);

            // Return success response (empty JSON object)
            server.send(200, \"application/json\", \"{}\");
        } else {
            // Invalid time values
            server.send(400, \"application/json\", \"{\\\"error\\\":\\\"Invalid time values\\\"}\");
        }
    } else {
        // Missing parameters
        server.send(400, \"application/json\", \"{\\\"error\\\":\\\"Missing parameters\\\"}\");
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EEPROM FUNCTIONS - Persistent Storage
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Load schedule from EEPROM
 * 
 * Reads the schedule structure from EEPROM and validates it.
 * If the data is corrupted or invalid, marks schedule as invalid.
 */
void loadSchedule() {
    // Read schedule structure from EEPROM
    EEPROM.get(EEPROM_ADDR, schedule);

    // Validate loaded data to prevent using corrupted values
    // Check if hours and minutes are within valid ranges
    if (schedule.startHour < 0 || schedule.startHour > 23 ||
        schedule.endHour < 0 || schedule.endHour > 23 ||
        schedule.startMinute < 0 || schedule.startMinute > 59 ||
        schedule.endMinute < 0 || schedule.endMinute > 59) {
        // Data is invalid, mark schedule as not valid
        schedule.isValid = false;
    }
}

/**
 * Save schedule to EEPROM
 * 
 * Writes the current schedule to EEPROM for persistence across power cycles.
 * The schedule will be automatically loaded on next boot.
 */
void saveSchedule() {
    // Write schedule structure to EEPROM
    EEPROM.put(EEPROM_ADDR, schedule);
    
    // Commit changes to flash memory
    // Without this, changes won't be saved!
    EEPROM.commit();
    
    Serial.println(\"Schedule saved to EEPROM\");
}

// ═══════════════════════════════════════════════════════════════════════════
// SCHEDULE CHECKING - Core Logic
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Check if current time is within the scheduled period
 * 
 * This function is called every loop iteration to determine if the load
 * should be ON or OFF based on the current time and schedule.
 * 
 * Special handling:
 *   - Schedules that cross midnight (e.g., 10 PM to 6 AM)
 *   - Only updates LED state when it needs to change (reduces wear)
 */
void checkSchedule() {
    // Don't check if schedule is invalid or time not initialized
    if (!schedule.isValid || !timeInitialized) {
        return;
    }

    // Convert all times to minutes since midnight for easier comparison
    // Example: 14:30 = 14*60 + 30 = 870 minutes
    int currentTimeInMinutes = currentHour * 60 + currentMinute;
    int startTimeInMinutes = schedule.startHour * 60 + schedule.startMinute;
    int endTimeInMinutes = schedule.endHour * 60 + schedule.endMinute;

    bool shouldBeOn = false;

    // Handle schedule that crosses midnight
    // Example: Start 22:00 (1320 min), End 06:00 (360 min)
    if (startTimeInMinutes > endTimeInMinutes) {
        // Load should be ON if current time is:
        // - After start time (e.g., 22:00-23:59), OR
        // - Before end time (e.g., 00:00-06:00)
        shouldBeOn = (currentTimeInMinutes >= startTimeInMinutes ||
                      currentTimeInMinutes < endTimeInMinutes);
    } else {
        // Normal schedule (doesn't cross midnight)
        // Load should be ON if current time is between start and end
        shouldBeOn = (currentTimeInMinutes >= startTimeInMinutes &&
                      currentTimeInMinutes < endTimeInMinutes);
    }

    // Only update LED state if it needs to change
    // This prevents unnecessary GPIO writes and serial output
    if (shouldBeOn != ledState) {
        ledState = shouldBeOn;
        
        // Update GPIO pin
        // Note: Built-in LED is active LOW (LOW = ON, HIGH = OFF)
        digitalWrite(LED_PIN, ledState ? LOW : HIGH);
        
        // Log state change to serial monitor
        Serial.print(\"LED turned \");
        Serial.println(ledState ? \"ON\" : \"OFF\");
        Serial.print(\"Current time: \");
        Serial.print(currentHour);
        Serial.print(\":\");
        if (currentMinute < 10) Serial.print(\"0\");
        Serial.println(currentMinute);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TIME CONVERSION UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Convert 12-hour format to 24-hour format
 * 
 * Examples:
 *   - 12:00 AM → 0:00 (midnight)
 *   - 1:00 AM → 1:00
 *   - 12:00 PM → 12:00 (noon)
 *   - 1:00 PM → 13:00
 *   - 11:59 PM → 23:59
 * 
 * @param hour   Hour in 12-hour format (1-12)
 * @param period \"AM\" or \"PM\"
 * @return       Hour in 24-hour format (0-23)
 */
int convertTo24Hour(int hour, String period) {
    period.toUpperCase();  // Ensure uppercase for comparison

    if (period == \"PM\") {
        // PM times: add 12 hours, except for 12 PM (noon)
        if (hour != 12) {
            hour += 12;
        }
    } else {  // AM
        // AM times: use as-is, except 12 AM (midnight) becomes 0
        if (hour == 12) {
            hour = 0;
        }
    }

    return hour;
}

/**
 * Format time as string in 12-hour format
 * 
 * Converts 24-hour time to readable 12-hour format with AM/PM
 * 
 * @param hour   Hour in 24-hour format (0-23)
 * @param minute Minute (0-59)
 * @return       Formatted string (e.g., \"2:30 PM\")
 */
String formatTime(int hour, int minute) {
    String period = \"AM\";
    int displayHour = hour;

    // Convert to 12-hour format
    if (hour >= 12) {
        period = \"PM\";
        if (hour > 12) {
            displayHour = hour - 12;
        }
    }
    if (hour == 0) {
        displayHour = 12;  // Midnight is 12 AM
    }

    // Add leading zero to minutes if needed
    String minStr = String(minute);
    if (minute < 10) {
        minStr = \"0\" + minStr;
    }

    return String(displayHour) + \":\" + minStr + \" \" + period;
}

// ═══════════════════════════════════════════════════════════════════════════
// HTTP REQUEST HANDLERS - Web Interface
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Handle root path - Serve HTML web interface
 * 
 * Endpoint: GET /
 * 
 * Serves a complete HTML page with:
 *   - Time display and sync button
 *   - Schedule input form
 *   - Status display
 *   - JavaScript for AJAX requests
 */
void handleRoot() {
    String html = \"<!DOCTYPE html><html><head>\";
    html += \"<meta name='viewport' content='width=device-width, initial-scale=1'>\";
    
    // Embedded CSS for styling
    html += \"<style>\";
    html += \"body{font-family:Arial;max-width:600px;margin:50px auto;padding:20px;background:#f0f0f0;}\";
    html += \".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);}\";
    html += \"h1{color:#333;text-align:center;}\";
    html += \".form-group{margin:15px 0;}\";
    html += \"label{display:block;margin-bottom:5px;font-weight:bold;color:#555;}\";
    html += \"input,select{width:100%;padding:10px;border:1px solid #ddd;border-radius:5px;box-sizing:border-box;}\";
    html += \".time-row{display:flex;gap:10px;}\";
    html += \".time-row>div{flex:1;}\";
    html += \"button{width:100%;padding:12px;margin:10px 0;border:none;border-radius:5px;cursor:pointer;font-size:16px;}\";
    html += \".btn-primary{background:#4CAF50;color:white;}\";
    html += \".btn-danger{background:#f44336;color:white;}\";
    html += \".btn-info{background:#2196F3;color:white;}\";
    html += \".btn-warning{background:#ff9800;color:white;}\";
    html += \".status{padding:15px;margin:15px 0;border-radius:5px;background:#e3f2fd;}\";
    html += \".time-display{text-align:center;font-size:24px;font-weight:bold;color:#2196F3;padding:10px;background:#f5f5f5;border-radius:5px;margin:10px 0;}\";
    html += \"</style></head><body>\";
    html += \"<div class='container'>\";
    html += \"<h1>⏰ ESP8266 LED Timer</h1>\";

    // Time display section
    html += \"<div class='time-display' id='currentTime'>Syncing time...</div>\";
    html += \"<button class='btn-warning' onclick='syncTime()'> Sync Time Now</button>\";

    // Status display area
    html += \"<div id='status' class='status'></div>\";

    // Schedule input form
    html += \"<div class='form-group'>\";
    html += \"<label>Start Time:</label>\";
    html += \"<div class='time-row'>\";
    html += \"<div><input type='number' id='startHour' min='1' max='12' placeholder='Hour' value='8'></div>\";
    html += \"<div><input type='number' id='startMin' min='0' max='59' placeholder='Min' value='0'></div>\";
    html += \"<div><select id='startPeriod'><option>AM</option><option>PM</option></select></div>\";
    html += \"</div></div>\";

    html += \"<div class='form-group'>\";
    html += \"<label>End Time:</label>\";
    html += \"<div class='time-row'>\";
    html += \"<div><input type='number' id='endHour' min='1' max='12' placeholder='Hour' value='6'></div>\";
    html += \"<div><input type='number' id='endMin' min='0' max='59' placeholder='Min' value='0'></div>\";
    html += \"<div><select id='endPeriod'><option>AM</option><option selected>PM</option></select></div>\";
    html += \"</div></div>\";

    // Action buttons
    html += \"<button class='btn-primary' onclick='setSchedule()'>✅ Set Schedule</button>\";
    html += \"<button class='btn-info' onclick='getSchedule()'>📋 Get Current Schedule</button>\";
    html += \"<button class='btn-danger' onclick='clearSchedule()'>🗑️ Clear Schedule</button>\";

    // JavaScript for AJAX functionality
    html += \"<script>\";

    // Sync time function
    html += \"function syncTime(){\";
    html += \"var now=new Date();\";
    html += \"var data={hour:now.getHours(),minute:now.getMinutes(),second:now.getSeconds()};\";
    html += \"fetch('/syncTime',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},\";
    html += \"body:new URLSearchParams(data)}).then(r=>r.text()).then(d=>{\";
    html += \"document.getElementById('status').innerHTML='✅ Time synced: '+now.toLocaleTimeString();\";
    html += \"updateDisplayTime();});\";
    html += \"}\";

    // Update displayed time
    html += \"function updateDisplayTime(){\";
    html += \"var now=new Date();\";
    html += \"document.getElementById('currentTime').innerHTML='Current Time: '+now.toLocaleTimeString();\";
    html += \"}\";

    // Set schedule function
    html += \"function setSchedule(){\";
    html += \"var data={startHour:document.getElementById('startHour').value,\";
    html += \"startMin:document.getElementById('startMin').value,\";
    html += \"startPeriod:document.getElementById('startPeriod').value,\";
    html += \"endHour:document.getElementById('endHour').value,\";
    html += \"endMin:document.getElementById('endMin').value,\";
    html += \"endPeriod:document.getElementById('endPeriod').value};\";
    html += \"fetch('/setSchedule',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},\";
    html += \"body:new URLSearchParams(data)}).then(r=>r.text()).then(d=>{document.getElementById('status').innerHTML=d;});}\";

    // Get schedule function
    html += \"function getSchedule(){\";
    html += \"fetch('/getSchedule').then(r=>r.text()).then(d=>{document.getElementById('status').innerHTML=d;});}\";

    // Clear schedule function
    html += \"function clearSchedule(){\";
    html += \"fetch('/clearSchedule',{method:'POST'}).then(r=>r.text()).then(d=>{document.getElementById('status').innerHTML=d;});}\";

    // Auto-run on page load
    html += \"window.onload=function(){syncTime();getSchedule();setInterval(updateDisplayTime,1000);};\";
    html += \"</script>\";

    html += \"</div></body></html>\";

    server.send(200, \"text/html\", html);
}

// ═══════════════════════════════════════════════════════════════════════════
// HTTP REQUEST HANDLERS - API Endpoints
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Handle schedule setting request
 * 
 * Endpoint: POST /setSchedule
 * Parameters: startHour, startMin, startPeriod, endHour, endMin, endPeriod
 * Response: JSON
 * 
 * Converts 12-hour format to 24-hour, saves to EEPROM, and activates schedule.
 */
void handleSetSchedule() {
    // Verify all required parameters are present
    if (server.hasArg(\"startHour\") && server.hasArg(\"startMin\") &&
        server.hasArg(\"endHour\") && server.hasArg(\"endMin\") &&
        server.hasArg(\"startPeriod\") && server.hasArg(\"endPeriod\")) {

        // Parse parameters
        int startHour = server.arg(\"startHour\").toInt();
        int startMin = server.arg(\"startMin\").toInt();
        String startPeriod = server.arg(\"startPeriod\");

        int endHour = server.arg(\"endHour\").toInt();
        int endMin = server.arg(\"endMin\").toInt();
        String endPeriod = server.arg(\"endPeriod\");

        // Convert 12-hour format to 24-hour format for internal storage
        schedule.startHour = convertTo24Hour(startHour, startPeriod);
        schedule.startMinute = startMin;
        schedule.endHour = convertTo24Hour(endHour, endPeriod);
        schedule.endMinute = endMin;

        // Store period strings as char arrays (for EEPROM compatibility)
        strncpy(schedule.startPeriod, startPeriod.c_str(), 2);
        schedule.startPeriod[2] = '\\0';  // Null terminator
        strncpy(schedule.endPeriod, endPeriod.c_str(), 2);
        schedule.endPeriod[2] = '\\0';    // Null terminator

        // Mark schedule as valid
        schedule.isValid = true;

        // Save to EEPROM for persistence
        saveSchedule();

        // Return success response
        server.send(200, \"application/json\", \"{}\");

        // Log to serial monitor
        Serial.println(\"\\n=== New Schedule Set ===\");
        printSchedule();

        // Warn if time not initialized
        if (!timeInitialized) {
            Serial.println(\"⚠️ Time not initialized - please sync time!\");
        }
    } else {
        // Missing parameters - return error
        server.send(400, \"application/json\", \"{\\\"error\\\":\\\"Missing parameters\\\"}\");
    }
}

/**
 * Handle get schedule request (HTML format for web interface)
 * 
 * Endpoint: GET /getSchedule
 * Response: HTML
 */
void handleGetSchedule() {
    String response;

    if (schedule.isValid) {
        response = \"<strong>📅 Current Schedule:</strong><br>\";
        response += \"Start: \" + formatTime(schedule.startHour, schedule.startMinute) + \"<br>\";
        response += \"End: \" + formatTime(schedule.endHour, schedule.endMinute) + \"<br>\";

        if (timeInitialized) {
            response += \"ESP Time: \" + String(currentHour) + \":\" +
                        (currentMinute < 10 ? \"0\" : \"\") + String(currentMinute) + \"<br>\";
            response += \"LED Status: \" + String(ledState ? \"💡 ON\" : \"⚫ OFF\");
        } else {
            response += \"⚠️ Time not synced yet!\";
        }
    } else {
        response = \"⚠️ No schedule set\";
    }

    server.send(200, \"text/html\", response);
}

/**
 * Handle get schedule request (JSON format for Android app)
 * 
 * Endpoint: GET /getSchedule/json
 * Response: JSON
 * 
 * Returns complete schedule information including current time and LED state.
 */
void handleGetScheduleJson() {
    // If no schedule set, return minimal JSON
    if (!schedule.isValid) {
        server.send(200, \"application/json\", \"{\\\"valid\\\":false}\");
        return;
    }

    // Build JSON response manually (no JSON library to save memory)
    String json = \"{\";
    json += \"\\\"valid\\\":true,\";
    json += \"\\\"startHour\\\":\" + String(schedule.startHour) + \",\";
    json += \"\\\"startMinute\\\":\" + String(schedule.startMinute) + \",\";
    json += \"\\\"endHour\\\":\" + String(schedule.endHour) + \",\";
    json += \"\\\"endMinute\\\":\" + String(schedule.endMinute) + \",\";
    json += \"\\\"startPeriod\\\":\\\"\" + String(schedule.startPeriod) + \"\\\",\";
    json += \"\\\"endPeriod\\\":\\\"\" + String(schedule.endPeriod) + \"\\\",\";
    json += \"\\\"timeInitialized\\\":\" + String(timeInitialized ? \"true\" : \"false\") + \",\";
    json += \"\\\"currentHour\\\":\" + String(currentHour) + \",\";
    json += \"\\\"currentMinute\\\":\" + String(currentMinute) + \",\";
    json += \"\\\"ledState\\\":\" + String(ledState ? \"true\" : \"false\");
    json += \"}\";

    server.send(200, \"application/json\", json);
}

/**
 * Handle clear schedule request
 * 
 * Endpoint: POST /clearSchedule
 * Response: JSON
 * 
 * Clears the schedule, turns off the load, and saves to EEPROM.
 */
void handleClearSchedule() {
    // Mark schedule as invalid
    schedule.isValid = false;
    
    // Save to EEPROM
    saveSchedule();
    
    // Turn off LED/relay
    digitalWrite(LED_PIN, HIGH);  // HIGH = OFF for built-in LED
    ledState = false;

    // Return success response
    server.send(200, \"application/json\", \"{}\");
    
    Serial.println(\"Schedule cleared. LED turned OFF.\");
}

/**
 * Handle status request
 * 
 * Endpoint: GET /status
 * Response: JSON
 * 
 * Returns current device status including LED state, schedule validity,
 * time initialization status, and current time.
 */
void handleStatus() {
    // Build JSON response
    String json = \"{\";
    json += \"\\\"ledState\\\":\" + String(ledState ? \"true\" : \"false\") + \",\";
    json += \"\\\"scheduleValid\\\":\" + String(schedule.isValid ? \"true\" : \"false\") + \",\";
    json += \"\\\"timeInitialized\\\":\" + String(timeInitialized ? \"true\" : \"false\") + \",\";
    json += \"\\\"currentHour\\\":\" + String(currentHour) + \",\";
    json += \"\\\"currentMinute\\\":\" + String(currentMinute);
    json += \"}\";

    server.send(200, \"application/json\", json);
}

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Print schedule to serial monitor
 * 
 * Outputs the current schedule in human-readable format for debugging.
 */
void printSchedule() {
    Serial.print(\"Start: \");
    Serial.println(formatTime(schedule.startHour, schedule.startMinute));
    Serial.print(\"End: \");
    Serial.println(formatTime(schedule.endHour, schedule.endMinute));
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * END OF FILE
 * ═══════════════════════════════════════════════════════════════════════════
 */