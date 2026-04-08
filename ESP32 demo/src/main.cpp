#include <Arduino.h>
#include <Wire.h>
#include <U8g2lib.h>
#include <WiFi.h>
#include <WiFiManager.h>
#include <ESPAsyncWebServer.h>
#include <ArduinoJson.h>
// #include <WiFiClientSecure.h>   // MQTT已禁用
// #include <PubSubClient.h>        // MQTT已禁用
#include <time.h>        // 【新增】NTP时间函数所需头文件
#include <Preferences.h> // 【新增】阈值持久化存储
// #include <mbedtls/md.h>  // MQTT已禁用（HMAC签名）
#include <SD.h>          // 【新增】SD卡读写
#include <SPI.h>         // 【新增】SPI总线（SD卡通信）

// ==============================================================================
// OLED屏幕配置
// ==============================================================================
U8G2_SSD1315_128X64_NONAME_F_HW_I2C u8g2(U8G2_R2, U8X8_PIN_NONE);

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

/* ============================================================================
// 华为 IoTDA MQTT 配置（已禁用）
// ============================================================================
const char *mqttHost = "0c303a8ecf.st1.iotda-device.cn-south-1.myhuaweicloud.com";
const uint16_t mqttPort = 8883;
const char *mqttDeviceId = "69568516c00ccb6d4b302187_esp32-001";
const char *mqttUsername = mqttDeviceId;
const char *deviceSecret = "Lzg551162";

WiFiClientSecure mqttSecureClient;
PubSubClient mqttClient(mqttSecureClient);

String mqttTopicReport = String("$oc/devices/") + mqttDeviceId + "/sys/properties/report";
String mqttTopicCommand = String("$oc/devices/") + mqttDeviceId + "/sys/commands/#";
String mqttTopicPropSet = String("$oc/devices/") + mqttDeviceId + "/sys/properties/set/#";

unsigned long lastMqttReconnect = 0;
const unsigned long mqttReconnectInterval = 30000;
unsigned long lastMqttPublish = 0;
const unsigned long mqttPublishInterval = 10000;
*/

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
#define SD_CS_PIN 5       // SD卡片选引脚（VSPI默认）
#define SGP30_ADDR 0x58
#define SGP30_INIT_RETRY 3
#define SGP30_WARM_UP_TIME 10000

// ==============================================================================
// 传感器参数配置
// ==============================================================================
#define READ_INTERVAL 1000
#define SERIAL_PRINT_INTERVAL 20000
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
// 土壤传感器滤波参数（修复ADC读取不稳定问题）
// ==============================================================================
#define SOIL_SAMPLES 10  // 每次采样的次数，用于平均值滤波
#define SOIL_FILTER_ALPHA 0.3  // 一阶低通滤波系数（0-1，值越小滤波越强）

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
float filteredSoilAO = -1;  // 【修复】存储滤波后的土壤ADC值
String lastSoilStatus = "";
bool soilSensorError = false;  // 【新增】土壤传感器故障标志
unsigned long soilErrorTime = 0;  // 【新增】记录故障时间
int errorCountsAO = 0;  // 【新增】连续异常计数（防止假报警）
#define ERROR_THRESHOLD 3  // 【新增】连续3次异常判定为故障
bool pumpState = false;
bool fanState = false;
bool lightState = false;
uint16_t lastEco2 = 0;
uint16_t lastTvoc = 0;
bool sgp30Available = false;
unsigned long sgp30WarmUpStart = 0;
unsigned long lastSerialPrintTime = 0;

