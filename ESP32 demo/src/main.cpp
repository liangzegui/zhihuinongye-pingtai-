#include <Arduino.h>
#include <Wire.h>
#include <U8g2lib.h>
#include <WiFi.h>
#include <WiFiManager.h>
#include <ESPAsyncWebServer.h>
#include <ArduinoJson.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <time.h>        // 【新增】NTP时间函数所需头文件
#include <Preferences.h> // 【新增】阈值持久化存储
#include <mbedtls/md.h>  // 【新增】HMAC-SHA256签名所需头文件

// ==============================================================================
// OLED屏幕配置
// ==============================================================================
U8G2_SSD1315_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// ==============================================================================
// WiFi & 异步WebServer配置
// ==============================================================================
WiFiManager wm;
AsyncWebServer server(80);
bool manualControl = false;
bool manualPumpState = false;
bool manualFanState = false;
bool manualLightState = false;

// ==============================================================================
// 静态IP配置（当前已禁用，使用DHCP动态获取IP）
// ==============================================================================
// 如需使用固定IP，将 useStaticIP 改为 true，并根据网络环境修改IP
// 手机热点网段参考：
// Android热点: 192.168.43.x (网关 192.168.43.1)
// iPhone热点:  172.20.10.x  (网关 172.20.10.1)
IPAddress staticIP(192, 168, 43, 100);    
IPAddress gateway(192, 168, 43, 1);        
IPAddress subnet(255, 255, 255, 0);        
bool useStaticIP = false;                  // false=动态IP(DHCP), true=固定IP

// ==============================================================================
// 自动控制阈值设置（可通过网页修改）
// ==============================================================================
// 风扇启停阈值
int fanTempThreshold = 30;        // 温度阈值（℃）- 超过此值开启风扇
int fanCO2Threshold = 1000;       // CO2阈值（ppm）- 超过此值开启风扇

// 水泵启停阈值（土壤ADC值，值越大越干燥）
int pumpDroughtThreshold = 3200;  // 土壤干旱阈值 - 超过此值开启水泵（对应中旱）

// 灯泡启停阈值（使用光照强度lux判断，更直观）
int lightLuxThreshold = 800;      // 光照强度阈值(lux) - 低于此值开启灯

// 【新增】Preferences对象用于阈值持久化
Preferences preferences;

// ============================================================================
// 华为 IoTDA MQTT 配置
// ============================================================================
const char *mqttHost = "0c303a8ecf.st1.iotda-device.cn-south-1.myhuaweicloud.com"; // 来自控制台直连参数
const uint16_t mqttPort = 8883;
const char *mqttDeviceId = "69568516c00ccb6d4b302187_esp32-001";
const char *mqttUsername = mqttDeviceId; // IoTDA 要求用户名为设备ID
// 【删除】原硬编码的mqttPassword = "Lzg551162"，改为动态计算
const char *deviceSecret = "Lzg551162";  // 【新增】设备密钥明文，用于HMAC-SHA256签名

WiFiClientSecure mqttSecureClient;
PubSubClient mqttClient(mqttSecureClient);

// 【删除】原硬编码的mqttClientId，改为在ensureMqttConnection()中动态生成
// clientId格式：deviceId + "_0_0_" + timestamp(yyyyMMddHH)
String mqttTopicReport = String("$oc/devices/") + mqttDeviceId + "/sys/properties/report";
String mqttTopicCommand = String("$oc/devices/") + mqttDeviceId + "/sys/commands/#";
String mqttTopicPropSet = String("$oc/devices/") + mqttDeviceId + "/sys/properties/set/#";

unsigned long lastMqttReconnect = 0;
const unsigned long mqttReconnectInterval = 5000;
unsigned long lastMqttPublish = 0;
const unsigned long mqttPublishInterval = 10000; // 10 秒上报一次

// ==============================================================================
// 传感器&执行器引脚定义
// ==============================================================================
#define DHT11_PIN 14
#define LIGHT_AO_PIN 34
#define LIGHT_DO_PIN 15
#define SOIL_AO_PIN 35
#define SOIL_DO_PIN 16
#define PUMP_PIN 32
#define PUMP_PIN2 33
#define FAN_PIN 25
#define FAN_PIN2 26
#define LIGHT_LAMP_PIN 2  
#define SGP30_ADDR 0x58
#define SGP30_INIT_RETRY 3
#define SGP30_WARM_UP_TIME 10000

// ==============================================================================
// 传感器参数配置
// ==============================================================================
#define READ_INTERVAL 1000
#define SERIAL_PRINT_INTERVAL 1000
#define LUX_MAX 8800
#define LUX_MIN 50
#define ULTRA_BRIGHT_AO 100
#define BRIGHT_AO 500
#define NORMAL_AO 1500
#define DARK_AO 3500
#define ULTRA_BRIGHT_LUX_MIN 6000
#define BRIGHT_LUX_MIN 3000
#define NORMAL_LUX_MIN 1000
#define DARK_LUX_MAX 800
#define SOIL_OVER_WET_AO 2200
#define SOIL_NORMAL_AO 2800
#define SOIL_LIGHT_DROUGHT_AO 3200
#define SOIL_MID_DROUGHT_AO 3500
#define SOIL_HEAVY_DROUGHT_AO 3800
#define SOIL_EXTREME_DROUGHT_AO 4000

// ==============================================================================
// 全局状态变量
// ==============================================================================
int lastLightLevel = -1;
float lastLightIntensity = 0;
String lastLightZone = "";
int lastDhtTemp = 0;
int lastDhtHumi = 0;
String lastDhtStatus = "";
int lastSoilAO = -1;
int lastSoilDO = -1;
String lastSoilStatus = "";
bool pumpState = false;
bool fanState = false;
bool lightState = false;
uint16_t lastEco2 = 0;
uint16_t lastTvoc = 0;
bool sgp30Available = false;
unsigned long sgp30WarmUpStart = 0;
unsigned long lastSerialPrintTime = 0;

// ==============================================================================
// 函数声明
// ==============================================================================
void handleRoot(AsyncWebServerRequest *request);
void handleData(AsyncWebServerRequest *request);
void handlePumpControl(AsyncWebServerRequest *request);
void handleFanControl(AsyncWebServerRequest *request);
void handleLightControl(AsyncWebServerRequest *request);
void handleModeControl(AsyncWebServerRequest *request);
void handleNotFound(AsyncWebServerRequest *request);
void handleSetThresholds(AsyncWebServerRequest *request);
void handleGetThresholds(AsyncWebServerRequest *request);
void configModeCallback(WiFiManager *myWiFiManager);
void mqttCallback(char *topic, byte *payload, unsigned int length);
void ensureMqttConnection();
void publishTelemetry();
String generateTimestamp();                                           // 【新增】生成yyyyMMddHH格式时间戳
String generateMqttPassword(const char* secret, const String& timestamp); // 【新增】HMAC-SHA256签名生成密码
void handleRemoteProperties(const JsonVariant &properties);
void i2cScan();
bool sgp30Init();
void sgp30ReadData(uint16_t &eco2, uint16_t &tvoc);
void updateOLED(float lightLux, String lightZone, int temp, int humi, int soilAO, String soilStatus, uint16_t eco2, uint16_t tvoc);
void calculateLightIntensity(int aoValue, float &lux, String &zone);
void updateSoilStatus(int aoValue, String &status);
void updateDhtStatus(int temp, int humi, String &status);
void controlPump(bool enable);
void controlFan(bool enable);
void handlePendingStarts();

// ==============================================================================
// DHT11类
// ==============================================================================
class DHT11 {
public:
  explicit DHT11(int pin) : _pin(pin), _temperature(0), _humidity(0), _isReadSuccess(false) {}

  void readData() {
    uint8_t dataBuffer[5] = {0};
    uint8_t bitIndex = 7;
    uint8_t byteIndex = 0;

    pinMode(_pin, OUTPUT);
    digitalWrite(_pin, LOW);
    delay(18);
    digitalWrite(_pin, HIGH);
    delayMicroseconds(40);
    pinMode(_pin, INPUT);

    unsigned long timeout = millis();
    while (digitalRead(_pin) == LOW) {
      if (millis() - timeout > 10) {
        _isReadSuccess = false;
        return;
      }
    }

    timeout = millis();
    while (digitalRead(_pin) == HIGH) {
      if (millis() - timeout > 10) {
        _isReadSuccess = false;
        return;
      }
    }

    for (int i = 0; i < 40; i++) {
      timeout = millis();
      while (digitalRead(_pin) == LOW) {
        if (millis() - timeout > 5) {
          _isReadSuccess = false;
          return;
        }
      }

      unsigned long highStartTime = micros();
      timeout = millis();
      while (digitalRead(_pin) == HIGH) {
        if (millis() - timeout > 5) {
          _isReadSuccess = false;
          return;
        }
      }

      if (micros() - highStartTime > 50) {
        dataBuffer[byteIndex] |= (1 << bitIndex);
      }

      if (bitIndex == 0) {
        bitIndex = 7;
        byteIndex++;
      } else {
        bitIndex--;
      }
    }

    if (dataBuffer[0] + dataBuffer[1] + dataBuffer[2] + dataBuffer[3] == dataBuffer[4]) {
      _humidity = dataBuffer[0];
      _temperature = dataBuffer[2];
      _isReadSuccess = (_temperature >= 10 && _temperature <= 40) && 
                      (_humidity >= 20 && _humidity <= 80);
    } else {
      _isReadSuccess = false;
    }
  }

  int getTemperature() const { return _isReadSuccess ? _temperature : 0; }
  int getHumidity() const { return _isReadSuccess ? _humidity : 0; }
  bool isReadSuccess() const { return _isReadSuccess; }

private:
  int _pin;
  int _temperature;
  int _humidity;
  bool _isReadSuccess;
};
DHT11 dht11(DHT11_PIN);

// ==============================================================================
// I2C扫描函数
// ==============================================================================
void i2cScan() {
  Serial.println("\n扫描I2C设备...");
  byte error, address;
  int nDevices = 0;
  sgp30Available = false;
  
  for(address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    error = Wire.endTransmission();
    if (error == 0) {
      Serial.print("I2C设备地址：0x");
      if (address < 16) Serial.print("0");
      Serial.print(address, HEX);
      Serial.println(" 找到！");
      
      if (address == SGP30_ADDR) {
        Serial.println("  -> 这是SGP30传感器！");
        sgp30Available = true;
      }
      nDevices++;
    } else if (error == 4) {
      Serial.print("地址0x");
      if (address < 16) Serial.print("0");
      Serial.println(address, HEX);
    }
  }
  
  if (nDevices == 0) {
    Serial.println("未找到I2C设备！\n");
  } else {
    Serial.println("I2C扫描完成！\n");
  }
}

