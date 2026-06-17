/*
 * ═══════════════════════════════════════════════════════════════════════════
 * ESP8266 Load Timer - Smart IoT Scheduler
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Features:
 *   - WiFi Access Point (192.168.4.1)
 *   - REST API for Android Integration
 *   - PWM Brightness Control for Power BJTs
 *   - EEPROM Persistence
 * 
 * Hardware:
 *   - GPIO2 (D4) -> 1k-4.7k Resistor -> Base of NPN BJT
 * ═══════════════════════════════════════════════════════════════════════════
 */

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <EEPROM.h>

// WiFi Configuration
const char* ap_ssid = "ESP_HOTSPOT";
const char* ap_pass = "12345678";

// GPIO Configuration (GPIO2 = D4 on NodeMCU)
const int LED_PIN = 2;

ESP8266WebServer server(80);

struct Schedule {
    int startHour;
    int startMinute;
    char startPeriod[3];
    int endHour;
    int endMinute;
    char endPeriod[3];
    bool isValid;
    int brightness;
};

Schedule schedule;
bool ledState = false;

// Time Tracking
unsigned long lastMillis = 0;
int currentHour = 0;
int currentMinute = 0;
int currentSecond = 0;
bool timeInitialized = false;

const int EEPROM_ADDR = 0;
const int EEPROM_SIZE = sizeof(Schedule);

void setup() {
    Serial.begin(115200);
    
    pinMode(LED_PIN, OUTPUT);
    analogWrite(LED_PIN, 0);

    EEPROM.begin(EEPROM_SIZE);
    loadSchedule();

    if (schedule.brightness < 0 || schedule.brightness > 1023) {
        schedule.brightness = 1023;
    }

    WiFi.softAP(ap_ssid, ap_pass);
    Serial.println("\n=== ESP Started ===");
    Serial.print("IP: "); Serial.println(WiFi.softAPIP());

    server.on("/", HTTP_GET, handleRoot);
    server.on("/setSchedule", HTTP_POST, handleSetSchedule);
    server.on("/getSchedule", HTTP_GET, handleGetSchedule);
    server.on("/getSchedule/json", HTTP_GET, handleGetScheduleJson);
    server.on("/clearSchedule", HTTP_POST, handleClearSchedule);
    server.on("/status", HTTP_GET, handleStatus);
    server.on("/syncTime", HTTP_POST, handleSyncTime);
    server.on("/setBrightness", HTTP_POST, handleSetBrightness);

    server.begin();
}

void loop() {
    server.handleClient();
    updateCurrentTime();
    checkSchedule();
    delay(20);
}

