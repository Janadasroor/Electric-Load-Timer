#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <EEPROM.h>

// WiFi credentials
const char* ap_ssid = "ESP_HOTSPOT";
const char* ap_pass = "12345678";

// LED Pin
const int LED_PIN = 2; // Built-in LED (use D1, D2, etc. for external LED)

// Web server on port 80
ESP8266WebServer server(80);

// Time schedule structure - FIXED: Use char arrays instead of String
struct Schedule {
    int startHour;
    int startMinute;
    char startPeriod[3];  // "AM" or "PM" + null terminator
    int endHour;
    int endMinute;
    char endPeriod[3];    // "AM" or "PM" + null terminator
    bool isValid;
};

Schedule schedule;
bool ledState = false;

// Current time tracking (from user's device)
unsigned long lastMillis = 0;

int currentHour = 0;
int currentMinute = 0;
int currentSecond = 0;
bool timeInitialized = false;

// EEPROM address for storing schedule
const int EEPROM_ADDR = 0;
const int EEPROM_SIZE = sizeof(Schedule);

void setup() {
    Serial.begin(115200);
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(LED_PIN, HIGH);

    // Initialize EEPROM
    EEPROM.begin(EEPROM_SIZE);
    loadSchedule();

    // Start ESP as WiFi Access Point
    WiFi.softAP(ap_ssid, ap_pass);
    Serial.println("\n=== AP Started ===");
    Serial.print("SSID: ");
    Serial.println(ap_ssid);
    Serial.print("Password: ");
    Serial.println(ap_pass);
    Serial.print("AP IP: ");
    Serial.println(WiFi.softAPIP());

    // Setup web server routes
    server.on("/", HTTP_GET, handleRoot);
    server.on("/setSchedule", HTTP_POST, handleSetSchedule);
    server.on("/getSchedule", HTTP_GET, handleGetSchedule);
    server.on("/getSchedule/json", HTTP_GET, handleGetScheduleJson);
    server.on("/clearSchedule", HTTP_POST, handleClearSchedule);
    server.on("/status", HTTP_GET, handleStatus);
    server.on("/syncTime", HTTP_POST, handleSyncTime);

    server.begin();
    Serial.println("HTTP server started");
    Serial.println("Waiting for time sync from client...");

    if (schedule.isValid) {
        Serial.println("Loaded schedule from EEPROM:");
        printSchedule();
    } else {
        Serial.println("No valid schedule found in EEPROM");
    }
}

void loop() {
    server.handleClient();
    updateCurrentTime();
    checkSchedule();
    delay(20);
}

// Update current time based on millis()
void updateCurrentTime() {
    if (!timeInitialized) return;

    unsigned long now = millis();

    // Calculate time elapsed since last update
    unsigned long delta = now - lastMillis;

    // Only update if at least 1 second has passed
    if (delta < 1000) return;

    // Update lastMillis for next iteration
    lastMillis = now;

    // Calculate how many seconds have passed
    unsigned long deltaSeconds = delta / 1000;

    // Update time
    currentSecond += deltaSeconds;

    if (currentSecond >= 60) {
        currentMinute += currentSecond / 60;
        currentSecond %= 60;
    }

    if (currentMinute >= 60) {
        currentHour += currentMinute / 60;
        currentMinute %= 60;
    }

    currentHour %= 24;
}

// FIXED: Handle time sync from client - returns JSON
void handleSyncTime() {
    if (server.hasArg("hour") && server.hasArg("minute") && server.hasArg("second")) {
        int hour = server.arg("hour").toInt();
        int minute = server.arg("minute").toInt();
        int second = server.arg("second").toInt();

        // Validate inputs
        if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60 && second >= 0 && second < 60) {
            currentHour = hour;
            currentMinute = minute;
            currentSecond = second;

            // IMPORTANT: Initialize lastMillis when time is synced
            lastMillis = millis();
            timeInitialized = true;

            Serial.print("Time synced: ");
            Serial.print(currentHour);
            Serial.print(":");
            if (currentMinute < 10) Serial.print("0");
            Serial.print(currentMinute);
            Serial.print(":");
            if (currentSecond < 10) Serial.print("0");
            Serial.println(currentSecond);

            // Return JSON response
            server.send(200, "application/json", "{}");
        } else {
            server.send(400, "application/json", "{\"error\":\"Invalid time values\"}");
        }
    } else {
        server.send(400, "application/json", "{\"error\":\"Missing parameters\"}");
    }
}