// ==============================================================================
// 水泵/风扇启动控制变量（非阻塞软启动）
// ==============================================================================
volatile bool pumpStartPending = false;
volatile bool fanStartPending = false;
volatile unsigned long pumpStartTime = 0;
volatile unsigned long fanStartTime = 0;
#define SOFT_START_DELAY 500  // 启动前延迟500ms让电源稳定（增加延迟）

// ==============================================================================
// 水泵控制函数（带保护延迟）
// ==============================================================================
void controlPump(bool enable) {
  if (enable) {
    digitalWrite(PUMP_PIN, LOW);
    digitalWrite(PUMP_PIN2, LOW);
    pumpStartPending = true;
    pumpStartTime = millis();
    Serial.println("水泵准备启动...");
  } else {
    pumpStartPending = false;
    digitalWrite(PUMP_PIN, LOW);
    digitalWrite(PUMP_PIN2, LOW);
    Serial.println("水泵已关闭");
  }
}

// ==============================================================================
// 风扇控制函数（带保护延迟）
// ==============================================================================
void controlFan(bool enable) {
  if (enable) {
    digitalWrite(FAN_PIN, LOW);
    digitalWrite(FAN_PIN2, LOW);
    fanStartPending = true;
    fanStartTime = millis();
    Serial.println("风扇准备启动...");
  } else {
    fanStartPending = false;
    digitalWrite(FAN_PIN, LOW);
    digitalWrite(FAN_PIN2, LOW);
    Serial.println("风扇已关闭");
  }
}

// ==============================================================================
// 处理延迟启动（在loop中调用，非阻塞）
// 重要：确保水泵和风扇不会同时启动，避免电流冲击
// ==============================================================================
void handlePendingStarts() {
  unsigned long now = millis();
  
  // 优先处理水泵（避免同时启动两个大负载）
  if (pumpStartPending && (now - pumpStartTime >= SOFT_START_DELAY)) {
    // 如果风扇也在等待，让水泵先启动，风扇延后
    if (fanStartPending) {
      fanStartTime = now;  // 重置风扇启动时间，等水泵启动后再启动
    }
    pumpStartPending = false;
    digitalWrite(PUMP_PIN, HIGH);
    digitalWrite(PUMP_PIN2, LOW);
    Serial.println("水泵已启动");
    return;  // 本次循环只处理一个设备
  }
  
  // 处理风扇延迟启动（水泵不在启动中时才启动风扇）
  if (fanStartPending && !pumpStartPending && (now - fanStartTime >= SOFT_START_DELAY)) {
    fanStartPending = false;
    digitalWrite(FAN_PIN, HIGH);
    digitalWrite(FAN_PIN2, LOW);
    Serial.println("风扇已启动");
  }
}

// ==============================================================================
// SGP30传感器操作函数
// ==============================================================================
bool sgp30Init() {
  int retry = 0;
  
  while (retry < SGP30_INIT_RETRY) {
    Wire.beginTransmission(static_cast<uint8_t>(SGP30_ADDR));
    Wire.write(0x20);
    Wire.write(0x03);
    int initErr = Wire.endTransmission();
    
    if (initErr == 0) {
      delay(200);
      sgp30WarmUpStart = millis();
      Serial.println("SGP30初始化成功！");
      return true;
    }
    
    retry++;
    Serial.print("SGP30初始化重试 ");
    Serial.println(retry);
    delay(100);
  }
  
  Serial.println("SGP30初始化失败！");
  return false;
}

void sgp30ReadData(uint16_t &eco2, uint16_t &tvoc) {
  if (!sgp30Available) {
    eco2 = 0;
    tvoc = 0;
    return;
  }

  if (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME) {
    eco2 = 400;
    tvoc = 0;
    return;
  }

  Wire.beginTransmission(static_cast<uint8_t>(SGP30_ADDR));
  Wire.write(0x20);
  Wire.write(0x08);
  int cmdErr = Wire.endTransmission(false);
  
  if (cmdErr != 0) {
    eco2 = lastEco2;
    tvoc = lastTvoc;
    Serial.println("SGP30命令错误！");
    return;
  }

  unsigned long timeout = millis();
  while (Wire.available() < 6 && millis() - timeout < 100) {
    Wire.requestFrom(static_cast<uint8_t>(SGP30_ADDR), static_cast<size_t>(6), static_cast<bool>(true));
    delayMicroseconds(100);
  }

  if (Wire.available() >= 6) {
    eco2 = (Wire.read() << 8) | Wire.read();
    Wire.read();
    tvoc = (Wire.read() << 8) | Wire.read();
    Wire.read();
    
    if (eco2 < 400 || eco2 > 6000) eco2 = lastEco2;
    if (tvoc > 6000) tvoc = lastTvoc;
  } else {
    eco2 = lastEco2;
    tvoc = lastTvoc;
    Serial.println("SGP30数据读取超时！");
  }
}

// ==============================================================================
// OLED更新函数
// ==============================================================================
void updateOLED(float lightLux, String lightZone, int temp, int humi, int soilAO, String soilStatus, uint16_t eco2, uint16_t tvoc) {
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_wqy12_t_gb2312);

  u8g2.setCursor(5, 12);
  u8g2.print("光照: ");
  u8g2.print(lightLux, 0);
  u8g2.print("lux (");
  u8g2.print(lightZone);
  u8g2.print(")");

  u8g2.setCursor(5, 24);
  u8g2.print("温湿度: ");
  u8g2.print(temp);
  u8g2.print("℃ / ");
  u8g2.print(humi);
  u8g2.print("%RH");

  u8g2.setCursor(5, 36);
  u8g2.print("土壤: ");
  u8g2.print(soilAO);
  u8g2.print(" (");
  u8g2.print(soilStatus);
  u8g2.print(")");

  u8g2.setCursor(5, 48);
  if (!sgp30Available) {
    u8g2.print("SGP30: 无数据");
  } else if (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME) {
    u8g2.print("SGP30: 预热中...");
  } else {
    u8g2.print("eCO₂:");
    u8g2.print(eco2);
    u8g2.print("ppm | TVOC:");
    u8g2.print(tvoc);
    u8g2.print("ppb");
  }

  u8g2.setCursor(5, 60);
  u8g2.print(manualControl ? "手动:" : "自动:");
  u8g2.print("泵");
  u8g2.print(pumpState ? "开" : "关");
  u8g2.print("|风");
  u8g2.print(fanState ? "开" : "关");
  u8g2.print("|灯");
  u8g2.print(lightState ? "开" : "关");

  u8g2.sendBuffer();
}

// ==============================================================================
// 光照强度计算函数
// ==============================================================================
void calculateLightIntensity(int aoValue, float &lux, String &zone) {
  if (aoValue <= ULTRA_BRIGHT_AO) {
    lux = ULTRA_BRIGHT_LUX_MIN + (LUX_MAX - ULTRA_BRIGHT_LUX_MIN) * (1 - (float)aoValue/ULTRA_BRIGHT_AO);
    zone = "极亮";
  } else if (aoValue <= BRIGHT_AO) {
    lux = BRIGHT_LUX_MIN + (ULTRA_BRIGHT_LUX_MIN - BRIGHT_LUX_MIN) * (1 - (float)(aoValue-ULTRA_BRIGHT_AO)/(BRIGHT_AO-ULTRA_BRIGHT_AO));
    zone = "明亮";
  } else if (aoValue <= NORMAL_AO) {
    lux = NORMAL_LUX_MIN + (BRIGHT_LUX_MIN - NORMAL_LUX_MIN) * (1 - (float)(aoValue-BRIGHT_AO)/(NORMAL_AO-BRIGHT_AO));
    zone = "正常";
  } else if (aoValue <= DARK_AO) {
    lux = DARK_LUX_MAX + (NORMAL_LUX_MIN - DARK_LUX_MAX) * (1 - (float)(aoValue-NORMAL_AO)/(DARK_AO-NORMAL_AO));
    zone = "偏暗";
  } else {
    lux = LUX_MIN + (DARK_LUX_MAX - LUX_MIN) * (1 - (float)(aoValue-DARK_AO)/(4095-DARK_AO));
    zone = "暗";
  }
  
  lux = constrain(lux, LUX_MIN, LUX_MAX);
}

// ==============================================================================
// 土壤湿度状态更新函数
// ==============================================================================
void updateSoilStatus(int aoValue, String &status) {
  if (aoValue <= SOIL_OVER_WET_AO) {
    status = "过湿";
  } else if (aoValue <= SOIL_NORMAL_AO) {
    status = "正常";
  } else if (aoValue <= SOIL_LIGHT_DROUGHT_AO) {
    status = "轻旱";
  } else if (aoValue <= SOIL_MID_DROUGHT_AO) {
    status = "中旱";
  } else if (aoValue <= SOIL_HEAVY_DROUGHT_AO) {
    status = "重旱";
  } else {
    status = "特旱";
  }
}

// ==============================================================================
// 温湿度状态更新函数
// ==============================================================================
void updateDhtStatus(int temp, int humi, String &status) {
  if (temp >= 15 && temp <= 30 && humi >= 30 && humi <= 70) {
    status = "正常";
  } else {
    status = "异常";
  }
}