void updateCurrentTime() {
    if (!timeInitialized) return;
    unsigned long now = millis();
    unsigned long delta = now - lastMillis;
    if (delta < 1000) return;

    lastMillis = now;
    unsigned long deltaSeconds = delta / 1000;
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

void handleSyncTime() {
    if (server.hasArg("hour") && server.hasArg("minute")) {
        currentHour = server.arg("hour").toInt();
        currentMinute = server.arg("minute").toInt();
        currentSecond = server.hasArg("second") ? server.arg("second").toInt() : 0;
        lastMillis = millis();
        timeInitialized = true;
        server.send(200, "application/json", "{}");
    } else {
        server.send(400, "application/json", "{\"error\":\"Missing params\"}");
    }
}

void loadSchedule() {
    EEPROM.get(EEPROM_ADDR, schedule);
    if (schedule.startHour < 0 || schedule.startHour > 23) {
        schedule.isValid = false;
        schedule.brightness = 1023;
    }
}

void saveSchedule() {
    EEPROM.put(EEPROM_ADDR, schedule);
    EEPROM.commit();
}

void checkSchedule() {
    if (!schedule.isValid || !timeInitialized) return;

    int curMin = currentHour * 60 + currentMinute;
    int startMin = schedule.startHour * 60 + schedule.startMinute;
    int endMin = schedule.endHour * 60 + schedule.endMinute;

    bool shouldBeOn = false;
    if (startMin > endMin) { // Midnight crossing
        shouldBeOn = (curMin >= startMin || curMin < endMin);
    } else {
        shouldBeOn = (curMin >= startMin && curMin < endMin);
    }

    if (shouldBeOn != ledState) {
        ledState = shouldBeOn;
        analogWrite(LED_PIN, ledState ? schedule.brightness : 0);
        Serial.printf("Output: %s (PWM: %d)\n", ledState ? "ON" : "OFF", schedule.brightness);
    }
}

void handleSetBrightness() {
    if (server.hasArg("brightness")) {
        int b = server.arg("brightness").toInt();
        if (b >= 0 && b <= 1023) {
            schedule.brightness = b;
            saveSchedule();
            if (ledState) analogWrite(LED_PIN, schedule.brightness);
            server.send(200, "application/json", "{}");
        } else {
            server.send(400, "application/json", "{\"error\":\"Range 0-1023\"}");
        }
    }
}

void handleSetSchedule() {
    if (server.hasArg("startHour") && server.hasArg("startMin") && server.hasArg("startPeriod") &&
        server.hasArg("endHour") && server.hasArg("endMin") && server.hasArg("endPeriod")) {

        schedule.startHour = convertTo24Hour(server.arg("startHour").toInt(), server.arg("startPeriod"));
        schedule.startMinute = server.arg("startMin").toInt();
        schedule.endHour = convertTo24Hour(server.arg("endHour").toInt(), server.arg("endPeriod"));
        schedule.endMinute = server.arg("endMin").toInt();

        strncpy(schedule.startPeriod, server.arg("startPeriod").c_str(), 2);
        schedule.startPeriod[2] = '\0';
        strncpy(schedule.endPeriod, server.arg("endPeriod").c_str(), 2);
        schedule.endPeriod[2] = '\0';

        schedule.isValid = true;
        saveSchedule();
        server.send(200, "application/json", "{}");
        Serial.println("Schedule Set");
    } else {
        server.send(400, "application/json", "{\"error\":\"Missing params\"}");
    }
}

void handleGetScheduleJson() {
    String json = "{";
    json += "\"valid\":" + String(schedule.isValid ? "true" : "false") + ",";
    json += "\"startHour\":" + String(schedule.startHour) + ",";
    json += "\"startMinute\":" + String(schedule.startMinute) + ",";
    json += "\"endHour\":" + String(schedule.endHour) + ",";
    json += "\"endMinute\":" + String(schedule.endMinute) + ",";
    json += "\"startPeriod\":\"" + String(schedule.startPeriod) + "\",";
    json += "\"endPeriod\":\"" + String(schedule.endPeriod) + "\",";
    json += "\"brightness\":" + String(schedule.brightness) + ",";
    json += "\"ledState\":" + String(ledState ? "true" : "false");
    json += "}";
    server.send(200, "application/json", json);
}

void handleStatus() {
    String json = "{";
    json += "\"ledState\":" + String(ledState ? "true" : "false") + ",";
    json += "\"scheduleValid\":" + String(schedule.isValid ? "true" : "false") + ",";
    json += "\"timeInitialized\":" + String(timeInitialized ? "true" : "false") + ",";
    json += "\"brightness\":" + String(schedule.brightness) + ",";
    json += "\"currentHour\":" + String(currentHour) + ",";
    json += "\"currentMinute\":" + String(currentMinute);
    json += "}";
    server.send(200, "application/json", json);
}

void handleClearSchedule() {
    schedule.isValid = false;
    saveSchedule();
    analogWrite(LED_PIN, 0);
    ledState = false;
    server.send(200, "application/json", "{}");
}

int convertTo24Hour(int hour, String period) {
    period.toUpperCase();
    if (period == "PM" && hour != 12) hour += 12;
    else if (period == "AM" && hour == 12) hour = 0;
    return hour;
}

void handleRoot() {
    String html = "<html><body><h2>ESP Scheduler</h2>";
    html += "Brightness: <input type='range' min='0' max='1023' value='" + String(schedule.brightness) + "' onchange=\"fetch('/setBrightness?brightness='+this.value,{method:'POST'})\">";
    html += "</body></html>";
    server.send(200, "text/html", html);
}

void handleGetSchedule() {
    server.send(200, "text/plain", schedule.isValid ? "Schedule Active" : "No Schedule");
}