// Load schedule from EEPROM
void loadSchedule() {
    EEPROM.get(EEPROM_ADDR, schedule);

    // Validate loaded data
    if (schedule.startHour < 0 || schedule.startHour > 23 ||
        schedule.endHour < 0 || schedule.endHour > 23 ||
        schedule.startMinute < 0 || schedule.startMinute > 59 ||
        schedule.endMinute < 0 || schedule.endMinute > 59) {
        schedule.isValid = false;
    }
}

// Save schedule to EEPROM
void saveSchedule() {
    EEPROM.put(EEPROM_ADDR, schedule);
    EEPROM.commit();
    Serial.println("Schedule saved to EEPROM");
}

// Check if current time is within schedule
void checkSchedule() {
    if (!schedule.isValid || !timeInitialized) {
        return;
    }

    // Convert times to minutes for easier comparison
    int currentTimeInMinutes = currentHour * 60 + currentMinute;
    int startTimeInMinutes = schedule.startHour * 60 + schedule.startMinute;
    int endTimeInMinutes = schedule.endHour * 60 + schedule.endMinute;

    bool shouldBeOn = false;

    // Handle schedule that crosses midnight
    if (startTimeInMinutes > endTimeInMinutes) {
        shouldBeOn = (currentTimeInMinutes >= startTimeInMinutes ||
                      currentTimeInMinutes < endTimeInMinutes);
    } else {
        shouldBeOn = (currentTimeInMinutes >= startTimeInMinutes &&
                      currentTimeInMinutes < endTimeInMinutes);
    }

    // Update LED state if needed
    if (shouldBeOn != ledState) {
        ledState = shouldBeOn;
        digitalWrite(LED_PIN, ledState ? LOW : HIGH);
        Serial.print("LED turned ");
        Serial.println(ledState ? "ON" : "OFF");
        Serial.print("Current time: ");
        Serial.print(currentHour);
        Serial.print(":");
        if (currentMinute < 10) Serial.print("0");
        Serial.println(currentMinute);
    }
}

// Convert 12-hour format to 24-hour format
int convertTo24Hour(int hour, String period) {
    period.toUpperCase();

    if (period == "PM") {
        if (hour != 12) {
            hour += 12;
        }
    } else { // AM
        if (hour == 12) {
            hour = 0;
        }
    }

    return hour;
}