// ==============================================================================
// 异步WebServer处理函数
// ==============================================================================
void handleRoot(AsyncWebServerRequest *request) {
  String html = "<!DOCTYPE html><html lang='zh-CN'>";
  html += "<head>";
  html += "<meta charset='UTF-8'>";
  html += "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0'>";
  html += "<title>ESP32智能环境监控系统</title>";
  html += "<link rel='stylesheet' href='https://cdn.bootcdn.net/ajax/libs/font-awesome/6.4.0/css/all.min.css'>";
  html += "<style>";
  html += ":root {";
  html += "  --color-primary: #2563eb;";
  html += "  --color-success: #10b981;";
  html += "  --color-warning: #f59e0b;";
  html += "  --color-danger: #ef4444;";
  html += "  --color-info: #3b82f6;";
  html += "  --color-dark: #1f2937;";
  html += "  --color-light: #f3f4f6;";
  html += "  --color-gray: #6b7280;";
  html += "  --shadow-sm: 0 2px 4px rgba(0,0,0,0.05);";
  html += "  --shadow-md: 0 4px 6px rgba(0,0,0,0.1);";
  html += "  --shadow-lg: 0 10px 15px rgba(0,0,0,0.1);";
  html += "  --radius-sm: 6px;";
  html += "  --radius-md: 12px;";
  html += "  --radius-lg: 16px;";
  html += "  --transition: all 0.2s ease;";
  html += "}";
  html += "* { margin: 0; padding: 0; box-sizing: border-box; }";
  html += "body {";
  html += "  font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;";
  html += "  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);";
  html += "  min-height: 100vh;";
  html += "  color: var(--color-dark);";
  html += "  padding-bottom: 20px;";
  html += "}";
  html += ".navbar {";
  html += "  background: white;";
  html += "  box-shadow: var(--shadow-sm);";
  html += "  padding: 15px 20px;";
  html += "  position: sticky;";
  html += "  top: 0;";
  html += "  z-index: 100;";
  html += "  display: flex;";
  html += "  justify-content: space-between;";
  html += "  align-items: center;";
  html += "}";
  html += ".navbar-title {";
  html += "  font-size: 18px;";
  html += "  font-weight: 600;";
  html += "  color: var(--color-primary);";
  html += "}";
  html += ".status-bar {";
  html += "  display: flex;";
  html += "  align-items: center;";
  html += "  gap: 10px;";
  html += "}";
  html += ".status-indicator {";
  html += "  display: flex;";
  html += "  align-items: center;";
  html += "  font-size: 14px;";
  html += "}";
  html += ".status-dot {";
  html += "  width: 10px;";
  html += "  height: 10px;";
  html += "  border-radius: 50%;";
  html += "  margin-right: 5px;";
  html += "  animation: pulse 2s infinite;";
  html += "}";
  html += ".status-online { background: var(--color-success); }";
  html += ".status-offline { background: var(--color-danger); }";
  html += ".mode-switch {";
  html += "  padding: 8px 16px;";
  html += "  border-radius: var(--radius-sm);";
  html += "  border: none;";
  html += "  background: var(--color-primary);";
  html += "  color: white;";
  html += "  font-size: 14px;";
  html += "  cursor: pointer;";
  html += "  transition: var(--transition);";
  html += "}";
  html += ".mode-switch:hover { opacity: 0.9; }";
  html += ".mode-auto { background: var(--color-warning); }";
  html += ".container {";
  html += "  max-width: 1200px;";
  html += "  margin: 20px auto;";
  html += "  padding: 0 20px;";
  html += "}";
  html += ".overview-grid {";
  html += "  display: grid;";
  html += "  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));";
  html += "  gap: 15px;";
  html += "  margin-bottom: 25px;";
  html += "}";
  html += ".card {";
  html += "  background: white;";
  html += "  border-radius: var(--radius-md);";
  html += "  box-shadow: var(--shadow-sm);";
  html += "  padding: 20px;";
  html += "  transition: var(--transition);";
  html += "}";
  html += ".card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }";
  html += ".card-header {";
  html += "  display: flex;";
  html += "  justify-content: space-between;";
  html += "  align-items: center;";
  html += "  margin-bottom: 15px;";
  html += "  padding-bottom: 10px;";
  html += "  border-bottom: 1px solid var(--color-light);";
  html += "}";
  html += ".card-title {";
  html += "  font-size: 16px;";
  html += "  font-weight: 600;";
  html += "  color: var(--color-dark);";
  html += "}";
  html += ".card-icon {";
  html += "  width: 32px;";
  html += "  height: 32px;";
  html += "  border-radius: 50%;";
  html += "  background: var(--color-light);";
  html += "  display: flex;";
  html += "  align-items: center;";
  html += "  justify-content: center;";
  html += "  color: var(--color-primary);";
  html += "}";
  html += ".data-value {";
  html += "  font-size: 24px;";
  html += "  font-weight: 700;";
  html += "  margin-bottom: 10px;";
  html += "}";
  html += ".data-unit { font-size: 14px; color: var(--color-gray); }";
  html += ".data-status {";
  html += "  font-size: 14px;";
  html += "  padding: 3px 8px;";
  html += "  border-radius: 20px;";
  html += "  display: inline-block;";
  html += "  margin-top: 5px;";
  html += "}";
  html += ".status-normal { background: rgba(16, 185, 129, 0.1); color: var(--color-success); }";
  html += ".status-warning { background: rgba(245, 158, 11, 0.1); color: var(--color-warning); }";
  html += ".status-danger { background: rgba(239, 68, 68, 0.1); color: var(--color-danger); }";
  html += ".status-info { background: rgba(59, 130, 246, 0.1); color: var(--color-info); }";
  html += ".progress-container {";
  html += "  width: 100%;";
  html += "  height: 8px;";
  html += "  background: var(--color-light);";
  html += "  border-radius: 4px;";
  html += "  margin: 10px 0;";
  html += "}";
  html += ".progress-bar {";
  html += "  height: 100%;";
  html += "  border-radius: 4px;";
  html += "  transition: width 0.3s ease;";
  html += "}";
  html += ".progress-normal { background: var(--color-success); }";
  html += ".progress-warning { background: var(--color-warning); }";
  html += ".progress-danger { background: var(--color-danger); }";
  html += ".detail-section {";
  html += "  background: white;";
  html += "  border-radius: var(--radius-md);";
  html += "  box-shadow: var(--shadow-sm);";
  html += "  padding: 20px;";
  html += "  margin-bottom: 25px;";
  html += "}";
  html += ".section-title {";
  html += "  font-size: 18px;";
  html += "  font-weight: 600;";
  html += "  margin-bottom: 20px;";
  html += "  color: var(--color-primary);";
  html += "}";
  html += ".detail-grid {";
  html += "  display: grid;";
  html += "  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));";
  html += "  gap: 15px;";
  html += "}";
  html += ".detail-item {";
  html += "  padding: 10px;";
  html += "  border-radius: var(--radius-sm);";
  html += "  background: var(--color-light);";
  html += "}";
  html += ".detail-label { font-size: 14px; color: var(--color-gray); margin-bottom: 5px; }";
  html += ".detail-value { font-size: 16px; font-weight: 600; }";
  html += ".control-section {";
  html += "  background: white;";
  html += "  border-radius: var(--radius-md);";
  html += "  box-shadow: var(--shadow-sm);";
  html += "  padding: 20px;";
  html += "}";
  html += ".control-grid {";
  html += "  display: grid;";
  html += "  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));";
  html += "  gap: 20px;";
  html += "}";
  html += ".control-card {";
  html += "  border: 1px solid var(--color-light);";
  html += "  border-radius: var(--radius-md);";
  html += "  padding: 15px;";
  html += "}";
  html += ".control-device {";
  html += "  display: flex;";
  html += "  justify-content: space-between;";
  html += "  align-items: center;";
  html += "  margin-bottom: 15px;";
  html += "}";
  html += ".device-name {";
  html += "  font-size: 16px;";
  html += "  font-weight: 600;";
  html += "}";
  html += ".toggle-switch {";
  html += "  position: relative;";
  html += "  width: 50px;";
  html += "  height: 26px;";
  html += "  display: inline-block;";
  html += "}";
  html += ".toggle-switch input { opacity: 0; width: 0; height: 0; }";
  html += ".toggle-slider {";
  html += "  position: absolute;";
  html += "  cursor: pointer;";
  html += "  top: 0; left: 0; right: 0; bottom: 0;";
  html += "  background-color: #ccc;";
  html += "  transition: .4s;";
  html += "  border-radius: 34px;";
  html += "}";
  html += ".toggle-slider:before {";
  html += "  position: absolute;";
  html += "  content: \"\";";
  html += "  height: 18px;";
  html += "  width: 18px;";
  html += "  left: 4px;";
  html += "  bottom: 4px;";
  html += "  background-color: white;";
  html += "  transition: .4s;";
  html += "  border-radius: 50%;";
  html += "}";
  html += "input:checked + .toggle-slider { background-color: var(--color-success); }";
  html += "input:checked + .toggle-slider:before { transform: translateX(24px); }";
  html += "input:disabled + .toggle-slider { background-color: #e5e7eb; cursor: not-allowed; }";
  html += "@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }";
  html += "@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }";
  html += ".loader {";
  html += "  border: 3px solid var(--color-light);";
  html += "  border-top: 3px solid var(--color-primary);";
  html += "  border-radius: 50%;";
  html += "  width: 20px;";
  html += "  height: 20px;";
  html += "  animation: spin 1s linear infinite;";
  html += "  display: inline-block;";
  html += "}";
  html += ".toast {";
  html += "  position: fixed;";
  html += "  bottom: 20px;";
  html += "  left: 50%;";
  html += "  transform: translateX(-50%) translateY(100px);";
  html += "  background: var(--color-dark);";
  html += "  color: white;";
  html += "  padding: 10px 20px;";
  html += "  border-radius: var(--radius-sm);";
  html += "  box-shadow: var(--shadow-lg);";
  html += "  z-index: 999;";
  html += "  opacity: 0;";
  html += "  transition: var(--transition);";
  html += "}";
  html += ".toast.show {";
  html += "  opacity: 1;";
  html += "  transform: translateX(-50%) translateY(0);";
  html += "}";
  html += ".footer {";
  html += "  text-align: center;";
  html += "  margin-top: 20px;";
  html += "  font-size: 12px;";
  html += "  color: var(--color-gray);";
  html += "}";
  html += "</style>";
  html += "</head>";
  html += "<body>";
  
  html += "<div class='toast' id='toast'></div>";
  
  html += "<div class='navbar'>";
  html += "  <div class='navbar-title'>ESP32智能环境监控系统</div>";
  html += "  <div class='status-bar'>";
  html += "    <div class='status-indicator'>";
  html += "      <span class='status-dot status-online' id='connDot'></span>";
  html += "      <span id='connStatus'>在线</span>";
  html += "    </div>";
  html += String("    <button class='mode-switch ") + String(manualControl ? "" : "mode-auto") + "' id='modeBtn'>";
  html += String("      ") + String(manualControl ? "手动模式" : "自动模式") + "";
  html += "    </button>";
  html += "  </div>";
  html += "</div>";
  
  html += "<div class='container'>";
  
  html += "<div class='overview-grid'>";
  
  html += "<div class='card'>";
  html += "  <div class='card-header'>";
  html += "    <div class='card-title'>温湿度</div>";
  html += "    <div class='card-icon'><i class='fa-solid fa-temperature-half'></i></div>";
  html += "  </div>";
  html += String("  <div class='data-value' id='tempValue'>") + String(lastDhtTemp) + "<span class='data-unit'>℃</span></div>";
  html += "  <div class='progress-container'>";
  // 修复1：将const char*转换为String
  html += String("    <div class='progress-bar ") + String(lastDhtTemp > 30 ? "progress-warning" : "progress-normal") + "' id='tempProgress' style='width: " + String(constrain(lastDhtTemp, 0, 50)*2) + "%'></div>";
  html += "  </div>";
  html += String("  <div class='data-value' id='humiValue' style='font-size: 18px;'>") + String(lastDhtHumi) + "<span class='data-unit'>%RH</span></div>";
  html += "  <div class='progress-container'>";
  // 修复2：将const char*转换为String
  html += String("    <div class='progress-bar ") + String(lastDhtHumi < 30 || lastDhtHumi > 70 ? "progress-warning" : "progress-normal") + "' id='humiProgress' style='width: " + String(lastDhtHumi) + "%'></div>";
  html += "  </div>";
  // 修复3：将const char*转换为String
  html += String("  <div class='data-status ") + String(lastDhtStatus == "正常" ? "status-normal" : "status-danger") + "' id='dhtStatus'>";
  html += String("    ") + lastDhtStatus + "";
  html += "  </div>";
  html += "</div>";
  
  html += "<div class='card'>";
  html += "  <div class='card-header'>";
  html += "    <div class='card-title'>光照强度</div>";
  html += "    <div class='card-icon'><i class='fa-solid fa-sun'></i></div>";
  html += "  </div>";
  html += String("  <div class='data-value' id='lightValue'>") + String(round(lastLightIntensity)) + "<span class='data-unit'>lux</span></div>";
  html += "  <div class='progress-container'>";
  // 修复4：将const char*转换为String
  html += String("    <div class='progress-bar ") + String(lastLightZone == "暗" ? "progress-warning" : "progress-normal") + "' id='lightProgress' style='width: " + String(constrain(round(lastLightIntensity)/LUX_MAX*100, 0, 100)) + "%'></div>";
  html += "  </div>";
  // 修复5：将const char*转换为String
  html += String("  <div class='data-status ") + String(lastLightZone == "暗" ? "status-warning" : "status-normal") + "' id='lightZone'>";
  html += String("    ") + lastLightZone + "";
  html += "  </div>";
  html += "</div>";
  
  html += "<div class='card'>";
  html += "  <div class='card-header'>";
  html += "    <div class='card-title'>土壤湿度</div>";
  html += "    <div class='card-icon'><i class='fa-solid fa-seedling'></i></div>";
  html += "  </div>";
  html += String("  <div class='data-value' id='soilValue'>") + String(lastSoilAO) + "<span class='data-unit'>ADC</span></div>";
  html += "  <div class='progress-container'>";
  // 修复6：将const char*转换为String
  html += String("    <div class='progress-bar ") + String(lastSoilStatus == "正常" ? "progress-normal" : (lastSoilStatus == "轻旱" ? "progress-warning" : "progress-danger")) + "' id='soilProgress' style='width: " + String(constrain(100 - (lastSoilAO - SOIL_OVER_WET_AO)/(SOIL_EXTREME_DROUGHT_AO - SOIL_OVER_WET_AO)*100, 0, 100)) + "%'></div>";
  html += "  </div>";
  // 修复7：将const char*转换为String
  html += String("  <div class='data-status ") + String(lastSoilStatus == "正常" ? "status-normal" : (lastSoilStatus == "轻旱" ? "status-warning" : "status-danger")) + "' id='soilStatus'>";
  html += String("    ") + lastSoilStatus + "";
  html += "  </div>";
  html += "</div>";
  
  html += "<div class='card'>";
  html += "  <div class='card-header'>";
  html += "    <div class='card-title'>空气质量</div>";
  html += "    <div class='card-icon'><i class='fa-solid fa-wind'></i></div>";
  html += "  </div>";
  if (!sgp30Available) {
    html += String("  <div class='data-value' style='font-size: 18px;'>eCO₂: <span id='eco2Value'>-") + "</span> ppm</div>";
    html += String("  <div class='data-value' style='font-size: 18px;'>TVOC: <span id='tvocValue'>-") + "</span> ppb</div>";
    html += "  <div class='data-status status-danger' id='sgp30Status'>无数据</div>";
  } else if (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME) {
    html += "  <div class='data-value'>预热中</div>";
    html += String("  <div class='data-value' style='font-size: 18px;'>eCO₂: <span id='eco2Value'>-") + "</span> ppm</div>";
    html += String("  <div class='data-value' style='font-size: 18px;'>TVOC: <span id='tvocValue'>-") + "</span> ppb</div>";
    html += String("  <div class='data-status status-info' id='sgp30Status'>剩余") + String((SGP30_WARM_UP_TIME - (millis() - sgp30WarmUpStart))/1000) + "秒</div>";
  } else {
    html += String("  <div class='data-value' style='font-size: 18px;'>eCO₂: <span id='eco2Value'>") + String(lastEco2) + "</span> ppm</div>";
    html += String("  <div class='data-value' style='font-size: 18px;'>TVOC: <span id='tvocValue'>") + String(lastTvoc) + "</span> ppb</div>";
    html += "  <div class='data-status status-normal' id='sgp30Status'>正常</div>";
  }
  html += "</div>";
  
  html += "</div>";
  
  html += "<div class='detail-section'>";
  html += "  <div class='section-title'>详细传感器数据</div>";
  html += "  <div class='detail-grid'>";
  html += "    <div class='detail-item'>";
  html += "      <div class='detail-label'>光照数字输出</div>";
  html += String("      <div class='detail-value' id='lightDO'>") + String(digitalRead(LIGHT_DO_PIN)) + "</div>";
  html += "    </div>";
  html += "    <div class='detail-item'>";
  html += "      <div class='detail-label'>土壤数字输出</div>";
  html += String("      <div class='detail-value' id='soilDO'>") + String(digitalRead(SOIL_DO_PIN)) + "</div>";
  html += "    </div>";
  html += "    <div class='detail-item'>";
  html += "      <div class='detail-label'>SGP30状态</div>";
  // 修复8：将const char*转换为String
  html += String("      <div class='detail-value' id='sgp30Detail'>") + String(sgp30Available ? (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME ? "预热中" : "正常") : "未连接") + "</div>";
  html += "    </div>";
  html += "    <div class='detail-item'>";
  html += "      <div class='detail-label'>系统运行模式</div>";
  html += String("      <div class='detail-value' id='modeDetail'>") + String(manualControl ? "手动控制" : "自动控制") + "</div>";
  html += "    </div>";
  html += "  </div>";
  html += "</div>";
  
  html += "<div class='control-section'>";
  html += "  <div class='section-title'>设备控制中心</div>";
  html += "  <div class='control-grid'>";
  
  html += "  <div class='control-card'>";
  html += "    <div class='control-device'>";
  html += "      <div class='device-name'>水泵</div>";
  html += "      <label class='toggle-switch'>";
  // 修复9：将const char*转换为String
  html += String("        <input type='checkbox' id='pumpToggle' ") + String(manualControl ? "" : "disabled") + " " + String(pumpState ? "checked" : "") + ">";
  html += "        <span class='toggle-slider'></span>";
  html += "      </label>";
  html += "    </div>";
  // 修复10：将const char*转换为String
  html += String("    <div class='detail-label'>当前状态: <span id='pumpState' class='") + String(pumpState ? "status-normal" : "status-danger") + "'>" + String(pumpState ? "开启" : "关闭") + "</span></div>";
  html += "  </div>";
  
  html += "  <div class='control-card'>";
  html += "    <div class='control-device'>";
  html += "      <div class='device-name'>风扇</div>";
  html += "      <label class='toggle-switch'>";
  // 修复11：将const char*转换为String
  html += String("        <input type='checkbox' id='fanToggle' ") + String(manualControl ? "" : "disabled") + " " + String(fanState ? "checked" : "") + ">";
  html += "        <span class='toggle-slider'></span>";
  html += "      </label>";
  html += "    </div>";
  // 修复12：将const char*转换为String
  html += String("    <div class='detail-label'>当前状态: <span id='fanState' class='") + String(fanState ? "status-normal" : "status-danger") + "'>" + String(fanState ? "开启" : "关闭") + "</span></div>";
  html += "  </div>";
  
  html += "  <div class='control-card'>";
  html += "    <div class='control-device'>";
  html += "      <div class='device-name'>照明灯</div>";
  html += "      <label class='toggle-switch'>";
  // 修复13：将const char*转换为String
  html += String("        <input type='checkbox' id='lightToggle' ") + String(manualControl ? "" : "disabled") + " " + String(lightState ? "checked" : "") + ">";
  html += "        <span class='toggle-slider'></span>";
  html += "      </label>";
  html += "    </div>";
  // 修复14：将const char*转换为String
  html += String("    <div class='detail-label'>当前状态: <span id='lightState' class='") + String(lightState ? "status-normal" : "status-danger") + "'>" + String(lightState ? "开启" : "关闭") + "</span></div>";
  html += "  </div>";
  
  html += "  </div>";
  html += "</div>";

  // ==============================================================================
  // 阈值设置区域
  // ==============================================================================
  html += "<div class='control-section' style='margin-top: 25px;'>";
  html += "  <div class='section-title'>自动控制阈值设置</div>";
  html += "  <div class='threshold-grid' style='display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;'>";
  
  // 风扇阈值设置
  html += "  <div class='control-card'>";
  html += "    <div class='device-name' style='margin-bottom: 15px;'><i class='fa-solid fa-fan' style='margin-right: 8px;'></i>风扇启动阈值</div>";
  html += "    <div class='threshold-item' style='margin-bottom: 12px;'>";
  html += "      <label class='detail-label'>温度阈值 (℃): 超过此值开启</label>";
  html += String("      <input type='number' id='fanTempThreshold' value='") + String(fanTempThreshold) + "' min='20' max='50' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>";
  html += "    </div>";
  html += "    <div class='threshold-item'>";
  html += "      <label class='detail-label'>CO₂阈值 (ppm): 超过此值开启</label>";
  html += String("      <input type='number' id='fanCO2Threshold' value='") + String(fanCO2Threshold) + "' min='400' max='5000' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>";
  html += "    </div>";
  html += "  </div>";
  
  // 水泵阈值设置
  html += "  <div class='control-card'>";
  html += "    <div class='device-name' style='margin-bottom: 15px;'><i class='fa-solid fa-droplet' style='margin-right: 8px;'></i>水泵启动阈值</div>";
  html += "    <div class='threshold-item'>";
  html += "      <label class='detail-label'>土壤干旱阈值 (ADC值): 超过此值开启水泵</label>";
  html += String("      <input type='number' id='pumpDroughtThreshold' value='") + String(pumpDroughtThreshold) + "' min='0' max='5000' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>";
  html += "      <div class='detail-label' style='margin-top: 5px; font-size: 12px; color: #888;'>ADC范围0-4095，值越大越干。参考: 过湿≤2200, 正常2200-2800, 轻旱2800-3200, 中旱3200-3500, 重旱>3500</div>";
  html += "    </div>";
  html += "  </div>";
  
  // 灯泡阈值设置（使用光照强度lux，更直观）
  html += "  <div class='control-card'>";
  html += "    <div class='device-name' style='margin-bottom: 15px;'><i class='fa-solid fa-lightbulb' style='margin-right: 8px;'></i>灯泡启动阈值</div>";
  html += "    <div class='threshold-item'>";
  html += "      <label class='detail-label'>光照强度阈值 (lux): 低于此值开启灯</label>";
  html += String("      <input type='number' id='lightLuxThreshold' value='") + String(lightLuxThreshold) + "' min='50' max='5000' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>";
  html += "      <div class='detail-label' style='margin-top: 5px; font-size: 12px; color: #888;'>参考: 暗<800lux, 偏暗800-1000, 正常1000-3000, 明亮>3000</div>";
  html += "    </div>";
  html += "  </div>";
  
  html += "  </div>";
  
  // 保存按钮
  html += "  <div style='text-align: center; margin-top: 20px;'>";
  html += "    <button id='saveThresholdsBtn' style='padding: 12px 40px; background: var(--color-primary); color: white; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; transition: var(--transition);'>保存阈值设置</button>";
  html += "  </div>";
  html += "</div>";
  
  html += "</div>";
  
  html += "<div class='footer'>";
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  html += String("  <p>最后更新时间: <span id='lastUpdate'>") +
    String(timeinfo->tm_year + 1900) + "年" +
    String(timeinfo->tm_mon + 1) + "月" +
    String(timeinfo->tm_mday) + "日 " +
    String(timeinfo->tm_hour) + ":" +
    String(timeinfo->tm_min) + ":" +
    String(timeinfo->tm_sec) + "</span></p>";
  html += "  <p>ESP32智能监控系统 © 2025</p>";
  html += "</div>";
  
  html += "<script>";
  html += "let refreshIntervalId;";
  html += "let isRefreshing = false;";
  html += String("let currentManualMode = ") + String(manualControl ? "true" : "false") + ";";
  html += "const toast = document.getElementById('toast');";
  
  html += "function showToast(message, duration = 2000) {";
  html += "  toast.textContent = message;";
  html += "  toast.classList.add('show');";
  html += "  setTimeout(() => {";
  html += "    toast.classList.remove('show');";
  html += "  }, duration);";
  html += "}";
  
  html += "window.addEventListener('DOMContentLoaded', function() {";
  html += "  startAutoRefresh();";
  html += "  document.getElementById('modeBtn').addEventListener('click', switchMode);";
  html += "  document.getElementById('pumpToggle').addEventListener('change', (e) => controlDevice('pump', e.target.checked ? 1 : 0));";
  html += "  document.getElementById('fanToggle').addEventListener('change', (e) => controlDevice('fan', e.target.checked ? 1 : 0));";
  html += "  document.getElementById('lightToggle').addEventListener('change', (e) => controlDevice('light', e.target.checked ? 1 : 0));";
  html += "  document.getElementById('saveThresholdsBtn').addEventListener('click', saveThresholds);";
  html += "  loadThresholds();";
  html += "});";
  
  html += "function startAutoRefresh() {";
  html += "  if (refreshIntervalId) clearInterval(refreshIntervalId);";
  html += "  autoRefreshData();";
  html += "  refreshIntervalId = setInterval(autoRefreshData, 1000);";
  html += "}";
  
  html += "function autoRefreshData() {";
  html += "  if (isRefreshing) return;";
  html += "  isRefreshing = true;";
  // 不显示刷新中，只显示在线与离线
  html += "  fetch('/data?rand=' + Math.random(), {cache: 'no-store'})";
  html += "  .then(response => {";
  html += "    if (!response.ok) throw new Error('网络错误');";
  html += "    return response.json();";
  html += "  })";
  html += "  .then(data => {";
  html += "    updatePageUI(data);";
  html += "    currentManualMode = data.manualControl;";
  html += "    document.getElementById('connStatus').textContent = '在线';";
  html += "    document.getElementById('connDot').className = 'status-dot status-online';";
  html += "  })";
  html += "  .catch(error => {";
  html += "    console.error('刷新失败:', error);";
  html += "    document.getElementById('connStatus').textContent = '离线';";
  html += "    document.getElementById('connDot').className = 'status-dot status-offline';";
  html += "    showToast('数据刷新失败，请检查连接', 3000);";
  html += "  })";
  html += "  .finally(() => {";
  html += "    isRefreshing = false;";
  html += "  });";
  html += "}";
  
  html += "function updatePageUI(data) {";
    html += "  currentManualMode = data.manualControl;";
  html += "  document.getElementById('tempValue').innerHTML = data.dhtTemp + ' <span class=\"data-unit\">℃</span>';";
  html += "  document.getElementById('humiValue').innerHTML = data.dhtHumi + ' <span class=\"data-unit\">%RH</span>';";
  html += "  document.getElementById('dhtStatus').textContent = data.dhtStatus;";
  html += "  document.getElementById('dhtStatus').className = 'data-status ' + (data.dhtStatus === '正常' ? 'status-normal' : 'status-danger');";
  html += "  const tempProgress = Math.min(Math.max(data.dhtTemp * 2, 0), 100);";
  html += "  document.getElementById('tempProgress').style.width = tempProgress + '%';";
  html += "  document.getElementById('tempProgress').className = 'progress-bar ' + (data.dhtTemp > 30 ? 'progress-warning' : 'progress-normal');";
  html += "  document.getElementById('humiProgress').style.width = data.dhtHumi + '%';";
  html += "  document.getElementById('humiProgress').className = 'progress-bar ' + (data.dhtHumi < 30 || data.dhtHumi > 70 ? 'progress-warning' : 'progress-normal');";
  
  html += "  document.getElementById('lightValue').innerHTML = data.lightIntensity + ' <span class=\"data-unit\">lux</span>';";
  html += "  document.getElementById('lightZone').textContent = data.lightZone;";
  html += "  document.getElementById('lightZone').className = 'data-status ' + (data.lightZone === '暗' ? 'status-warning' : 'status-normal');";
  // 【优化】使用后端计算的lightProgress，避免前端硬编码LUX_MAX
  html += "  document.getElementById('lightProgress').style.width = data.lightProgress + '%';";
  html += "  document.getElementById('lightProgress').className = 'progress-bar ' + (data.lightZone === '暗' ? 'progress-warning' : 'progress-normal');";
  
  html += "  document.getElementById('soilValue').innerHTML = data.soilAO + ' <span class=\"data-unit\">ADC</span>';";
  html += "  document.getElementById('soilStatus').textContent = data.soilStatus;";
  html += "  document.getElementById('soilStatus').className = 'data-status ' + (data.soilStatus === '正常' ? 'status-normal' : (data.soilStatus === '轻旱' ? 'status-warning' : 'status-danger'));";
  html += String("  const soilProgress = Math.min(Math.max(100 - (data.soilAO - ") + String(SOIL_OVER_WET_AO) + ")/(" + String(SOIL_EXTREME_DROUGHT_AO - SOIL_OVER_WET_AO) + ")*100, 0), 100);";
  html += "  document.getElementById('soilProgress').style.width = soilProgress + '%';";
  html += "  document.getElementById('soilProgress').className = 'progress-bar ' + (data.soilStatus === '正常' ? 'progress-normal' : (data.soilStatus === '轻旱' ? 'progress-warning' : 'progress-danger'));";
  
  html += "  if (data.sgp30Status === '无数据') {";
  html += "    document.getElementById('eco2Value').textContent = '-';";
  html += "    document.getElementById('tvocValue').textContent = '-';";
  html += "    document.getElementById('sgp30Status').textContent = '无数据';";
  html += "    document.getElementById('sgp30Status').className = 'data-status status-danger';";
  html += "    document.getElementById('sgp30Detail').textContent = '未连接';";
  html += "  } else if (data.sgp30Status.includes('预热')) {";
  html += "    document.getElementById('eco2Value').textContent = '-';";
  html += "    document.getElementById('tvocValue').textContent = '-';";
  html += "    document.getElementById('sgp30Status').textContent = data.sgp30Status;";
  html += "    document.getElementById('sgp30Status').className = 'data-status status-info';";
  html += "    document.getElementById('sgp30Detail').textContent = '预热中';";
  html += "  } else {";
  html += "    document.getElementById('eco2Value').textContent = data.eco2;";
  html += "    document.getElementById('tvocValue').textContent = data.tvoc;";
  html += "    document.getElementById('sgp30Status').textContent = '正常';";
  html += "    document.getElementById('sgp30Status').className = 'data-status status-normal';";
  html += "    document.getElementById('sgp30Detail').textContent = '正常';";
  html += "  }";
  
  html += "  document.getElementById('lightDO').textContent = data.lightDO;";
  html += "  document.getElementById('soilDO').textContent = data.soilDO;";
  
  html += "  document.getElementById('pumpState').textContent = data.pumpState ? '开启' : '关闭';";
  html += "  document.getElementById('pumpState').className = data.pumpState ? 'status-normal' : 'status-danger';";
  html += "  document.getElementById('fanState').textContent = data.fanState ? '开启' : '关闭';";
  html += "  document.getElementById('fanState').className = data.fanState ? 'status-normal' : 'status-danger';";
  html += "  document.getElementById('lightState').textContent = data.lightState ? '开启' : '关闭';";
  html += "  document.getElementById('lightState').className = data.lightState ? 'status-normal' : 'status-danger';";
  
  html += "  document.getElementById('modeBtn').textContent = data.manualControl ? '手动模式' : '自动模式';";
  html += "  document.getElementById('modeBtn').className = 'mode-switch ' + (data.manualControl ? '' : 'mode-auto');";
  html += "  document.getElementById('modeDetail').textContent = data.manualControl ? '手动控制' : '自动控制';";
  
  html += "  document.getElementById('pumpToggle').checked = data.pumpState;";
  html += "  document.getElementById('fanToggle').checked = data.fanState;";
  html += "  document.getElementById('lightToggle').checked = data.lightState;";
  html += "  document.getElementById('pumpToggle').disabled = !data.manualControl;";
  html += "  document.getElementById('fanToggle').disabled = !data.manualControl;";
  html += "  document.getElementById('lightToggle').disabled = !data.manualControl;";
  
  html += "  if (data.lastUpdate) { document.getElementById('lastUpdate').textContent = data.lastUpdate; }";
  html += "}";
  
  html += "function switchMode() {";
  html += "  if (isRefreshing) return;";
  html += "  fetch('/mode?state=' + (currentManualMode ? '0' : '1'))";
  html += "  .then(response => response.text())";
  html += "  .then(() => {";
  html += "    const newMode = !currentManualMode;";
  html += "    showToast('已切换到' + (newMode ? '手动模式' : '自动模式'));";
  html += "    autoRefreshData();";
  html += "  })";
  html += "  .catch(error => {";
  html += "    console.error('模式切换失败:', error);";
  html += "    showToast('模式切换失败，请重试', 3000);";
  html += "  });";
  html += "}";
  
  html += "function controlDevice(device, state) {";
  html += "  if (!currentManualMode) return;";
  html += "  if (refreshIntervalId) clearInterval(refreshIntervalId);";
  html += "  const url = '/' + device + '?state=' + state + '&ts=' + Date.now();";
  html += "  fetch(url, { cache: 'no-store' })";
  html += "  .then(response => response.text())";
  html += "  .then(() => {";
  html += "    const deviceName = device === 'pump' ? '水泵' : device === 'fan' ? '风扇' : '照明灯';";
  html += "    showToast(deviceName + (state === 1 ? '已开启' : '已关闭'));";
  html += "    isRefreshing = false;";
  html += "    autoRefreshData();";
  html += "  })";
  html += "  .catch(error => {";
  html += "    console.error(device + '控制失败:', error);";
  html += "    showToast('设备控制失败，请重试', 3000);";
  html += "    isRefreshing = false;";
  html += "    autoRefreshData();";
  html += "  })";
  html += "  .finally(() => {";
  html += "    startAutoRefresh();";
  html += "  });";
  html += "}";
  
  // 阈值设置相关函数
  html += "function loadThresholds() {";
  html += "  fetch('/getThresholds')";
  html += "  .then(response => response.json())";
  html += "  .then(data => {";
  html += "    document.getElementById('fanTempThreshold').value = data.fanTempThreshold;";
  html += "    document.getElementById('fanCO2Threshold').value = data.fanCO2Threshold;";
  html += "    document.getElementById('pumpDroughtThreshold').value = data.pumpDroughtThreshold;";
  html += "    document.getElementById('lightLuxThreshold').value = data.lightLuxThreshold;";
  html += "  })";
  html += "  .catch(error => console.error('加载阈值失败:', error));";
  html += "}";
  
  html += "function saveThresholds() {";
  html += "  const fanTemp = document.getElementById('fanTempThreshold').value;";
  html += "  const fanCO2 = document.getElementById('fanCO2Threshold').value;";
  html += "  const pumpDrought = document.getElementById('pumpDroughtThreshold').value;";
  html += "  const lightLux = document.getElementById('lightLuxThreshold').value;";
  html += "  const params = `fanTemp=${fanTemp}&fanCO2=${fanCO2}&pumpDrought=${pumpDrought}&lightLux=${lightLux}`;";
  html += "  fetch('/setThresholds?' + params)";
  html += "  .then(response => response.text())";
  html += "  .then(result => {";
  html += "    showToast('阈值设置已保存！', 2000);";
  html += "  })";
  html += "  .catch(error => {";
  html += "    console.error('保存阈值失败:', error);";
  html += "    showToast('保存失败，请重试', 3000);";
  html += "  });";
  html += "}";
  
  html += "</script>";
  html += "</body></html>";
  
  request->send(200, "text/html", html);
}