// ==============================================================================
// 【新增】SD卡离线缓存相关变量
// ==============================================================================
bool sdCardAvailable = false;           // SD卡是否可用
volatile bool sdCachePending = false;   // 是否存在待取走的缓存数据（避免网页轮询时频繁访问SD）
SPIClass spiSD(VSPI);                   // SD卡专用SPI实例（必须全局，否则函数返回后被销毁导致崩溃）
const char* SD_CACHE_FILE = "/cache.jsonl"; // 缓存文件路径（JSON Lines格式）
unsigned long lastCacheWrite = 0;       // 上次缓存写入时间
unsigned long cacheWriteInterval = 10000; // 缓存写入间隔（默认10秒，可通过网页设置）
volatile unsigned long lastWebPoll = 0;  // 上次网页端轮询/data的时间（volatile：跨核访问）
const unsigned long webDisconnectTimeout = 60000; // 超过60秒没轮询才视为断连
volatile bool webClientConnected = false; // 网页端是否在线（volatile：跨核访问）

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
// MQTT相关函数声明（已禁用）
// void mqttCallback(char *topic, byte *payload, unsigned int length);
// void ensureMqttConnection();
// void publishTelemetry();
// String generateTimestamp();
// String generateMqttPassword(const char* secret, const String& timestamp);
// void handleRemoteProperties(const JsonVariant &properties);
void i2cScan();
bool sgp30Init();
void sgp30ReadData(uint16_t &eco2, uint16_t &tvoc);
void updateOLED(float lightLux, const String &lightZone, int temp, int humi, int soilAO, const String &soilStatus, uint16_t eco2, uint16_t tvoc);
void calculateLightIntensity(int aoValue, float &lux, String &zone);
void updateSoilStatus(int aoValue, String &status);
void updateDhtStatus(int temp, int humi, String &status);
void controlPump(bool enable);
void controlFan(bool enable);
void handlePendingStarts();
int readSoilSensorRaw();
float filterSoilAO(float rawValue);
void checkSoilSensorHealth(int rawAO, int soilDO);
bool initSDCard();                    // 【新增】SD卡初始化
void cacheDataToSD();                 // 【新增】将传感器数据缓存到SD卡
void handleCachedData(AsyncWebServerRequest *request); // 【新增】HTTP接口返回缓存数据
bool hasSDCachedData();               // 【新增】检查SD卡是否有缓存数据
void handleSetCacheInterval(AsyncWebServerRequest *request); // 【新增】设置缓存间隔
void handleGetCacheInterval(AsyncWebServerRequest *request); // 【新增】获取缓存间隔

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
void updateOLED(float lightLux, const String &lightZone, int temp, int humi, int soilAO, const String &soilStatus, uint16_t eco2, uint16_t tvoc) {
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
// 【修复】土壤传感器原始值读取函数 - 多采样平均
// ==============================================================================
int readSoilSensorRaw() {
  long sum = 0;
  for (int i = 0; i < SOIL_SAMPLES; i++) {
    sum += analogRead(SOIL_AO_PIN);
    delayMicroseconds(100);  // 采样间隔
  }
  return (int)(sum / SOIL_SAMPLES);  // 返回平均值
}

// ==============================================================================
// 【修复】土壤传感器滤波函数 - 一阶低通滤波
// ==============================================================================
float filterSoilAO(float rawValue) {
  if (filteredSoilAO < 0) {
    // 首次读取，初始化滤波值
    filteredSoilAO = rawValue;
  } else {
    // 一阶低通滤波：y = α*x + (1-α)*y_prev
    filteredSoilAO = SOIL_FILTER_ALPHA * rawValue + (1.0 - SOIL_FILTER_ALPHA) * filteredSoilAO;
  }
  return filteredSoilAO;
}

// ==============================================================================
// 【新增】土壤传感器故障检测函数 - 连续异常判定
// ==============================================================================
void checkSoilSensorHealth(int rawAO, int doPin) {
  // 判断AO值是否异常（悬空或接触不良）
  bool aoAbnormal = (rawAO >= 4090 || rawAO <= 10);

  // 判断DO值是否合理（DO应该和AO值匹配：干旱时DO=0，潮湿时DO=1）
  bool doAbnormal = false;
  if (!soilSensorError) {  // 只在非故障状态下检查DO-AO配合
    // 如果AO显示很干（>3500）但DO=1（数字触发），可能接线问题
    if (rawAO > 3500 && doPin == 1) doAbnormal = true;
  }

  // 计算异常计数
  if (aoAbnormal || doAbnormal) {
    errorCountsAO++;
    if (errorCountsAO >= ERROR_THRESHOLD) {
      if (!soilSensorError) {  // 从正常→故障
        soilSensorError = true;
        soilErrorTime = millis();
        filteredSoilAO = -1;  // 清除滤波值，强制重新初始化
        Serial.println("\n【严重警告】土壤传感器故障检测-连续异常!");
        if (aoAbnormal) {
          if (rawAO >= 4090) {
            Serial.println("  AO值过高(≈4095) → 检查GPIO35接线或AO标签");
          } else {
            Serial.println("  AO值过低(≈0) → 检查GND连接");
          }
        }
        if (doAbnormal) {
          Serial.println("  DO值与AO值不匹配 → 检查DO接线或传感器阈值设置");
        }
      }
    }
  } else {
    // 正常读数，计数清零
    if (errorCountsAO > 0) {
      errorCountsAO = 0;
    }
    if (soilSensorError) {  // 从故障→正常
      Serial.println("【恢复】土壤传感器已恢复正常!");
    }
    soilSensorError = false;
  }
}

// ==============================================================================
// 【原有】土壤湿度状态更新函数
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
  AsyncResponseStream *response = request->beginResponseStream("text/html");
  // 使用流式输出（print），避免构建巨大String导致内存耗尽和设备重启
  #define P(s) response->print(s)
  P("<!DOCTYPE html><html lang='zh-CN'>");
  P("<head>");
  P("<meta charset='UTF-8'>");
  P("<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0'>");
  P("<title>ESP32智能环境监控系统</title>");
  P("<link rel='stylesheet' href='https://cdn.bootcdn.net/ajax/libs/font-awesome/6.4.0/css/all.min.css'>");
  P("<style>");
  P(":root {");
  P("  --color-primary: #2563eb;");
  P("  --color-success: #10b981;");
  P("  --color-warning: #f59e0b;");
  P("  --color-danger: #ef4444;");
  P("  --color-info: #3b82f6;");
  P("  --color-dark: #1f2937;");
  P("  --color-light: #f3f4f6;");
  P("  --color-gray: #6b7280;");
  P("  --shadow-sm: 0 2px 4px rgba(0,0,0,0.05);");
  P("  --shadow-md: 0 4px 6px rgba(0,0,0,0.1);");
  P("  --shadow-lg: 0 10px 15px rgba(0,0,0,0.1);");
  P("  --radius-sm: 6px;");
  P("  --radius-md: 12px;");
  P("  --radius-lg: 16px;");
  P("  --transition: all 0.2s ease;");
  P("}");
  P("* { margin: 0; padding: 0; box-sizing: border-box; }");
  P("body {");
  P("  font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;");
  P("  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);");
  P("  min-height: 100vh;");
  P("  color: var(--color-dark);");
  P("  padding-bottom: 20px;");
  P("}");
  P(".navbar {");
  P("  background: white;");
  P("  box-shadow: var(--shadow-sm);");
  P("  padding: 15px 20px;");
  P("  position: sticky;");
  P("  top: 0;");
  P("  z-index: 100;");
  P("  display: flex;");
  P("  justify-content: space-between;");
  P("  align-items: center;");
  P("}");
  P(".navbar-title {");
  P("  font-size: 18px;");
  P("  font-weight: 600;");
  P("  color: var(--color-primary);");
  P("}");
  P(".status-bar {");
  P("  display: flex;");
  P("  align-items: center;");
  P("  gap: 10px;");
  P("}");
  P(".status-indicator {");
  P("  display: flex;");
  P("  align-items: center;");
  P("  font-size: 14px;");
  P("}");
  P(".status-dot {");
  P("  width: 10px;");
  P("  height: 10px;");
  P("  border-radius: 50%;");
  P("  margin-right: 5px;");
  P("  animation: pulse 2s infinite;");
  P("}");
  P(".status-online { background: var(--color-success); }");
  P(".status-offline { background: var(--color-danger); }");
  P(".mode-switch {");
  P("  padding: 8px 16px;");
  P("  border-radius: var(--radius-sm);");
  P("  border: none;");
  P("  background: var(--color-primary);");
  P("  color: white;");
  P("  font-size: 14px;");
  P("  cursor: pointer;");
  P("  transition: var(--transition);");
  P("}");
  P(".mode-switch:hover { opacity: 0.9; }");
  P(".mode-auto { background: var(--color-warning); }");
  P(".container {");
  P("  max-width: 1200px;");
  P("  margin: 20px auto;");
  P("  padding: 0 20px;");
  P("}");
  P(".overview-grid {");
  P("  display: grid;");
  P("  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));");
  P("  gap: 15px;");
  P("  margin-bottom: 25px;");
  P("}");
  P(".card {");
  P("  background: white;");
  P("  border-radius: var(--radius-md);");
  P("  box-shadow: var(--shadow-sm);");
  P("  padding: 20px;");
  P("  transition: var(--transition);");
  P("}");
  P(".card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }");
  P(".card-header {");
  P("  display: flex;");
  P("  justify-content: space-between;");
  P("  align-items: center;");
  P("  margin-bottom: 15px;");
  P("  padding-bottom: 10px;");
  P("  border-bottom: 1px solid var(--color-light);");
  P("}");
  P(".card-title {");
  P("  font-size: 16px;");
  P("  font-weight: 600;");
  P("  color: var(--color-dark);");
  P("}");
  P(".card-icon {");
  P("  width: 32px;");
  P("  height: 32px;");
  P("  border-radius: 50%;");
  P("  background: var(--color-light);");
  P("  display: flex;");
  P("  align-items: center;");
  P("  justify-content: center;");
  P("  color: var(--color-primary);");
  P("}");
  P(".data-value {");
  P("  font-size: 24px;");
  P("  font-weight: 700;");
  P("  margin-bottom: 10px;");
  P("}");
  P(".data-unit { font-size: 14px; color: var(--color-gray); }");
  P(".data-status {");
  P("  font-size: 14px;");
  P("  padding: 3px 8px;");
  P("  border-radius: 20px;");
  P("  display: inline-block;");
  P("  margin-top: 5px;");
  P("}");
  P(".status-normal { background: rgba(16, 185, 129, 0.1); color: var(--color-success); }");
  P(".status-warning { background: rgba(245, 158, 11, 0.1); color: var(--color-warning); }");
  P(".status-danger { background: rgba(239, 68, 68, 0.1); color: var(--color-danger); }");
  P(".status-info { background: rgba(59, 130, 246, 0.1); color: var(--color-info); }");
  P(".progress-container {");
  P("  width: 100%;");
  P("  height: 8px;");
  P("  background: var(--color-light);");
  P("  border-radius: 4px;");
  P("  margin: 10px 0;");
  P("}");
  P(".progress-bar {");
  P("  height: 100%;");
  P("  border-radius: 4px;");
  P("  transition: width 0.3s ease;");
  P("}");
  P(".progress-normal { background: var(--color-success); }");
  P(".progress-warning { background: var(--color-warning); }");
  P(".progress-danger { background: var(--color-danger); }");
  P(".detail-section {");
  P("  background: white;");
  P("  border-radius: var(--radius-md);");
  P("  box-shadow: var(--shadow-sm);");
  P("  padding: 20px;");
  P("  margin-bottom: 25px;");
  P("}");
  P(".section-title {");
  P("  font-size: 18px;");
  P("  font-weight: 600;");
  P("  margin-bottom: 20px;");
  P("  color: var(--color-primary);");
  P("}");
  P(".detail-grid {");
  P("  display: grid;");
  P("  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));");
  P("  gap: 15px;");
  P("}");
  P(".detail-item {");
  P("  padding: 10px;");
  P("  border-radius: var(--radius-sm);");
  P("  background: var(--color-light);");
  P("}");
  P(".detail-label { font-size: 14px; color: var(--color-gray); margin-bottom: 5px; }");
  P(".detail-value { font-size: 16px; font-weight: 600; }");
  P(".control-section {");
  P("  background: white;");
  P("  border-radius: var(--radius-md);");
  P("  box-shadow: var(--shadow-sm);");
  P("  padding: 20px;");
  P("}");
  P(".control-grid {");
  P("  display: grid;");
  P("  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));");
  P("  gap: 20px;");
  P("}");
  P(".control-card {");
  P("  border: 1px solid var(--color-light);");
  P("  border-radius: var(--radius-md);");
  P("  padding: 15px;");
  P("}");
  P(".control-device {");
  P("  display: flex;");
  P("  justify-content: space-between;");
  P("  align-items: center;");
  P("  margin-bottom: 15px;");
  P("}");
  P(".device-name {");
  P("  font-size: 16px;");
  P("  font-weight: 600;");
  P("}");
  P(".toggle-switch {");
  P("  position: relative;");
  P("  width: 50px;");
  P("  height: 26px;");
  P("  display: inline-block;");
  P("}");
  P(".toggle-switch input { opacity: 0; width: 0; height: 0; }");
  P(".toggle-slider {");
  P("  position: absolute;");
  P("  cursor: pointer;");
  P("  top: 0; left: 0; right: 0; bottom: 0;");
  P("  background-color: #ccc;");
  P("  transition: .4s;");
  P("  border-radius: 34px;");
  P("}");
  P(".toggle-slider:before {");
  P("  position: absolute;");
  P("  content: \"\";");
  P("  height: 18px;");
  P("  width: 18px;");
  P("  left: 4px;");
  P("  bottom: 4px;");
  P("  background-color: white;");
  P("  transition: .4s;");
  P("  border-radius: 50%;");
  P("}");
  P("input:checked + .toggle-slider { background-color: var(--color-success); }");
  P("input:checked + .toggle-slider:before { transform: translateX(24px); }");
  P("input:disabled + .toggle-slider { background-color: #e5e7eb; cursor: not-allowed; }");
  P("@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }");
  P("@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }");
  P(".loader {");
  P("  border: 3px solid var(--color-light);");
  P("  border-top: 3px solid var(--color-primary);");
  P("  border-radius: 50%;");
  P("  width: 20px;");
  P("  height: 20px;");
  P("  animation: spin 1s linear infinite;");
  P("  display: inline-block;");
  P("}");
  P(".toast {");
  P("  position: fixed;");
  P("  bottom: 20px;");
  P("  left: 50%;");
  P("  transform: translateX(-50%) translateY(100px);");
  P("  background: var(--color-dark);");
  P("  color: white;");
  P("  padding: 10px 20px;");
  P("  border-radius: var(--radius-sm);");
  P("  box-shadow: var(--shadow-lg);");
  P("  z-index: 999;");
  P("  opacity: 0;");
  P("  transition: var(--transition);");
  P("}");
  P(".toast.show {");
  P("  opacity: 1;");
  P("  transform: translateX(-50%) translateY(0);");
  P("}");
  P(".footer {");
  P("  text-align: center;");
  P("  margin-top: 20px;");
  P("  font-size: 12px;");
  P("  color: var(--color-gray);");
  P("}");
  P("</style>");
  P("</head>");
  P("<body>");
  
  P("<div class='toast' id='toast'></div>");
  
  P("<div class='navbar'>");
  P("  <div class='navbar-title'>ESP32智能环境监控系统</div>");
  P("  <div class='status-bar'>");
  P("    <div class='status-indicator'>");
  P("      <span class='status-dot status-online' id='connDot'></span>");
  P("      <span id='connStatus'>在线</span>");
  P("    </div>");
  P(String("    <button class='mode-switch ") + String(manualControl ? "" : "mode-auto") + "' id='modeBtn'>");
  P(String("      ") + String(manualControl ? "手动模式" : "自动模式") + "");
  P("    </button>");
  P("  </div>");
  P("</div>");
  
  P("<div class='container'>");
  
  P("<div class='overview-grid'>");
  
  P("<div class='card'>");
  P("  <div class='card-header'>");
  P("    <div class='card-title'>温湿度</div>");
  P("    <div class='card-icon'><i class='fa-solid fa-temperature-half'></i></div>");
  P("  </div>");
  P(String("  <div class='data-value' id='tempValue'>") + String(lastDhtTemp) + "<span class='data-unit'>℃</span></div>");
  P("  <div class='progress-container'>");
  // 修复1：将const char*转换为String
  P(String("    <div class='progress-bar ") + String(lastDhtTemp > 30 ? "progress-warning" : "progress-normal") + "' id='tempProgress' style='width: " + String(constrain(lastDhtTemp, 0, 50)*2) + "%'></div>");
  P("  </div>");
  P(String("  <div class='data-value' id='humiValue' style='font-size: 18px;'>") + String(lastDhtHumi) + "<span class='data-unit'>%RH</span></div>");
  P("  <div class='progress-container'>");
  // 修复2：将const char*转换为String
  P(String("    <div class='progress-bar ") + String(lastDhtHumi < 30 || lastDhtHumi > 70 ? "progress-warning" : "progress-normal") + "' id='humiProgress' style='width: " + String(lastDhtHumi) + "%'></div>");
  P("  </div>");
  // 修复3：将const char*转换为String
  P(String("  <div class='data-status ") + String(lastDhtStatus == "正常" ? "status-normal" : "status-danger") + "' id='dhtStatus'>");
  P(String("    ") + lastDhtStatus + "");
  P("  </div>");
  P("</div>");
  
  P("<div class='card'>");
  P("  <div class='card-header'>");
  P("    <div class='card-title'>光照强度</div>");
  P("    <div class='card-icon'><i class='fa-solid fa-sun'></i></div>");
  P("  </div>");
  P(String("  <div class='data-value' id='lightValue'>") + String(round(lastLightIntensity)) + "<span class='data-unit'>lux</span></div>");
  P("  <div class='progress-container'>");
  // 修复4：将const char*转换为String
  P(String("    <div class='progress-bar ") + String(lastLightZone == "暗" ? "progress-warning" : "progress-normal") + "' id='lightProgress' style='width: " + String(constrain(round(lastLightIntensity)/LUX_MAX*100, 0, 100)) + "%'></div>");
  P("  </div>");
  // 修复5：将const char*转换为String
  P(String("  <div class='data-status ") + String(lastLightZone == "暗" ? "status-warning" : "status-normal") + "' id='lightZone'>");
  P(String("    ") + lastLightZone + "");
  P("  </div>");
  P("</div>");
  
  P("<div class='card'>");
  P("  <div class='card-header'>");
  P("    <div class='card-title'>土壤湿度</div>");
  P("    <div class='card-icon'><i class='fa-solid fa-seedling'></i></div>");
  P("  </div>");

  // 【修复】根据故障状态显示AO值
  if (lastSoilAO < 0) {
    // 故障状态
    P("  <div class='data-value' id='soilValue' style='color: red;'>传感器故障</div>");
    P("  <div class='progress-container'>");
    P("    <div class='progress-bar progress-danger' id='soilProgress' style='width: 0%'></div>");
    P("  </div>");
    P("  <div class='data-status status-danger' id='soilStatus'>");
    P("    检查AO/GND接线");
    P("  </div>");
  } else {
    // 正常状态
    P(String("  <div class='data-value' id='soilValue'>") + String(lastSoilAO) + "<span class='data-unit'>ADC</span></div>");
    P("  <div class='progress-container'>");
    P(String("    <div class='progress-bar ") + String(lastSoilStatus == "正常" ? "progress-normal" : (lastSoilStatus == "轻旱" ? "progress-warning" : "progress-danger")) + "' id='soilProgress' style='width: " + String(constrain(100 - (lastSoilAO - SOIL_OVER_WET_AO)/(SOIL_EXTREME_DROUGHT_AO - SOIL_OVER_WET_AO)*100, 0, 100)) + "%'></div>");
    P("  </div>");
    P(String("  <div class='data-status ") + String(lastSoilStatus == "正常" ? "status-normal" : (lastSoilStatus == "轻旱" ? "status-warning" : "status-danger")) + "' id='soilStatus'>");
    P(String("    ") + lastSoilStatus + "");
    P("  </div>");
  }
  P("</div>");
  
  P("<div class='card'>");
  P("  <div class='card-header'>");
  P("    <div class='card-title'>空气质量</div>");
  P("    <div class='card-icon'><i class='fa-solid fa-wind'></i></div>");
  P("  </div>");
  if (!sgp30Available) {
    P(String("  <div class='data-value' style='font-size: 18px;'>eCO₂: <span id='eco2Value'>-") + "</span> ppm</div>");
    P(String("  <div class='data-value' style='font-size: 18px;'>TVOC: <span id='tvocValue'>-") + "</span> ppb</div>");
    P("  <div class='data-status status-danger' id='sgp30Status'>无数据</div>");
  } else if (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME) {
    P("  <div class='data-value'>预热中</div>");
    P(String("  <div class='data-value' style='font-size: 18px;'>eCO₂: <span id='eco2Value'>-") + "</span> ppm</div>");
    P(String("  <div class='data-value' style='font-size: 18px;'>TVOC: <span id='tvocValue'>-") + "</span> ppb</div>");
    P(String("  <div class='data-status status-info' id='sgp30Status'>剩余") + String((SGP30_WARM_UP_TIME - (millis() - sgp30WarmUpStart))/1000) + "秒</div>");
  } else {
    P(String("  <div class='data-value' style='font-size: 18px;'>eCO₂: <span id='eco2Value'>") + String(lastEco2) + "</span> ppm</div>");
    P(String("  <div class='data-value' style='font-size: 18px;'>TVOC: <span id='tvocValue'>") + String(lastTvoc) + "</span> ppb</div>");
    P("  <div class='data-status status-normal' id='sgp30Status'>正常</div>");
  }
  P("</div>");
  
  P("</div>");
  
  P("<div class='detail-section'>");
  P("  <div class='section-title'>详细传感器数据</div>");
  P("  <div class='detail-grid'>");
  P("    <div class='detail-item'>");
  P("      <div class='detail-label'>光照数字输出</div>");
  P(String("      <div class='detail-value' id='lightDO'>") + String(digitalRead(LIGHT_DO_PIN)) + "</div>");
  P("    </div>");
  P("    <div class='detail-item'>");
  P("      <div class='detail-label'>土壤数字输出</div>");
  P(String("      <div class='detail-value' id='soilDO'>") + String(digitalRead(SOIL_DO_PIN)) + "</div>");
  P("    </div>");
  P("    <div class='detail-item'>");
  P("      <div class='detail-label'>SGP30状态</div>");
  // 修复8：将const char*转换为String
  P(String("      <div class='detail-value' id='sgp30Detail'>") + String(sgp30Available ? (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME ? "预热中" : "正常") : "未连接") + "</div>");
  P("    </div>");
  P("    <div class='detail-item'>");
  P("      <div class='detail-label'>系统运行模式</div>");
  P(String("      <div class='detail-value' id='modeDetail'>") + String(manualControl ? "手动控制" : "自动控制") + "</div>");
  P("    </div>");
  P("  </div>");
  P("</div>");
  
  P("<div class='control-section'>");
  P("  <div class='section-title'>设备控制中心</div>");
  P("  <div class='control-grid'>");
  
  P("  <div class='control-card'>");
  P("    <div class='control-device'>");
  P("      <div class='device-name'>水泵</div>");
  P("      <label class='toggle-switch'>");
  // 修复9：将const char*转换为String
  P(String("        <input type='checkbox' id='pumpToggle' ") + String(manualControl ? "" : "disabled") + " " + String(pumpState ? "checked" : "") + ">");
  P("        <span class='toggle-slider'></span>");
  P("      </label>");
  P("    </div>");
  // 修复10：将const char*转换为String
  P(String("    <div class='detail-label'>当前状态: <span id='pumpState' class='") + String(pumpState ? "status-normal" : "status-danger") + "'>" + String(pumpState ? "开启" : "关闭") + "</span></div>");
  P("  </div>");
  
  P("  <div class='control-card'>");
  P("    <div class='control-device'>");
  P("      <div class='device-name'>风扇</div>");
  P("      <label class='toggle-switch'>");
  // 修复11：将const char*转换为String
  P(String("        <input type='checkbox' id='fanToggle' ") + String(manualControl ? "" : "disabled") + " " + String(fanState ? "checked" : "") + ">");
  P("        <span class='toggle-slider'></span>");
  P("      </label>");
  P("    </div>");
  // 修复12：将const char*转换为String
  P(String("    <div class='detail-label'>当前状态: <span id='fanState' class='") + String(fanState ? "status-normal" : "status-danger") + "'>" + String(fanState ? "开启" : "关闭") + "</span></div>");
  P("  </div>");
  
  P("  <div class='control-card'>");
  P("    <div class='control-device'>");
  P("      <div class='device-name'>照明灯</div>");
  P("      <label class='toggle-switch'>");
  // 修复13：将const char*转换为String
  P(String("        <input type='checkbox' id='lightToggle' ") + String(manualControl ? "" : "disabled") + " " + String(lightState ? "checked" : "") + ">");
  P("        <span class='toggle-slider'></span>");
  P("      </label>");
  P("    </div>");
  // 修复14：将const char*转换为String
  P(String("    <div class='detail-label'>当前状态: <span id='lightState' class='") + String(lightState ? "status-normal" : "status-danger") + "'>" + String(lightState ? "开启" : "关闭") + "</span></div>");
  P("  </div>");
  
  P("  </div>");
  P("</div>");

  // ==============================================================================
  // 阈值设置区域
  // ==============================================================================
  P("<div class='control-section' style='margin-top: 25px;'>");
  P("  <div class='section-title'>自动控制阈值设置</div>");
  P("  <div class='threshold-grid' style='display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;'>");
  
  // 风扇阈值设置
  P("  <div class='control-card'>");
  P("    <div class='device-name' style='margin-bottom: 15px;'><i class='fa-solid fa-fan' style='margin-right: 8px;'></i>风扇启动阈值</div>");
  P("    <div class='threshold-item' style='margin-bottom: 12px;'>");
  P("      <label class='detail-label'>温度阈值 (℃): 超过此值开启</label>");
  P(String("      <input type='number' id='fanTempThreshold' value='") + String(fanTempThreshold) + "' min='20' max='50' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>");
  P("    </div>");
  P("    <div class='threshold-item'>");
  P("      <label class='detail-label'>CO₂阈值 (ppm): 超过此值开启</label>");
  P(String("      <input type='number' id='fanCO2Threshold' value='") + String(fanCO2Threshold) + "' min='400' max='5000' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>");
  P("    </div>");
  P("  </div>");
  
  // 水泵阈值设置
  P("  <div class='control-card'>");
  P("    <div class='device-name' style='margin-bottom: 15px;'><i class='fa-solid fa-droplet' style='margin-right: 8px;'></i>水泵启动阈值</div>");
  P("    <div class='threshold-item'>");
  P("      <label class='detail-label'>土壤干旱阈值 (ADC值): 超过此值开启水泵</label>");
  P(String("      <input type='number' id='pumpDroughtThreshold' value='") + String(pumpDroughtThreshold) + "' min='0' max='5000' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>");
  P("      <div class='detail-label' style='margin-top: 5px; font-size: 12px; color: #888;'>ADC范围0-4095，值越大越干。参考: 过湿≤2200, 正常2200-2800, 轻旱2800-3200, 中旱3200-3500, 重旱>3500</div>");
  P("    </div>");
  P("  </div>");
  
  // 灯泡阈值设置（使用光照强度lux，更直观）
  P("  <div class='control-card'>");
  P("    <div class='device-name' style='margin-bottom: 15px;'><i class='fa-solid fa-lightbulb' style='margin-right: 8px;'></i>灯泡启动阈值</div>");
  P("    <div class='threshold-item'>");
  P("      <label class='detail-label'>光照强度阈值 (lux): 低于此值开启灯</label>");
  P(String("      <input type='number' id='lightLuxThreshold' value='") + String(lightLuxThreshold) + "' min='50' max='5000' style='width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;'>");
  P("      <div class='detail-label' style='margin-top: 5px; font-size: 12px; color: #888;'>参考: 暗<800lux, 偏暗800-1000, 正常1000-3000, 明亮>3000</div>");
  P("    </div>");
  P("  </div>");
  
  P("  </div>");
  
  // 保存按钮
  P("  <div style='text-align: center; margin-top: 20px;'>");
  P("    <button id='saveThresholdsBtn' style='padding: 12px 40px; background: var(--color-primary); color: white; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; transition: var(--transition);'>保存阈值设置</button>");
  P("  </div>");
  P("</div>");
  
  P("</div>");
  
  P("<div class='footer'>");
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  P(String("  <p>最后更新时间: <span id='lastUpdate'>") +
    String(timeinfo->tm_year + 1900) + "年" +
    String(timeinfo->tm_mon + 1) + "月" +
    String(timeinfo->tm_mday) + "日 " +
    String(timeinfo->tm_hour) + ":" +
    String(timeinfo->tm_min) + ":" +
    String(timeinfo->tm_sec) + "</span></p>");
  P("  <p>ESP32智能监控系统 © 2025</p>");
  P("</div>");
  
  P("<script>");
  P("let refreshIntervalId;");
  P("let isRefreshing = false;");
  P(String("let currentManualMode = ") + String(manualControl ? "true" : "false") + ";");
  P("const toast = document.getElementById('toast');");
  
  P("function showToast(message, duration = 2000) {");
  P("  toast.textContent = message;");
  P("  toast.classList.add('show');");
  P("  setTimeout(() => {");
  P("    toast.classList.remove('show');");
  P("  }, duration);");
  P("}");
  
  P("window.addEventListener('DOMContentLoaded', function() {");
  P("  startAutoRefresh();");
  P("  document.getElementById('modeBtn').addEventListener('click', switchMode);");
  P("  document.getElementById('pumpToggle').addEventListener('change', (e) => controlDevice('pump', e.target.checked ? 1 : 0));");
  P("  document.getElementById('fanToggle').addEventListener('change', (e) => controlDevice('fan', e.target.checked ? 1 : 0));");
  P("  document.getElementById('lightToggle').addEventListener('change', (e) => controlDevice('light', e.target.checked ? 1 : 0));");
  P("  document.getElementById('saveThresholdsBtn').addEventListener('click', saveThresholds);");
  P("  document.addEventListener('visibilitychange', () => {");
  P("    if (document.hidden) {");
  P("      if (refreshIntervalId) clearInterval(refreshIntervalId);");
  P("    } else {");
  P("      startAutoRefresh();");
  P("    }");
  P("  });");
  P("  loadThresholds();");
  P("});");
  
  P("function startAutoRefresh() {");
  P("  if (refreshIntervalId) clearInterval(refreshIntervalId);");
  P("  autoRefreshData();");
  P("  refreshIntervalId = setInterval(autoRefreshData, 3000);");
  P("}");
  
  P("function autoRefreshData() {");
  P("  if (document.hidden || isRefreshing) return;");
  P("  isRefreshing = true;");
  // 不显示刷新中，只显示在线与离线
  P("  fetch('/data?rand=' + Math.random(), {cache: 'no-store'})");
  P("  .then(response => {");
  P("    if (!response.ok) throw new Error('网络错误');");
  P("    return response.json();");
  P("  })");
  P("  .then(data => {");
  P("    updatePageUI(data);");
  P("    currentManualMode = data.manualControl;");
  P("    document.getElementById('connStatus').textContent = '在线';");
  P("    document.getElementById('connDot').className = 'status-dot status-online';");
  P("  })");
  P("  .catch(error => {");
  P("    console.error('刷新失败:', error);");
  P("    document.getElementById('connStatus').textContent = '离线';");
  P("    document.getElementById('connDot').className = 'status-dot status-offline';");
  P("    showToast('数据刷新失败，请检查连接', 3000);");
  P("  })");
  P("  .finally(() => {");
  P("    isRefreshing = false;");
  P("  });");
  P("}");
  
  P("function updatePageUI(data) {");
    P("  currentManualMode = data.manualControl;");
  P("  document.getElementById('tempValue').innerHTML = data.dhtTemp + ' <span class=\"data-unit\">℃</span>';");
  P("  document.getElementById('humiValue').innerHTML = data.dhtHumi + ' <span class=\"data-unit\">%RH</span>';");
  P("  document.getElementById('dhtStatus').textContent = data.dhtStatus;");
  P("  document.getElementById('dhtStatus').className = 'data-status ' + (data.dhtStatus === '正常' ? 'status-normal' : 'status-danger');");
  P("  const tempProgress = Math.min(Math.max(data.dhtTemp * 2, 0), 100);");
  P("  document.getElementById('tempProgress').style.width = tempProgress + '%';");
  P("  document.getElementById('tempProgress').className = 'progress-bar ' + (data.dhtTemp > 30 ? 'progress-warning' : 'progress-normal');");
  P("  document.getElementById('humiProgress').style.width = data.dhtHumi + '%';");
  P("  document.getElementById('humiProgress').className = 'progress-bar ' + (data.dhtHumi < 30 || data.dhtHumi > 70 ? 'progress-warning' : 'progress-normal');");
  
  P("  document.getElementById('lightValue').innerHTML = data.lightIntensity + ' <span class=\"data-unit\">lux</span>';");
  P("  document.getElementById('lightZone').textContent = data.lightZone;");
  P("  document.getElementById('lightZone').className = 'data-status ' + (data.lightZone === '暗' ? 'status-warning' : 'status-normal');");
  // 【优化】使用后端计算的lightProgress，避免前端硬编码LUX_MAX
  P("  document.getElementById('lightProgress').style.width = data.lightProgress + '%';");
  P("  document.getElementById('lightProgress').className = 'progress-bar ' + (data.lightZone === '暗' ? 'progress-warning' : 'progress-normal');");
  
  P("  document.getElementById('soilValue').innerHTML = data.soilAO + ' <span class=\"data-unit\">ADC</span>';");
  P("  document.getElementById('soilStatus').textContent = data.soilStatus;");
  P("  document.getElementById('soilStatus').className = 'data-status ' + (data.soilStatus === '正常' ? 'status-normal' : (data.soilStatus === '轻旱' ? 'status-warning' : 'status-danger'));");
  P(String("  const soilProgress = Math.min(Math.max(100 - (data.soilAO - ") + String(SOIL_OVER_WET_AO) + ")/(" + String(SOIL_EXTREME_DROUGHT_AO - SOIL_OVER_WET_AO) + ")*100, 0), 100);");
  P("  document.getElementById('soilProgress').style.width = soilProgress + '%';");
  P("  document.getElementById('soilProgress').className = 'progress-bar ' + (data.soilStatus === '正常' ? 'progress-normal' : (data.soilStatus === '轻旱' ? 'progress-warning' : 'progress-danger'));");
  
  P("  if (data.sgp30Status === '无数据') {");
  P("    document.getElementById('eco2Value').textContent = '-';");
  P("    document.getElementById('tvocValue').textContent = '-';");
  P("    document.getElementById('sgp30Status').textContent = '无数据';");
  P("    document.getElementById('sgp30Status').className = 'data-status status-danger';");
  P("    document.getElementById('sgp30Detail').textContent = '未连接';");
  P("  } else if (data.sgp30Status.includes('预热')) {");
  P("    document.getElementById('eco2Value').textContent = '-';");
  P("    document.getElementById('tvocValue').textContent = '-';");
  P("    document.getElementById('sgp30Status').textContent = data.sgp30Status;");
  P("    document.getElementById('sgp30Status').className = 'data-status status-info';");
  P("    document.getElementById('sgp30Detail').textContent = '预热中';");
  P("  } else {");
  P("    document.getElementById('eco2Value').textContent = data.eco2;");
  P("    document.getElementById('tvocValue').textContent = data.tvoc;");
  P("    document.getElementById('sgp30Status').textContent = '正常';");
  P("    document.getElementById('sgp30Status').className = 'data-status status-normal';");
  P("    document.getElementById('sgp30Detail').textContent = '正常';");
  P("  }");
  
  P("  document.getElementById('lightDO').textContent = data.lightDO;");
  P("  document.getElementById('soilDO').textContent = data.soilDO;");
  
  P("  document.getElementById('pumpState').textContent = data.pumpState ? '开启' : '关闭';");
  P("  document.getElementById('pumpState').className = data.pumpState ? 'status-normal' : 'status-danger';");
  P("  document.getElementById('fanState').textContent = data.fanState ? '开启' : '关闭';");
  P("  document.getElementById('fanState').className = data.fanState ? 'status-normal' : 'status-danger';");
  P("  document.getElementById('lightState').textContent = data.lightState ? '开启' : '关闭';");
  P("  document.getElementById('lightState').className = data.lightState ? 'status-normal' : 'status-danger';");
  
  P("  document.getElementById('modeBtn').textContent = data.manualControl ? '手动模式' : '自动模式';");
  P("  document.getElementById('modeBtn').className = 'mode-switch ' + (data.manualControl ? '' : 'mode-auto');");
  P("  document.getElementById('modeDetail').textContent = data.manualControl ? '手动控制' : '自动控制';");
  
  P("  document.getElementById('pumpToggle').checked = data.pumpState;");
  P("  document.getElementById('fanToggle').checked = data.fanState;");
  P("  document.getElementById('lightToggle').checked = data.lightState;");
  P("  document.getElementById('pumpToggle').disabled = !data.manualControl;");
  P("  document.getElementById('fanToggle').disabled = !data.manualControl;");
  P("  document.getElementById('lightToggle').disabled = !data.manualControl;");
  
  P("  if (data.lastUpdate) { document.getElementById('lastUpdate').textContent = data.lastUpdate; }");
  P("}");
  
  P("function switchMode() {");
  P("  if (isRefreshing) return;");
  P("  fetch('/mode?state=' + (currentManualMode ? '0' : '1'))");
  P("  .then(response => response.text())");
  P("  .then(() => {");
  P("    const newMode = !currentManualMode;");
  P("    showToast('已切换到' + (newMode ? '手动模式' : '自动模式'));");
  P("    autoRefreshData();");
  P("  })");
  P("  .catch(error => {");
  P("    console.error('模式切换失败:', error);");
  P("    showToast('模式切换失败，请重试', 3000);");
  P("  });");
  P("}");
  
  P("function controlDevice(device, state) {");
  P("  if (!currentManualMode) return;");
  P("  if (refreshIntervalId) clearInterval(refreshIntervalId);");
  P("  const url = '/' + device + '?state=' + state + '&ts=' + Date.now();");
  P("  fetch(url, { cache: 'no-store' })");
  P("  .then(response => response.text())");
  P("  .then(() => {");
  P("    const deviceName = device === 'pump' ? '水泵' : device === 'fan' ? '风扇' : '照明灯';");
  P("    showToast(deviceName + (state === 1 ? '已开启' : '已关闭'));");
  P("    isRefreshing = false;");
  P("    autoRefreshData();");
  P("  })");
  P("  .catch(error => {");
  P("    console.error(device + '控制失败:', error);");
  P("    showToast('设备控制失败，请重试', 3000);");
  P("    isRefreshing = false;");
  P("    autoRefreshData();");
  P("  })");
  P("  .finally(() => {");
  P("    startAutoRefresh();");
  P("  });");
  P("}");
  
  // 阈值设置相关函数
  P("function loadThresholds() {");
  P("  fetch('/getThresholds')");
  P("  .then(response => response.json())");
  P("  .then(data => {");
  P("    document.getElementById('fanTempThreshold').value = data.fanTempThreshold;");
  P("    document.getElementById('fanCO2Threshold').value = data.fanCO2Threshold;");
  P("    document.getElementById('pumpDroughtThreshold').value = data.pumpDroughtThreshold;");
  P("    document.getElementById('lightLuxThreshold').value = data.lightLuxThreshold;");
  P("  })");
  P("  .catch(error => console.error('加载阈值失败:', error));");
  P("}");
  
  P("function saveThresholds() {");
  P("  const fanTemp = document.getElementById('fanTempThreshold').value;");
  P("  const fanCO2 = document.getElementById('fanCO2Threshold').value;");
  P("  const pumpDrought = document.getElementById('pumpDroughtThreshold').value;");
  P("  const lightLux = document.getElementById('lightLuxThreshold').value;");
  P("  const params = `fanTemp=${fanTemp}&fanCO2=${fanCO2}&pumpDrought=${pumpDrought}&lightLux=${lightLux}`;");
  P("  fetch('/setThresholds?' + params)");
  P("  .then(response => response.text())");
  P("  .then(result => {");
  P("    showToast('阈值设置已保存！', 2000);");
  P("  })");
  P("  .catch(error => {");
  P("    console.error('保存阈值失败:', error);");
  P("    showToast('保存失败，请重试', 3000);");
  P("  });");
  P("}");
  
  P("</script>");
  P("</body></html>");
  
  request->send(response);
  #undef P
}