// Web page handler
void handleRoot() {
    String html = "<!DOCTYPE html><html><head>";
    html += "<meta name='viewport' content='width=device-width, initial-scale=1'>";
    html += "<style>";
    html += "body{font-family:Arial;max-width:600px;margin:50px auto;padding:20px;background:#f0f0f0;}";
    html += ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);}";
    html += "h1{color:#333;text-align:center;}";
    html += ".form-group{margin:15px 0;}";
    html += "label{display:block;margin-bottom:5px;font-weight:bold;color:#555;}";
    html += "input,select{width:100%;padding:10px;border:1px solid #ddd;border-radius:5px;box-sizing:border-box;}";
    html += ".time-row{display:flex;gap:10px;}";
    html += ".time-row>div{flex:1;}";
    html += "button{width:100%;padding:12px;margin:10px 0;border:none;border-radius:5px;cursor:pointer;font-size:16px;}";
    html += ".btn-primary{background:#4CAF50;color:white;}";
    html += ".btn-danger{background:#f44336;color:white;}";
    html += ".btn-info{background:#2196F3;color:white;}";
    html += ".btn-warning{background:#ff9800;color:white;}";
    html += ".status{padding:15px;margin:15px 0;border-radius:5px;background:#e3f2fd;}";
    html += ".time-display{text-align:center;font-size:24px;font-weight:bold;color:#2196F3;padding:10px;background:#f5f5f5;border-radius:5px;margin:10px 0;}";
    html += "</style></head><body>";
    html += "<div class='container'>";
    html += "<h1>⏰ ESP8266 LED Timer</h1>";

    html += "<div class='time-display' id='currentTime'>Syncing time...</div>";
    html += "<button class='btn-warning' onclick='syncTime()'> Sync Time Now</button>";

    html += "<div id='status' class='status'></div>";

    html += "<div class='form-group'>";
    html += "<label>Start Time:</label>";
    html += "<div class='time-row'>";
    html += "<div><input type='number' id='startHour' min='1' max='12' placeholder='Hour' value='8'></div>";
    html += "<div><input type='number' id='startMin' min='0' max='59' placeholder='Min' value='0'></div>";
    html += "<div><select id='startPeriod'><option>AM</option><option>PM</option></select></div>";
    html += "</div></div>";

    html += "<div class='form-group'>";
    html += "<label>End Time:</label>";
    html += "<div class='time-row'>";
    html += "<div><input type='number' id='endHour' min='1' max='12' placeholder='Hour' value='6'></div>";
    html += "<div><input type='number' id='endMin' min='0' max='59' placeholder='Min' value='0'></div>";
    html += "<div><select id='endPeriod'><option>AM</option><option selected>PM</option></select></div>";
    html += "</div></div>";

    html += "<button class='btn-primary' onclick='setSchedule()'>✅ Set Schedule</button>";
    html += "<button class='btn-info' onclick='getSchedule()'> Get Current Schedule</button>";
    html += "<button class='btn-danger' onclick='clearSchedule()'>️ Clear Schedule</button>";

    html += "<script>";

    // Auto sync time on page load
    html += "function syncTime(){";
    html += "var now=new Date();";
    html += "var data={hour:now.getHours(),minute:now.getMinutes(),second:now.getSeconds()};";
    html += "fetch('/syncTime',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},";
    html += "body:new URLSearchParams(data)}).then(r=>r.text()).then(d=>{";
    html += "document.getElementById('status').innerHTML='✅ Time synced: '+now.toLocaleTimeString();";
    html += "updateDisplayTime();});";
    html += "}";

    // Update displayed time
    html += "function updateDisplayTime(){";
    html += "var now=new Date();";
    html += "document.getElementById('currentTime').innerHTML='Current Time: '+now.toLocaleTimeString();";
    html += "}";

    html += "function setSchedule(){";
    html += "var data={startHour:document.getElementById('startHour').value,";
    html += "startMin:document.getElementById('startMin').value,";
    html += "startPeriod:document.getElementById('startPeriod').value,";
    html += "endHour:document.getElementById('endHour').value,";
    html += "endMin:document.getElementById('endMin').value,";
    html += "endPeriod:document.getElementById('endPeriod').value};";
    html += "fetch('/setSchedule',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},";
    html += "body:new URLSearchParams(data)}).then(r=>r.text()).then(d=>{document.getElementById('status').innerHTML=d;});}";

    html += "function getSchedule(){";
    html += "fetch('/getSchedule').then(r=>r.text()).then(d=>{document.getElementById('status').innerHTML=d;});}";

    html += "function clearSchedule(){";
    html += "fetch('/clearSchedule',{method:'POST'}).then(r=>r.text()).then(d=>{document.getElementById('status').innerHTML=d;});}";

    // Auto sync time on load and update display every second
    html += "window.onload=function(){syncTime();getSchedule();setInterval(updateDisplayTime,1000);};";
    html += "</script>";

    html += "</div></body></html>";

    server.send(200, "text/html", html);
}

// FIXED: Handle schedule setting - returns JSON
void handleSetSchedule() {
    if (server.hasArg("startHour") && server.hasArg("startMin") &&
        server.hasArg("endHour") && server.hasArg("endMin") &&
        server.hasArg("startPeriod") && server.hasArg("endPeriod")) {

        int startHour = server.arg("startHour").toInt();
        int startMin = server.arg("startMin").toInt();
        String startPeriod = server.arg("startPeriod");

        int endHour = server.arg("endHour").toInt();
        int endMin = server.arg("endMin").toInt();
        String endPeriod = server.arg("endPeriod");

        // Convert to 24-hour format
        schedule.startHour = convertTo24Hour(startHour, startPeriod);
        schedule.startMinute = startMin;
        schedule.endHour = convertTo24Hour(endHour, endPeriod);
        schedule.endMinute = endMin;

        // Store periods as char arrays
        strncpy(schedule.startPeriod, startPeriod.c_str(), 2);
        schedule.startPeriod[2] = '\0';
        strncpy(schedule.endPeriod, endPeriod.c_str(), 2);
        schedule.endPeriod[2] = '\0';

        schedule.isValid = true;

        saveSchedule();

        // Return JSON response
        server.send(200, "application/json", "{}");

        Serial.println("\n=== New Schedule Set ===");
        printSchedule();

        if (!timeInitialized) {
            Serial.println("⚠️ Time not initialized - please sync time!");
        }
    } else {
        server.send(400, "application/json", "{\"error\":\"Missing parameters\"}");
    }
}