void handleData(AsyncWebServerRequest *request) {
  StaticJsonDocument<512> doc;
  
  doc["lightIntensity"] = round(lastLightIntensity);
  doc["lightZone"] = lastLightZone;
  doc["lightDO"] = digitalRead(LIGHT_DO_PIN);
  
  doc["dhtTemp"] = lastDhtTemp;
  doc["dhtHumi"] = lastDhtHumi;
  doc["dhtStatus"] = lastDhtStatus;
  
  doc["soilAO"] = lastSoilAO;
  doc["soilStatus"] = lastSoilStatus;
  doc["soilDO"] = digitalRead(SOIL_DO_PIN);
  
  if (!sgp30Available) {
    doc["sgp30Status"] = "无数据";
    doc["eco2"] = "-";
    doc["tvoc"] = "-";
  } else if (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME) {
    // 【优化】预热期间显示标准基线值
    doc["sgp30Status"] = String("预热中（剩余") + String((SGP30_WARM_UP_TIME - (millis() - sgp30WarmUpStart))/1000) + "秒）";
    doc["eco2"] = 400;   // 显示标准基线值 400 ppm
    doc["tvoc"] = 0;
  } else {
    doc["sgp30Status"] = "正常";
    doc["eco2"] = lastEco2;
    doc["tvoc"] = lastTvoc;
  }
  
  doc["pumpState"] = pumpState;
  doc["fanState"] = fanState;
  doc["lightState"] = lightState;
  doc["manualControl"] = manualControl;
  
  // 【新增】光照进度条百分比，由后端计算避免前端硬编码
  int lightProgress = constrain((int)(lastLightIntensity / LUX_MAX * 100), 0, 100);
  doc["lightProgress"] = lightProgress;
  
  // 新增：lastUpdate字段，格式为 YYYY-MM-DD HH:MM:SS
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  char timeStr[24];
  snprintf(timeStr, sizeof(timeStr), "%04d-%02d-%02d %02d:%02d:%02d",
    timeinfo->tm_year + 1900,
    timeinfo->tm_mon + 1,
    timeinfo->tm_mday,
    timeinfo->tm_hour,
    timeinfo->tm_min,
    timeinfo->tm_sec);
  doc["lastUpdate"] = String(timeStr);

  String jsonResponse;
  serializeJson(doc, jsonResponse);

  AsyncWebServerResponse *response = request->beginResponse(200, "application/json", jsonResponse);
  response->addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
  response->addHeader("Pragma", "no-cache");
  response->addHeader("Expires", "0");
  request->send(response);
}