void handleData(AsyncWebServerRequest *request) {
  // 【新增】记录网页端轮询时间，用于判断网页端是否在线
  lastWebPoll = millis();
  if (!webClientConnected) {
    webClientConnected = true;
    Serial.println("[SD] 网页端已重新连接");
    if (hasSDCachedData()) {
      Serial.println("[SD] 检测到缓存数据，网页端可通过 /cachedData 获取");
    }
  }

  StaticJsonDocument<512> doc;
  
  doc["lightIntensity"] = round(lastLightIntensity);
  doc["lightZone"] = lastLightZone;
  doc["lightDO"] = digitalRead(LIGHT_DO_PIN);
  
  doc["dhtTemp"] = lastDhtTemp;
  doc["dhtHumi"] = lastDhtHumi;
  doc["dhtStatus"] = lastDhtStatus;
  
  doc["soilAO"] = (lastSoilAO < 0) ? 0 : lastSoilAO;  // 【修复】故障时返回0而不是-1
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

  // 【新增】告知网页端是否有离线缓存数据待获取
  doc["hasCachedData"] = sdCachePending;
  
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

// ==============================================================================
// 【新增】设置离线缓存间隔（秒）
// GET /setCacheInterval?interval=10  (范围: 5-3600秒)
// ==============================================================================
void handleSetCacheInterval(AsyncWebServerRequest *request) {
  if (!request->hasArg("interval")) {
    request->send(400, "application/json", "{\"error\":\"缺少interval参数\"}");
    return;
  }
  
  long intervalSec = request->arg("interval").toInt();
  if (intervalSec < 5 || intervalSec > 3600) {
    request->send(400, "application/json", "{\"error\":\"interval范围: 5-3600秒\"}");
    return;
  }
  
  cacheWriteInterval = (unsigned long)intervalSec * 1000;
  preferences.putULong("cacheIntv", cacheWriteInterval);
  
  Serial.printf("[SD] 离线缓存间隔已更新: %ld秒\n", intervalSec);
  
  StaticJsonDocument<128> doc;
  doc["success"] = true;
  doc["cacheInterval"] = intervalSec;
  String resp;
  serializeJson(doc, resp);
  
  AsyncWebServerResponse *response = request->beginResponse(200, "application/json", resp);
  response->addHeader("Access-Control-Allow-Origin", "*");
  request->send(response);
}

// ==============================================================================
// 【新增】获取离线缓存间隔和状态
// GET /getCacheInterval
// ==============================================================================
void handleGetCacheInterval(AsyncWebServerRequest *request) {
  StaticJsonDocument<256> doc;
  doc["cacheInterval"] = cacheWriteInterval / 1000;  // 返回秒
  doc["sdCardAvailable"] = sdCardAvailable;
  doc["webClientConnected"] = webClientConnected;
  doc["hasCachedData"] = hasSDCachedData();
  
  if (sdCardAvailable && SD.exists(SD_CACHE_FILE)) {
    File f = SD.open(SD_CACHE_FILE, FILE_READ);
    if (f) {
      doc["cachedFileSize"] = (unsigned long)f.size();
      f.close();
    }
  } else {
    doc["cachedFileSize"] = 0;
  }
  
  String resp;
  serializeJson(doc, resp);
  
  AsyncWebServerResponse *response = request->beginResponse(200, "application/json", resp);
  response->addHeader("Access-Control-Allow-Origin", "*");
  request->send(response);
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

/* ============================================================================
// MQTT相关函数（已禁用）
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

  // 【修复】断开旧的TLS连接，释放内存，防止泄漏
  mqttClient.disconnect();
  mqttSecureClient.stop();
  delay(100);

  // 【修改】动态生成clientId和password
  String timestamp = generateTimestamp();
  String dynamicClientId = String(mqttDeviceId) + "_0_0_" + timestamp;
  String dynamicPassword = generateMqttPassword(deviceSecret, timestamp);
  
  Serial.printf("[MQTT] 时间戳: %s, ClientId: %s\n", timestamp.c_str(), dynamicClientId.c_str());
  Serial.printf("[MQTT] 空闲堆: %u bytes, 最大块: %u bytes\n", ESP.getFreeHeap(), ESP.getMaxAllocHeap());
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
*/ // MQTT相关函数结束

// ==============================================================================
// 【新增】SD卡初始化函数（带重试和低速模式）
// ==============================================================================
bool initSDCard() {
  // 先确保CS引脚为高电平（取消选中），避免与其他SPI设备冲突
  pinMode(SD_CS_PIN, OUTPUT);
  digitalWrite(SD_CS_PIN, HIGH);
  delay(100);

  // 使用全局SPI实例，避免与OLED等设备的默认SPI冲突
  spiSD.begin(18, 19, 23, SD_CS_PIN);  // SCK, MISO, MOSI, CS

  // 尝试多次初始化，逐步降低SPI频率
  const uint32_t frequencies[] = {4000000, 2000000, 1000000, 400000};  // 4M→400K
  for (int freq = 0; freq < 4; freq++) {
    Serial.printf("[SD] 尝试初始化... SPI频率: %luHz\n", frequencies[freq]);
    if (SD.begin(SD_CS_PIN, spiSD, frequencies[freq])) {
      uint8_t cardType = SD.cardType();
      if (cardType != CARD_NONE) {
        Serial.print("[SD] ★ 初始化成功！卡类型: ");
        if (cardType == CARD_MMC) Serial.println("MMC");
        else if (cardType == CARD_SD) Serial.println("SDSC");
        else if (cardType == CARD_SDHC) Serial.println("SDHC");
        Serial.printf("[SD] 容量: %lluMB, 已用: %lluMB\n", 
                      SD.totalBytes() / (1024 * 1024), 
                      SD.usedBytes() / (1024 * 1024));
        return true;
      }
      Serial.println("[SD] SD.begin成功但未检测到卡，重试...");
      SD.end();
    }
    delay(200);
  }

  Serial.println("【警告】SD卡初始化失败！请检查：");
  Serial.println("  1. 接线: CS→5, SCK→18, MOSI→23, MISO→19, VCC→5V, GND→GND");
  Serial.println("  2. SD卡是否已格式化为FAT32");
  Serial.println("  3. SD卡是否插紧");
  Serial.println("  4. 模块电源指示灯是否亮");
  return false;
}

// ==============================================================================
// 【新增】将传感器数据缓存到SD卡（JSON Lines格式，每行一条记录）
// ==============================================================================
void cacheDataToSD() {
  if (!sdCardAvailable) return;

  // 【新增】过滤异常数据：所有传感器数据都为0时不缓存
  if (lastDhtTemp == 0 && lastDhtHumi == 0 && lastSoilAO == 0 && 
      static_cast<int>(lastLightIntensity) == 0 && lastEco2 == 0) {
    Serial.println("[SD] 传感器数据全为0，跳过缓存");
    return;
  }

  File file = SD.open(SD_CACHE_FILE, FILE_APPEND);
  if (!file) {
    Serial.println("[SD] 打开缓存文件失败！");
    return;
  }

  // 获取当前时间戳
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  char timeStr[24];
  snprintf(timeStr, sizeof(timeStr), "%04d-%02d-%02d %02d:%02d:%02d",
    timeinfo->tm_year + 1900, timeinfo->tm_mon + 1, timeinfo->tm_mday,
    timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec);

  // 构造JSON行
  StaticJsonDocument<384> doc;
  doc["ts"] = timeStr;
  doc["temp"] = lastDhtTemp;
  doc["humi"] = lastDhtHumi;
  doc["soil"] = lastSoilAO;
  doc["lightLux"] = static_cast<int>(lastLightIntensity);
  doc["eco2"] = lastEco2;
  doc["tvoc"] = lastTvoc;
  doc["pump"] = pumpState;
  doc["fan"] = fanState;
  doc["light"] = lightState;
  doc["manual"] = manualControl;

  String line;
  serializeJson(doc, line);
  file.println(line);
  file.close();
  sdCachePending = true;

  Serial.println("[SD] 数据已缓存: " + String(timeStr));
}

// ==============================================================================
// 【新增】检查SD卡是否存在缓存数据
// ==============================================================================
bool hasSDCachedData() {
  if (!sdCardAvailable) return false;
  if (sdCachePending) return true;

  sdCachePending = SD.exists(SD_CACHE_FILE);
  return sdCachePending;
}

// ==============================================================================
// 【新增】HTTP接口 - 返回SD卡中的所有缓存数据并清除缓存文件
// 网页端重连后调用 GET /cachedData 获取断连期间的历史数据
// 返回JSON数组，每个元素包含时间戳和传感器数据
// ==============================================================================
void handleCachedData(AsyncWebServerRequest *request) {
  // 更新轮询时间（说明网页端在线）
  lastWebPoll = millis();
  webClientConnected = true;

  if (!sdCardAvailable || !SD.exists(SD_CACHE_FILE)) {
    // 无缓存数据，返回空数组
    sdCachePending = false;
    request->send(200, "application/json", "{\"cached\":[]}");
    return;
  }

  File file = SD.open(SD_CACHE_FILE, FILE_READ);
  if (!file) {
    request->send(500, "application/json", "{\"error\":\"无法读取缓存文件\"}");
    return;
  }

  // 构造JSON数组响应（使用流式输出避免内存溢出）
  AsyncResponseStream *resp = request->beginResponseStream("application/json");
  resp->addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
  resp->addHeader("Access-Control-Allow-Origin", "*");
  resp->print("{\"cached\":[");
  bool first = true;
  int count = 0;
  const int MAX_CACHE_ITEMS = 500;  // 限制最大返回条数

  while (file.available() && count < MAX_CACHE_ITEMS) {
    String line = file.readStringUntil('\n');
    line.trim();
    if (line.length() == 0) continue;

    // 验证JSON格式有效
    StaticJsonDocument<384> testDoc;
    DeserializationError err = deserializeJson(testDoc, line);
    if (err) continue;

    if (!first) resp->print(",");
    resp->print(line);
    first = false;
    count++;
  }
  file.close();

  resp->print("]}");

  // 删除缓存文件（数据已被取走）
  SD.remove(SD_CACHE_FILE);
  sdCachePending = false;
  Serial.printf("[SD] ★ 网页端取走 %d 条缓存数据，缓存文件已清除\n", count);

  request->send(resp);
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

  // 【关键】配置ADC衰减，扩展测量范围到0-3.3V
  // 默认0db只能测0-1.1V，会导致读数饱和为4095触发故障检测
  analogSetPinAttenuation(SOIL_AO_PIN, ADC_11db);  // 土壤传感器AO
  analogSetPinAttenuation(LIGHT_AO_PIN, ADC_11db); // 光照传感器AO

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

  // 【诊断】检测OLED I2C地址
  Serial.println("\n===== OLED屏幕诊断 =====");
  uint8_t oledAddr = 0;
  for (uint8_t addr = 0x3C; addr <= 0x3D; addr++) {
    Wire.beginTransmission(addr);
    if (Wire.endTransmission() == 0) {
      oledAddr = addr;
      Serial.print("找到OLED，I2C地址: 0x");
      Serial.println(addr, HEX);
      break;
    }
  }
  if (oledAddr == 0) {
    Serial.println("未检测到OLED！请检查：");
    Serial.println("  1. VCC是否接3.3V或5V");
    Serial.println("  2. GND是否接地");
    Serial.println("  3. SDA是否接GPIO21");
    Serial.println("  4. SCL是否接GPIO22");
    Serial.println("  5. 模块是否损坏");
  } else if (oledAddr == 0x3D) {
    Serial.println("OLED使用非默认地址0x3D，已自动设置");
    u8g2.setI2CAddress(0x3D * 2);
  }
  Serial.println("========================\n");

  u8g2.begin();
  u8g2.enableUTF8Print();

  // 【诊断】OLED初始化后测试显示
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_wqy12_t_gb2312);
  u8g2.setCursor(20, 35);
  u8g2.print("OLED 初始化成功");
  u8g2.sendBuffer();
  Serial.println("OLED测试画面已发送");
  delay(1500);

  // 【新增】从Flash加载持久化的阈值设置
  preferences.begin("thresholds", false);
  fanTempThreshold = preferences.getInt("fanTemp", 30);           // 默认30℃
  fanCO2Threshold = preferences.getInt("fanCO2", 1000);          // 默认1000ppm
  pumpDroughtThreshold = preferences.getInt("pumpDrought", 3200); // 默认3200
  lightLuxThreshold = preferences.getInt("lightLux", 800);        // 默认800lux
  cacheWriteInterval = preferences.getULong("cacheIntv", 10000);  // 默认10秒
  Serial.println("已从Flash加载阈值设置:");
  Serial.print("  风扇温度阈值: "); Serial.println(fanTempThreshold);
  Serial.print("  风扇CO2阈值: "); Serial.println(fanCO2Threshold);
  Serial.print("  水泵干旱阈值: "); Serial.println(pumpDroughtThreshold);
  Serial.print("  灯泡光照阈值(lux): "); Serial.println(lightLuxThreshold);
  Serial.print("  离线缓存间隔: "); Serial.print(cacheWriteInterval / 1000); Serial.println("秒");

  i2cScan();

  // 【新增】初始化SD卡
  sdCardAvailable = initSDCard();
  if (sdCardAvailable && hasSDCachedData()) {
    Serial.println("[SD] 检测到历史缓存数据");
  }

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

  // NTP时间同步（MQTT已禁用，但保留时间同步用于SD卡缓存时间戳）
  configTime(8 * 3600, 0, "ntp.aliyun.com", "ntp1.aliyun.com", "pool.ntp.org");
  Serial.println("正在同步网络时间...");
  int retry = 0;
  const int maxRetry = 20;
  time_t now = time(nullptr);
  while ((now < 1672531200) && (retry < maxRetry)) {
    delay(500);
    now = time(nullptr);
    retry++;
  }
  if (now >= 1672531200) {
    struct tm* timeinfo = localtime(&now);
    Serial.printf("当前时间: %04d-%02d-%02d %02d:%02d:%02d\n", timeinfo->tm_year+1900, timeinfo->tm_mon+1, timeinfo->tm_mday, timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec);
  } else {
    Serial.println("NTP时间同步失败");
  }

  /* MQTT已禁用
  mqttSecureClient.setInsecure();
  mqttSecureClient.setTimeout(15);
  mqttClient.setServer(mqttHost, mqttPort);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setBufferSize(1024);
  mqttClient.setSocketTimeout(15);
  mqttClient.setKeepAlive(120);
  delay(1000);
  ensureMqttConnection();
  */

  server.on("/", HTTP_GET, handleRoot);
  server.on("/data", HTTP_GET, handleData);
  server.on("/pump", HTTP_GET, handlePumpControl);
  server.on("/fan", HTTP_GET, handleFanControl);
  server.on("/light", HTTP_GET, handleLightControl);
  server.on("/mode", HTTP_GET, handleModeControl);
  server.on("/setThresholds", HTTP_GET, handleSetThresholds);
  server.on("/getThresholds", HTTP_GET, handleGetThresholds);
  server.on("/cachedData", HTTP_GET, handleCachedData);  // 【新增】获取离线缓存数据
  server.on("/setCacheInterval", HTTP_GET, handleSetCacheInterval);  // 【新增】设置缓存间隔
  server.on("/getCacheInterval", HTTP_GET, handleGetCacheInterval);  // 【新增】获取缓存间隔
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
  static unsigned long lastHeapCheck = 0;
  unsigned long currentTime = millis();

  // 【修复】堆内存监控 - 每10秒检查一次，低于阈值时警告
  if (currentTime - lastHeapCheck >= 10000) {
    lastHeapCheck = currentTime;
    uint32_t freeHeap = ESP.getFreeHeap();
    if (freeHeap < 30000) {
      Serial.printf("【警告】堆内存不足！剩余: %u bytes，最大块: %u bytes\n", freeHeap, ESP.getMaxAllocHeap());
    }
  }

  // 【新增】实时诊断模式 - 输入"s"进行土壤传感器快速诊断
  if (Serial.available()) {
    char cmd = Serial.read();
    if (cmd == 's' || cmd == 'S') {
      Serial.println("\n╔════════════════════════════════════════╗");
      Serial.println("║   【土壤传感器完整诊断模式】             ║");
      Serial.println("║        AO→GPIO35  DO→GPIO16              ║");
      Serial.println("╚════════════════════════════════════════╝");

      int minAO = 5000, maxAO = 0;
      int doCount_0 = 0, doCount_1 = 0;

      for (int i = 0; i < 20; i++) {
        int rawAO = analogRead(SOIL_AO_PIN);
        int doPin = digitalRead(SOIL_DO_PIN);

        minAO = min(minAO, rawAO);
        maxAO = max(maxAO, rawAO);
        if (doPin == 0) doCount_0++;
        else doCount_1++;

        Serial.printf("[%2d] AO=%4d DO=%d ", i+1, rawAO, doPin);

        // 状态判断
        if (rawAO >= 4090) {
          Serial.println("【异常高-悬空】");
        } else if (rawAO <= 10) {
          Serial.println("【异常低-短路】");
        } else if (rawAO <= 2200) {
          Serial.println(" → 过湿");
        } else if (rawAO <= 2800) {
          Serial.println(" → 正常");
        } else if (rawAO <= 3200) {
          Serial.println(" → 轻旱");
        } else if (rawAO <= 3500) {
          Serial.println(" → 中旱");
        } else {
          Serial.println(" → 重旱");
        }

        delay(150);
      }

      Serial.println("\n【诊断结果统计】");
      Serial.printf("  AO范围: %d ~ %d (变化: %d)\n", minAO, maxAO, maxAO - minAO);
      Serial.printf("  DO值: %d次为0(干)  %d次为1(湿)\n", doCount_0, doCount_1);

      if (minAO >= 4090 || maxAO >= 4090) {
        Serial.println("\n⚠️  【故障】AO发现4090+ → GPIO35接线断开或传感器未供电");
      } else if (maxAO <= 10) {
        Serial.println("\n⚠️  【故障】AO发现0~10 → GPIO35接GND短路或传感器坏");
      } else if ((maxAO - minAO) < 50) {
        Serial.println("\n⚠️  【异常】AO变化<50 → 可能接线接触不良");
      } else {
        Serial.println("\n✓ 【正常】AO值在合理范围内, 传感器工作正常");
      }

      Serial.println("\n【DO口单独诊断】");
      if (doCount_0 > 15) {
        Serial.println("  DO持续为0(干旱) → 需要浇水 或 DO接线问题");
      } else if (doCount_1 > 15) {
        Serial.println("  DO持续为1(潮湿) → 土壤过湿 或 DO阈值设置太低");
      } else if (doCount_0 > 0 && doCount_1 > 0) {
        Serial.println("  DO值在变化 → 工作正常");
      }

      Serial.println("\n═══════════════════════════════════════\n");
    }
  }

  /* MQTT已禁用
  ensureMqttConnection();
  mqttClient.loop();

  if (mqttClient.connected() && currentTime - lastMqttPublish >= mqttPublishInterval) {
    lastMqttPublish = currentTime;
    publishTelemetry();
  }
  */

  // ==============================================================================
  // 【新增】SD卡离线缓存逻辑（检测网页端是否在轮询）
  // ==============================================================================
  // 判断网页端是否断连：超过60秒没有轮询/data接口
  // 注意：lastWebPoll 由异步回调（Core 0）更新，需要先缓存再比较，防止unsigned溢出
  unsigned long cachedLastWebPoll = lastWebPoll;
  if (cachedLastWebPoll > 0 && currentTime >= cachedLastWebPoll && currentTime - cachedLastWebPoll > webDisconnectTimeout) {
    if (webClientConnected) {
      webClientConnected = false;
      Serial.println("[SD] 网页端已断连，开始缓存数据到SD卡...");
    }
  }

  // 网页端离线时（包括开机后从未连接），定期将数据缓存到SD卡
  if (!webClientConnected) {
    if (sdCardAvailable && currentTime - lastCacheWrite >= cacheWriteInterval) {
      lastCacheWrite = currentTime;
      cacheDataToSD();
    }
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

    int soilAO = readSoilSensorRaw();  // 【修复】使用多采样平均读取
    int soilDO = digitalRead(SOIL_DO_PIN);  // 【修复】实时读取DO值

    // 【调试模式】直接显示原始ADC值，跳过故障检测
    lastSoilAO = soilAO;  // 直接使用原始值
    lastSoilDO = soilDO;
    updateSoilStatus(soilAO, lastSoilStatus);

    // 串口输出原始值便于调试
    static unsigned long lastDebugPrint = 0;
    if (millis() - lastDebugPrint > 2000) {  // 每2秒打印一次
      Serial.printf("[调试] 土壤原始ADC=%d, DO=%d\n", soilAO, soilDO);
      lastDebugPrint = millis();
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
      Serial.printf("传感器数据更新 - %lu秒\n", millis()/1000);
      Serial.printf("空闲堆内存: %u bytes, 最大可分配块: %u bytes\n", ESP.getFreeHeap(), ESP.getMaxAllocHeap());
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
      if (soilSensorError) {
        Serial.println("【故障】");
        Serial.printf("⚠️  原始值=%d  DO=%d  (传感器异常,检查AO/GND接线)\n",
          readSoilSensorRaw(), digitalRead(SOIL_DO_PIN));
      } else {
        Serial.printf("%d  (DO=%d)\n", lastSoilAO, lastSoilDO);
      }
      Serial.print("土壤湿度状态: ");
      Serial.println(lastSoilStatus);
      Serial.print("土壤DO值: ");
      Serial.println(digitalRead(SOIL_DO_PIN));
      
      if (sgp30Available) {
        if (millis() - sgp30WarmUpStart < SGP30_WARM_UP_TIME) {
          Serial.printf("SGP30: 预热中（剩余%lu秒）\n", (SGP30_WARM_UP_TIME - (millis() - sgp30WarmUpStart))/1000);
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
      Serial.println("------------------------------------");
      Serial.print("SD卡状态: ");
      Serial.println(sdCardAvailable ? "正常" : "未连接");
      if (sdCardAvailable) {
        Serial.printf("SD卡容量: %lluMB, 已用: %lluMB\n", SD.totalBytes() / (1024 * 1024), SD.usedBytes() / (1024 * 1024));
        Serial.print("离线缓存: ");
        if (SD.exists(SD_CACHE_FILE)) {
          File f = SD.open(SD_CACHE_FILE, FILE_READ);
          if (f) {
            Serial.printf("有缓存数据 (%lu 字节)\n", (unsigned long)f.size());
            f.close();
          }
        } else {
          Serial.println("无缓存");
        }
      }
      Serial.print("网页端状态: ");
      Serial.println(webClientConnected ? "在线" : "离线");
      Serial.println("====================================\n");
    }
  }

  // 处理WiFiManager循环
  wm.process();
}