// Handle get schedule (HTML for web interface)
void handleGetSchedule() {
    String response;

    if (schedule.isValid) {
        response = "<strong> Current Schedule:</strong><br>";
        response += "Start: " + formatTime(schedule.startHour, schedule.startMinute) + "<br>";
        response += "End: " + formatTime(schedule.endHour, schedule.endMinute) + "<br>";

        if (timeInitialized) {
            response += "ESP Time: " + String(currentHour) + ":" +
                        (currentMinute < 10 ? "0" : "") + String(currentMinute) + "<br>";
            response += "LED Status: " + String(ledState ? " ON" : "⚫ OFF");
        } else {
            response += "⚠️ Time not synced yet!";
        }
    } else {
        response = "⚠️ No schedule set";
    }

    server.send(200, "text/html", response);
}

// Handle get schedule JSON (for Android app)
void handleGetScheduleJson() {
    // If no schedule set
    if (!schedule.isValid) {
        server.send(200, "application/json", "{\"valid\":false}");
        return;
    }

    // Build JSON manually
    String json = "{";
    json += "\"valid\":true,";
    json += "\"startHour\":" + String(schedule.startHour) + ",";
    json += "\"startMinute\":" + String(schedule.startMinute) + ",";
    json += "\"endHour\":" + String(schedule.endHour) + ",";
    json += "\"endMinute\":" + String(schedule.endMinute) + ",";
    json += "\"startPeriod\":\"" + String(schedule.startPeriod) + "\",";
    json += "\"endPeriod\":\"" + String(schedule.endPeriod) + "\",";
    json += "\"timeInitialized\":" + String(timeInitialized ? "true" : "false") + ",";
    json += "\"currentHour\":" + String(currentHour) + ",";
    json += "\"currentMinute\":" + String(currentMinute) + ",";
    json += "\"ledState\":" + String(ledState ? "true" : "false");
    json += "}";

    server.send(200, "application/json", json);
}

// FIXED: Handle clear schedule - returns JSON
void handleClearSchedule() {
    schedule.isValid = false;
    saveSchedule();
    digitalWrite(LED_PIN, HIGH);
    ledState = false;

    // Return JSON response
    server.send(200, "application/json", "{}");
    Serial.println("Schedule cleared. LED turned OFF.");
}

// Handle status request
void handleStatus() {
    String json = "{";
    json += "\"ledState\":" + String(ledState ? "true" : "false") + ",";
    json += "\"scheduleValid\":" + String(schedule.isValid ? "true" : "false") + ",";
    json += "\"timeInitialized\":" + String(timeInitialized ? "true" : "false") + ",";
    json += "\"currentHour\":" + String(currentHour) + ",";
    json += "\"currentMinute\":" + String(currentMinute);
    json += "}";

    server.send(200, "application/json", json);
}

// Format time as string
String formatTime(int hour, int minute) {
    String period = "AM";
    int displayHour = hour;

    if (hour >= 12) {
        period = "PM";
        if (hour > 12) {
            displayHour = hour - 12;
        }
    }
    if (hour == 0) {
        displayHour = 12;
    }

    String minStr = String(minute);
    if (minute < 10) {
        minStr = "0" + minStr;
    }

    return String(displayHour) + ":" + minStr + " " + period;
}

// Print schedule to serial
void printSchedule() {
    Serial.print("Start: ");
    Serial.println(formatTime(schedule.startHour, schedule.startMinute));
    Serial.print("End: ");
    Serial.println(formatTime(schedule.endHour, schedule.endMinute));
}