void handlePumpControl(AsyncWebServerRequest *request) {
  if (manualControl && request->hasArg("state")) {
    manualPumpState = (request->arg("state") == "1");
    pumpState = manualPumpState;
    controlPump(pumpState);  // 使用非阻塞延迟启动
  }
  request->send(200, "text/plain", String("水泵状态：") + String(pumpState ? "开启" : "关闭"));
}

void handleFanControl(AsyncWebServerRequest *request) {
  if (manualControl && request->hasArg("state")) {
    manualFanState = (request->arg("state") == "1");
    fanState = manualFanState;
    controlFan(fanState);  // 使用非阻塞延迟启动
  }
  request->send(200, "text/plain", String("风扇状态：") + String(fanState ? "开启" : "关闭"));
}

// 【修复】手动灯控制逻辑 - 只在手动模式下才允许控制
void handleLightControl(AsyncWebServerRequest *request) {
  if (request->hasArg("state")) {
    bool newState = (request->arg("state") == "1");
    // 只有在手动模式下才允许手动控制灯
    if (manualControl) {
      manualLightState = newState;
      lightState = newState;
      digitalWrite(LIGHT_LAMP_PIN, lightState ? HIGH : LOW);
    }
    // 自动模式下忽略手动灯控制请求，由光照阈值自动控制
  }
  request->send(200, "text/plain", String("照明灯状态：") + String(lightState ? "开启" : "关闭"));
}

