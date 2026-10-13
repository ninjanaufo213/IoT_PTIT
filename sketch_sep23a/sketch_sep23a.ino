#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>

// ================= WIFI =================
const char* ssid = "TP-LINK_2B32";
const char* password = "99990000";

// ================= MQTT (giữ nguyên hạ tầng cũ) =================
const char* mqtt_server = "192.168.5.101"; // IP máy chủ MQTT (Mosquitto/EMQX/HiveMQ)
const int mqtt_port = 1884;

const char* mqtt_user = "HoangXuanVinh";      // Để trống "" nếu broker không đặt user
const char* mqtt_password = "B23DCCN929";  // Để trống "" nếu broker không đặt pass

// ================= MQTT TOPIC (khop voi backend) =================
const char* mqttDataTopic = "esp32/iot/data";
const char* mqttControlTopic = "esp32/iot/control";
const char* mqttActionHistoryTopic = "esp32/iot/action-history";

// ================= DHT11 (giữ nguyên chân cũ) =================
#define DHTPIN 4
#define DHTTYPE DHT11

// ================= LDR (giữ nguyên chân cũ) =================
#define LDR_PIN 34

// ================= LED (giữ nguyên chân cũ) =================
#define LED_RED 18
#define LED_GREEN 19
#define LED_YELLOW 21

const uint8_t LED_PINS[] = {LED_RED, LED_GREEN, LED_YELLOW};
const size_t LED_COUNT = sizeof(LED_PINS) / sizeof(LED_PINS[0]);
bool ledStates[LED_COUNT] = {};

const unsigned long SENSOR_INTERVAL_MS = 2000;  // chu ky doc DHT11

DHT dht(DHTPIN, DHTTYPE);
WiFiClient espClient;
PubSubClient client(espClient);

// ================= WIFI SETUP =================
void onWiFiEvent(WiFiEvent_t event, WiFiEventInfo_t info) {
  if (event == ARDUINO_EVENT_WIFI_STA_DISCONNECTED) {
    // 201 = khong thay mang, 2/15/202 = sai mat khau, 205 = router tu choi
    Serial.printf("\nNgat ket noi WiFi, reason=%d\n", info.wifi_sta_disconnected.reason);
  }
}

void setup_wifi() {
  Serial.println();
  Serial.print("Dang ket noi WiFi: ");
  Serial.println(ssid);

  WiFi.onEvent(onWiFiEvent);
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true);
  delay(100);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi da ket noi!");
  Serial.print("IP ESP32: ");
  Serial.println(WiFi.localIP());
}

// ================= LED HELPERS =================
void setLedState(size_t index, bool isOn) {
  ledStates[index] = isOn;
  digitalWrite(LED_PINS[index], isOn ? HIGH : LOW);
}

void setAllLedStates(bool isOn) {
  for (size_t i = 0; i < LED_COUNT; i++) setLedState(i, isOn);
}

// ================= PUBLISH TRANG THAI LED (snapshot: led1:on,led2:off,led3:on) =================
void publishLedStatus() {
  if (!client.connected()) return;

  String payload = "";
  for (size_t i = 0; i < LED_COUNT; i++) {
    if (i > 0) payload += ",";
    payload += "led" + String(i + 1) + ":" + (ledStates[i] ? "on" : "off");
  }

  client.publish(mqttActionHistoryTopic, payload.c_str());
  Serial.print("Response: ");
  Serial.println(payload);
}

// ================= XU LY LENH DIEU KHIEN (dang: led1:on,led2:off,all:on...) =================
bool handleMqttControl(String message) {
  message.trim();
  message.toLowerCase();
  int start = 0;
  bool handled = false;

  while (start < (int)message.length()) {
    int end = message.indexOf(',', start);
    if (end < 0) end = message.length();
    String command = message.substring(start, end);
    command.trim();

    if (command == "all:on") {
      setAllLedStates(true);
      handled = true;
    } else if (command == "all:off") {
      setAllLedStates(false);
      handled = true;
    } else {
      int colon = command.indexOf(':');
      String ledName = colon < 0 ? "" : command.substring(0, colon);
      String requestedState = colon < 0 ? "" : command.substring(colon + 1);

      if (!ledName.startsWith("led") || (requestedState != "on" && requestedState != "off")) {
        Serial.println("Lenh khong hop le: dung all:on, all:off, hoac ledN:on/off");
        return false;
      }

      int ledNumber = ledName.substring(3).toInt();
      if (ledNumber < 1 || ledNumber > (int)LED_COUNT) {
        Serial.println("So LED khong hop le");
        return false;
      }

      setLedState(ledNumber - 1, requestedState == "on");
      handled = true;
    }
    start = end + 1;
  }
  return handled;
}

// ================= MQTT CALLBACK (NHAN LENH) =================
void callback(char* topic, byte* payload, unsigned int length) {
  if (String(topic) != mqttControlTopic) return;

  String message;
  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  Serial.println("\n===== MQTT NHAN LENH =====");
  Serial.print("Topic: ");
  Serial.println(topic);
  Serial.print("Message: ");
  Serial.println(message);

  if (handleMqttControl(message)) {
    publishLedStatus();
  }
}

// ================= MQTT RECONNECT =================
void reconnect() {
  while (!client.connected()) {
    Serial.print("Dang ket noi MQTT...");
    String clientId = "ESP32Client-";
    clientId += String(random(0xffff), HEX);

    // Kiem tra ket noi co can user/pass hay khong
    bool connected = (strlen(mqtt_user) > 0) ?
                     client.connect(clientId.c_str(), mqtt_user, mqtt_password) :
                     client.connect(clientId.c_str());

    if (connected) {
      Serial.println(" Thanh cong!");
      client.subscribe(mqttControlTopic);
      Serial.print("Da dang ky (subscribe) topic: ");
      Serial.println(mqttControlTopic);
      publishLedStatus();
    } else {
      Serial.print(" That bai, rc=");
      Serial.print(client.state());
      Serial.println(" -> Thu lai sau 5 giay");
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);

  // Khoi tao cam bien & chan LED
  dht.begin();
  pinMode(LDR_PIN, INPUT);

  for (size_t i = 0; i < LED_COUNT; i++) {
    pinMode(LED_PINS[i], OUTPUT);
    setLedState(i, false);
  }

  // Ket noi mang
  setup_wifi();

  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // Khong dung delay() de client.loop() chay lien tuc -> nhan lenh LED ngay lap tuc
  static unsigned long lastSensorRead = 0;
  if (millis() - lastSensorRead < SENSOR_INTERVAL_MS) return;
  lastSensorRead = millis();

  // Doc cam bien
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();
  int light = map(analogRead(LDR_PIN), 0, 4095, 100, 0);  // quy doi sang %, dao chieu (cam bien: sang -> ADC nho)

  if (isnan(temperature) || isnan(humidity)) {
    Serial.println("Loi doc cam bien DHT11!");
  } else {
    // Payload khong co timestamp: backend tu gan thoi gian khi nhan ban tin
    char data[100];
    snprintf(data, sizeof(data),
      "{\"temperature\":%.1f,\"humidity\":%.1f,\"light\":%d}",
      temperature, humidity, light
    );

    if (client.publish(mqttDataTopic, data)) {
      Serial.print("Published: ");
      Serial.println(data);
    } else {
      Serial.println("Publish that bai!");
    }
  }
}