void handleModeControl(AsyncWebServerRequest *request) {
  if (request->hasArg("state")) {
    manualControl = (request->arg("state") == "1");
    if (!manualControl) {
      bool newPumpState = (lastSoilAO > pumpDroughtThreshold);
      bool newFanState = (lastDhtTemp > fanTempThreshold || lastEco2 > fanCO2Threshold);
      lightState = (lastLightIntensity < lightLuxThreshold);
      
      // 只有状态变化时才控制
      if (newPumpState != pumpState) {
        pumpState = newPumpState;
        controlPump(pumpState);
      }
      if (newFanState != fanState) {
        fanState = newFanState;
        controlFan(fanState);
      }
      digitalWrite(LIGHT_LAMP_PIN, lightState);
      manualPumpState = pumpState;
      manualFanState = fanState;
      manualLightState = lightState;
    }
  }
  request->send(200, "text/plain", String("模式切换成功：") + String(manualControl ? "手动" : "自动"));
}

// ==============================================================================
// 阈值设置处理函数
// ==============================================================================
void handleSetThresholds(AsyncWebServerRequest *request) {
  if (request->hasArg("fanTemp")) {
    fanTempThreshold = request->arg("fanTemp").toInt();
    fanTempThreshold = constrain(fanTempThreshold, 20, 50);
  }
  if (request->hasArg("fanCO2")) {
    fanCO2Threshold = request->arg("fanCO2").toInt();
    fanCO2Threshold = constrain(fanCO2Threshold, 400, 5000);
  }
  if (request->hasArg("pumpDrought")) {
    pumpDroughtThreshold = request->arg("pumpDrought").toInt();
    pumpDroughtThreshold = constrain(pumpDroughtThreshold, 0, 5000);  // 范围0-5000
  }
  if (request->hasArg("lightLux")) {
    lightLuxThreshold = request->arg("lightLux").toInt();
    lightLuxThreshold = constrain(lightLuxThreshold, 50, 5000);
  }
  
  // 【新增】持久化保存阈值到Flash
  preferences.putInt("fanTemp", fanTempThreshold);
  preferences.putInt("fanCO2", fanCO2Threshold);
  preferences.putInt("pumpDrought", pumpDroughtThreshold);
  preferences.putInt("lightLux", lightLuxThreshold);
  
  Serial.println("阈值设置已更新并保存:");
  Serial.print("  风扇温度阈值: "); Serial.println(fanTempThreshold);
  Serial.print("  风扇CO2阈值: "); Serial.println(fanCO2Threshold);
  Serial.print("  水泵干旱阈值: "); Serial.println(pumpDroughtThreshold);
  Serial.print("  灯泡光照阈值(lux): "); Serial.println(lightLuxThreshold);
  
  request->send(200, "text/plain", "阈值设置成功（已持久化保存）");
}

void handleGetThresholds(AsyncWebServerRequest *request) {
  StaticJsonDocument<256> doc;
  doc["fanTempThreshold"] = fanTempThreshold;
  doc["fanCO2Threshold"] = fanCO2Threshold;
  doc["pumpDroughtThreshold"] = pumpDroughtThreshold;
  doc["lightLuxThreshold"] = lightLuxThreshold;
  
  String jsonResponse;
  serializeJson(doc, jsonResponse);
  request->send(200, "application/json", jsonResponse);
}

void handleNotFound(AsyncWebServerRequest *request) {
  String html = "<!DOCTYPE html><html lang='zh-CN'>";
  html += "<head><meta charset='UTF-8'><title>404 Not Found</title></head>";
  html += "<body style='text-align:center; padding-top:50px; font-family:Arial;'>";
  html += "<h1>404 - 页面未找到</h1>";
  html += "<p>您访问的页面不存在！</p>";
  html += "<p><a href='/'>返回首页</a></p>";
  html += "</body></html>";
  request->send(404, "text/html", html);
}

void configModeCallback (WiFiManager *myWiFiManager) {
  Serial.println("进入配网模式");
  Serial.println("SSID: ESP32-Sensor-AP");
  Serial.println("密码: 无（开放AP）");
  Serial.println("请连接此WiFi后，在浏览器访问 192.168.4.1");
}

// ============================================================================
// 【新增】生成时间戳函数 - 返回当前小时的yyyyMMddHH格式字符串
// ============================================================================
String generateTimestamp() {
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  char timestamp[16];
  // 格式：yyyyMMddHH（年月日小时）
  snprintf(timestamp, sizeof(timestamp), "%04d%02d%02d%02d",
           timeinfo->tm_year + 1900,
           timeinfo->tm_mon + 1,
           timeinfo->tm_mday,
           timeinfo->tm_hour);
  return String(timestamp);
}

// ============================================================================
// 【新增】HMAC-SHA256签名生成MQTT密码
// 华为云IoTDA正确公式：password = hex(HMAC-SHA256(timestamp, secret))
// 注意：timestamp作为HMAC密钥，secret作为消息！
// ============================================================================
String generateMqttPassword(const char* secret, const String& timestamp) {
  // HMAC-SHA256输出32字节
  byte hmacResult[32];
  
  // 使用mbedtls进行HMAC-SHA256计算
  // 【关键修复】华为云IoTDA要求：timestamp作为key，secret作为message
  mbedtls_md_context_t ctx;
  mbedtls_md_type_t md_type = MBEDTLS_MD_SHA256;
  
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(md_type), 1); // 1表示HMAC
  // 【修复】timestamp作为HMAC密钥，secret作为消息
  mbedtls_md_hmac_starts(&ctx, (const unsigned char*)timestamp.c_str(), timestamp.length());
  mbedtls_md_hmac_update(&ctx, (const unsigned char*)secret, strlen(secret));
  mbedtls_md_hmac_finish(&ctx, hmacResult);
  mbedtls_md_free(&ctx);
  
  // 转换为小写十六进制字符串
  String hexPassword = "";
  for (int i = 0; i < 32; i++) {
    char hex[3];
    snprintf(hex, sizeof(hex), "%02x", hmacResult[i]);
    hexPassword += hex;
  }
  
  return hexPassword;
}

// ============================================================================
// MQTT回调与工具函数
// ============================================================================
void handleRemoteProperties(const JsonVariant &properties) {
  if (properties.isNull()) return;

  if (properties.containsKey("manual")) {
    manualControl = properties["manual"].as<bool>();
    Serial.printf("远程设置手动模式: %s\n", manualControl ? "开启" : "关闭");
  }

  if (properties.containsKey("pump")) {
    bool target = properties["pump"].as<bool>();
    manualControl = true;
    manualPumpState = target;
    pumpState = target;
    controlPump(pumpState);
    Serial.printf("远程控制水泵: %s\n", target ? "开启" : "关闭");
  }

  if (properties.containsKey("fan")) {
    bool target = properties["fan"].as<bool>();
    manualControl = true;
    manualFanState = target;
    fanState = target;
    controlFan(fanState);
    Serial.printf("远程控制风扇: %s\n", target ? "开启" : "关闭");
  }

  if (properties.containsKey("light")) {
    bool target = properties["light"].as<bool>();
    manualControl = true;
    manualLightState = target;
    lightState = target;
    digitalWrite(LIGHT_LAMP_PIN, lightState ? HIGH : LOW);
    Serial.printf("远程控制照明: %s\n", target ? "开启" : "关闭");
  }

  auto updateThreshold = [&](const char *key, int &target, int minV, int maxV) {
    if (!properties.containsKey(key)) return;
    int val = properties[key].as<int>();
    val = constrain(val, minV, maxV);
    target = val;
    preferences.putInt(key, target);
    Serial.printf("远程阈值更新 %s = %d\n", key, target);
  };

  updateThreshold("lightLux", lightLuxThreshold, 50, 5000);
  updateThreshold("pumpDrought", pumpDroughtThreshold, 0, 5000);
  updateThreshold("fanTemp", fanTempThreshold, 20, 50);
  updateThreshold("fanCO2", fanCO2Threshold, 400, 5000);
}

void mqttCallback(char *topic, byte *payload, unsigned int length) {
  String topicStr(topic);
  String body;
  body.reserve(length + 1);
  for (unsigned int i = 0; i < length; i++) {
    body += static_cast<char>(payload[i]);
  }

  Serial.println("[MQTT] 收到消息: " + topicStr);
  Serial.println(body);

  StaticJsonDocument<512> doc;
  DeserializationError err = deserializeJson(doc, body);
  if (err) {
    Serial.printf("JSON解析失败: %s\n", err.c_str());
    return;
  }

  if (topicStr.indexOf("/sys/properties/set") >= 0) {
    JsonArray services = doc["services"].as<JsonArray>();
    if (!services.isNull()) {
      for (size_t i = 0; i < services.size(); i++) {
        JsonObject svc = services[i].as<JsonObject>();
        if (!svc.isNull()) handleRemoteProperties(svc["properties"]);
      }
    }
    return;
  }

  if (topicStr.indexOf("/sys/commands") >= 0) {
    handleRemoteProperties(doc["paras"]);
    return;
  }
}

void ensureMqttConnection() {
  if (!WiFi.isConnected()) return;
  if (mqttClient.connected()) return;

  unsigned long now = millis();
  if (now - lastMqttReconnect < mqttReconnectInterval) return;
  lastMqttReconnect = now;

  // 【修改】动态生成clientId和password
  // 1. 获取当前小时时间戳（yyyyMMddHH格式）
  String timestamp = generateTimestamp();
  
  // 2. 构造clientId = deviceId + "_0_0_" + timestamp
  String dynamicClientId = String(mqttDeviceId) + "_0_0_" + timestamp;
  
  // 3. 计算password = hex(HMAC-SHA256(secret, timestamp))
  String dynamicPassword = generateMqttPassword(deviceSecret, timestamp);
  
  // 【调试】打印生成的clientId和password
  Serial.println("[MQTT] ====== 动态凭证生成 ======");
  Serial.println("[MQTT] 时间戳: " + timestamp);
  Serial.println("[MQTT] ClientId: " + dynamicClientId);
  Serial.println("[MQTT] Password: " + dynamicPassword);
  Serial.println("[MQTT] Username: " + String(mqttUsername));
  Serial.println("[MQTT] Host: " + String(mqttHost));
  Serial.println("[MQTT] Port: " + String(mqttPort));
  Serial.println("[MQTT] ============================");

  Serial.println("[MQTT] 正在连接IoTDA MQTT Broker...");
  
  // 【修改】使用动态生成的clientId和password进行连接
  if (mqttClient.connect(dynamicClientId.c_str(), mqttUsername, dynamicPassword.c_str())) {
    mqttClient.subscribe(mqttTopicCommand.c_str());
    mqttClient.subscribe(mqttTopicPropSet.c_str());
    Serial.println("[MQTT] ★★★ 连接成功！已订阅命令/属性下发 ★★★");
  } else {
    int state = mqttClient.state();
    Serial.printf("[MQTT] 连接失败, state=%d\n", state);
    // 打印详细错误信息
    switch(state) {
      case -4: Serial.println("[MQTT] 错误: 连接超时 - 检查网络和防火墙"); break;
      case -3: Serial.println("[MQTT] 错误: 连接丢失"); break;
      case -2: Serial.println("[MQTT] 错误: 连接失败 - TLS握手可能失败"); break;
      case -1: Serial.println("[MQTT] 错误: 断开连接"); break;
      case 1:  Serial.println("[MQTT] 错误: 协议版本不匹配"); break;
      case 2:  Serial.println("[MQTT] 错误: ClientId被拒绝 - 检查设备是否在IoTDA注册"); break;
      case 3:  Serial.println("[MQTT] 错误: 服务器不可用"); break;
      case 4:  Serial.println("[MQTT] 错误: 用户名或密码错误 - 检查HMAC签名"); break;
      case 5:  Serial.println("[MQTT] 错误: 未授权"); break;
      default: Serial.println("[MQTT] 错误: 未知错误"); break;
    }
  }
}

void publishTelemetry() {
  if (!mqttClient.connected()) return;

  StaticJsonDocument<512> doc;
  JsonArray services = doc.createNestedArray("services");
  JsonObject svc = services.createNestedObject();
  svc["service_id"] = "default";
  JsonObject props = svc.createNestedObject("properties");

  props["temp"] = lastDhtTemp;
  props["humi"] = lastDhtHumi;
  props["soil"] = lastSoilAO;
  props["lightLux"] = static_cast<int>(lastLightIntensity);
  props["eco2"] = lastEco2;
  props["tvoc"] = lastTvoc;
  props["pump"] = pumpState;
  props["fan"] = fanState;
  props["light"] = lightState;
  props["manual"] = manualControl;
  props["lightLuxThreshold"] = lightLuxThreshold;
  props["pumpDroughtThreshold"] = pumpDroughtThreshold;
  props["fanTempThreshold"] = fanTempThreshold;
  props["fanCO2Threshold"] = fanCO2Threshold;

  String payload;
  serializeJson(doc, payload);

  if (mqttClient.publish(mqttTopicReport.c_str(), payload.c_str())) {
    Serial.println("[MQTT] 遥测上报成功");
  } else {
    Serial.println("[MQTT] 遥测上报失败");
  }
}

// ==============================================================================
// 初始化函数
// ==============================================================================
void setup() {

  Serial.begin(9600);
  Wire.begin();
  Wire.setClock(100000);

  pinMode(LIGHT_AO_PIN, INPUT);
  pinMode(LIGHT_DO_PIN, INPUT);
  pinMode(SOIL_AO_PIN, INPUT);
  pinMode(SOIL_DO_PIN, INPUT);

  pinMode(PUMP_PIN, OUTPUT);
  pinMode(PUMP_PIN2, OUTPUT);
  pinMode(FAN_PIN, OUTPUT);
  pinMode(FAN_PIN2, OUTPUT);
  pinMode(LIGHT_LAMP_PIN, OUTPUT);
  digitalWrite(PUMP_PIN, LOW);
  digitalWrite(PUMP_PIN2, LOW);
  digitalWrite(FAN_PIN, LOW);
  digitalWrite(FAN_PIN2, LOW);
  digitalWrite(LIGHT_LAMP_PIN, LOW);

  u8g2.begin();
  u8g2.enableUTF8Print();

  // 【新增】从Flash加载持久化的阈值设置
  preferences.begin("thresholds", false);
  fanTempThreshold = preferences.getInt("fanTemp", 30);           // 默认30℃
  fanCO2Threshold = preferences.getInt("fanCO2", 1000);          // 默认1000ppm
  pumpDroughtThreshold = preferences.getInt("pumpDrought", 3200); // 默认3200
  lightLuxThreshold = preferences.getInt("lightLux", 800);        // 默认800lux
  Serial.println("已从Flash加载阈值设置:");
  Serial.print("  风扇温度阈值: "); Serial.println(fanTempThreshold);
  Serial.print("  风扇CO2阈值: "); Serial.println(fanCO2Threshold);
  Serial.print("  水泵干旱阈值: "); Serial.println(pumpDroughtThreshold);
  Serial.print("  灯泡光照阈值(lux): "); Serial.println(lightLuxThreshold);

  i2cScan();

  if (sgp30Available) {
    sgp30Available = sgp30Init();
  }

  wm.setAPCallback(configModeCallback);
  wm.setConfigPortalTimeout(180);
  wm.setMinimumSignalQuality(20);
  wm.setBreakAfterConfig(true);
  
  // 设置静态IP地址（仅当useStaticIP为true时生效）
  if (useStaticIP) {
    wm.setSTAStaticIPConfig(staticIP, gateway, subnet);
    Serial.println("将使用固定IP地址: " + staticIP.toString());
  } else {
    Serial.println("将使用DHCP自动获取IP地址");
  }

  Serial.println("正在连接WiFi...");
  if (!wm.autoConnect("ESP32-Sensor-AP")) {
    Serial.println("WiFi配网超时/失败，即将重启设备...");
    delay(3000);
    ESP.restart();
  }

  Serial.println("====================================");
  Serial.println("WiFi连接成功！");
  Serial.print("设备IP地址: ");
  Serial.println(WiFi.localIP());
  Serial.print("网关地址: ");
  Serial.println(WiFi.gatewayIP());
  Serial.print("子网掩码: ");
  Serial.println(WiFi.subnetMask());
  Serial.print("WiFi信号强度: ");
  Serial.print(WiFi.RSSI());
  Serial.println(" dBm");
  Serial.println("------------------------------------");
  Serial.println("【网页访问地址】");
  Serial.print("http://");
  Serial.println(WiFi.localIP());
  Serial.println("====================================");

  // NTP时间同步，设置中国时区
  // 【修改】加强NTP同步，确保时间准确（华为云IoTDA对时间戳有严格要求）
  configTime(8 * 3600, 0, "ntp.aliyun.com", "ntp1.aliyun.com", "pool.ntp.org");
  Serial.println("正在同步网络时间（华为云IoTDA需要准确时间）...");
  int retry = 0;
  const int maxRetry = 40;  // 【修改】增加重试次数，最多等待20秒
  time_t now = time(nullptr);
  while ((now < 1672531200) && (retry < maxRetry)) { // 2023-01-01 00:00:00
    delay(500);
    now = time(nullptr);
    retry++;
    if (retry % 4 == 0) {
      Serial.print(".");
    }
  }
  Serial.println();
  if (now < 1672531200) {
    Serial.println("【警告】NTP时间同步失败！MQTT连接可能会失败！");
  } else {
    struct tm* timeinfo = localtime(&now);
    Serial.printf("当前时间: %04d-%02d-%02d %02d:%02d:%02d\n", timeinfo->tm_year+1900, timeinfo->tm_mon+1, timeinfo->tm_mday, timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec);
    Serial.println("NTP时间同步成功！");
  }

  // 【修改】设置MQTT超时和缓冲区
  mqttSecureClient.setInsecure();  // 跳过证书验证
  mqttSecureClient.setTimeout(15); // 设置15秒超时
  mqttClient.setServer(mqttHost, mqttPort);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setBufferSize(1024);
  mqttClient.setSocketTimeout(15);  // 【新增】设置socket超时
  mqttClient.setKeepAlive(120);     // 【新增】设置心跳间隔120秒
  
  // 延迟一下再连接MQTT，确保网络稳定
  delay(1000);
  ensureMqttConnection();

  server.on("/", HTTP_GET, handleRoot);
  server.on("/data", HTTP_GET, handleData);
  server.on("/pump", HTTP_GET, handlePumpControl);
  server.on("/fan", HTTP_GET, handleFanControl);
  server.on("/light", HTTP_GET, handleLightControl);
  server.on("/mode", HTTP_GET, handleModeControl);
  server.on("/setThresholds", HTTP_GET, handleSetThresholds);
  server.on("/getThresholds", HTTP_GET, handleGetThresholds);
  server.onNotFound(handleNotFound);

  server.begin();
  Serial.println("异步WebServer已启动，端口: 80");

  Serial.println("系统初始化完成！");
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_wqy12_t_gb2312);
  u8g2.setCursor(10, 32);
  u8g2.print("系统启动成功");
  u8g2.setCursor(10, 48);
  u8g2.print("IP: ");
  u8g2.print(WiFi.localIP().toString());
  u8g2.sendBuffer();
  delay(2000);
}

// ==============================================================================
// 主循环函数
// ==============================================================================
void loop() {
  static unsigned long lastReadTime = 0;
  unsigned long currentTime = millis();

  ensureMqttConnection();
  mqttClient.loop();

  if (mqttClient.connected() && currentTime - lastMqttPublish >= mqttPublishInterval) {
    lastMqttPublish = currentTime;
    publishTelemetry();
  }

  // 处理水泵/风扇的延迟启动（非阻塞）
  handlePendingStarts();

  if (currentTime - lastReadTime >= READ_INTERVAL) {
    lastReadTime = currentTime;

    int lightAO = analogRead(LIGHT_AO_PIN);
    if (lightAO != lastLightLevel) {
      lastLightLevel = lightAO;
      calculateLightIntensity(lightAO, lastLightIntensity, lastLightZone);
    }

    dht11.readData();
    if (dht11.isReadSuccess()) {
      lastDhtTemp = dht11.getTemperature();
      lastDhtHumi = dht11.getHumidity();
      updateDhtStatus(lastDhtTemp, lastDhtHumi, lastDhtStatus);
    }

    int soilAO = analogRead(SOIL_AO_PIN);
    if (soilAO != lastSoilAO) {
            lastSoilAO = soilAO;
      updateSoilStatus(soilAO, lastSoilStatus);
    }

    // 读取SGP30数据
    uint16_t eco2 = 0, tvoc = 0;
    sgp30ReadData(eco2, tvoc);
    lastEco2 = eco2;
    lastTvoc = tvoc;

    // 自动控制逻辑（仅在非手动模式下生效）
    if (!manualControl) {
      // 水泵控制：土壤ADC值超过阈值时开启（ADC值越大越干燥）
      bool newPumpState = (lastSoilAO > pumpDroughtThreshold);
      if (newPumpState != pumpState) {
        pumpState = newPumpState;
        controlPump(pumpState);  // 使用非阻塞延迟启动
      }

      // 风扇控制：温度超过阈值 或 eCO2超过阈值时开启
      bool newFanState = (lastDhtTemp > fanTempThreshold) || (lastEco2 > fanCO2Threshold && sgp30Available && (millis() - sgp30WarmUpStart >= SGP30_WARM_UP_TIME));
      if (newFanState != fanState) {
        fanState = newFanState;
        controlFan(fanState);  // 使用非阻塞延迟启动
      }

      // 照明灯控制：光照强度(lux)低于阈值时开启（越亮lux越大）
      bool newLightState = (lastLightIntensity < lightLuxThreshold);
      if (newLightState != lightState) {
        lightState = newLightState;
        digitalWrite(LIGHT_LAMP_PIN, lightState ? HIGH : LOW);
      }
    }
    // 手动模式下不需要每次循环都写GPIO，只在状态变化时通过handleXXXControl函数处理

    // 更新OLED显示
    updateOLED(lastLightIntensity, lastLightZone, lastDhtTemp, lastDhtHumi, lastSoilAO, lastSoilStatus, lastEco2, lastTvoc);

    // 定期串口打印数据
    if (currentTime - lastSerialPrintTime >= SERIAL_PRINT_INTERVAL) {
      lastSerialPrintTime = currentTime;
      Serial.println("\n====================================");
      Serial.println(String("传感器数据更新 - ") + String(millis()/1000) + "秒");
      Serial.println("====================================");
      Serial.print("光照强度: ");
      Serial.print(lastLightIntensity, 0);
      Serial.print(" lux (");
      Serial.print(lastLightZone);
      Serial.println(")");
      Serial.print("光照AO值: ");
      Serial.println(lastLightLevel);
      Serial.print("光照DO值: ");
      Serial.println(digitalRead(LIGHT_DO_PIN));
      
      Serial.print("温度: ");
      Serial.print(lastDhtTemp);
      Serial.println(" ℃");
      Serial.print("湿度: ");
      Serial.print(lastDhtHumi);
      Serial.println(" %RH");
      Serial.print("温湿度状态: ");
      Serial.println(lastDhtStatus);
      
      Serial.print("土壤湿度AO值: ");
      Serial.println(lastSoilAO);
      Serial.print("土壤湿度状态: ");
      Serial.println(lastSoilStatus);
      Serial.print("土壤DO值: ");
      Serial.println(digitalRead(SOIL_DO_PIN));
      
      if (sgp30Available) {
        if (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME) {
          Serial.println(String("SGP30: 预热中（剩余") + String((SGP30_WARM_UP_TIME - (millis() - sgp30WarmUpStart))/1000) + "秒）");
        } else {
          Serial.print("eCO₂: ");
          Serial.print(lastEco2);
          Serial.println(" ppm");
          Serial.print("TVOC: ");
          Serial.print(lastTvoc);
          Serial.println(" ppb");
        }
      } else {
        Serial.println("SGP30: 未连接/初始化失败");
      }
      
      Serial.println("------------------------------------");
      Serial.print("系统模式: ");
      Serial.println(manualControl ? "手动控制" : "自动控制");
      Serial.print("水泵状态: ");
      Serial.println(pumpState ? "开启" : "关闭");
      Serial.print("风扇状态: ");
      Serial.println(fanState ? "开启" : "关闭");
      Serial.print("照明灯状态: ");
      Serial.println(lightState ? "开启" : "关闭");
      Serial.println("====================================\n");
    }
  }

  // 处理WiFiManager循环
  wm.process();
}
