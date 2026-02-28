# -*- coding:utf-8 -*-
"""
华为云FunctionGraph函数 - 接收IoTDA推送的ESP32传感器数据并写入RDS MySQL

环境变量配置（在FunctionGraph控制台设置）:
- DB_HOST: RDS MySQL主机地址
- DB_PORT: 3306
- DB_USER: 数据库用户名
- DB_PASSWORD: 数据库密码
- DB_NAME: agri_db

触发器: HTTP触发器（通过IoTDA数据转发规则推送）
"""

import json
import pymysql
import os
from datetime import datetime

def handler(event, context):
    """
    处理IoTDA推送的设备数据
    
    event格式示例:
    {
        "body": "{\"notify_data\": {...}}",
        "headers": {...},
        "httpMethod": "POST"
    }
    """
    try:
        # 解析请求体
        if isinstance(event.get('body'), str):
            body = json.loads(event['body'])
        else:
            body = event.get('body', {})
        
        print(f"收到IoTDA推送: {json.dumps(body, ensure_ascii=False)}")
        
        # 提取设备数据
        notify_data = body.get('notify_data', {})
        device_body = notify_data.get('body', {})
        services = device_body.get('services', [])
        
        if not services:
            return {
                "statusCode": 200,
                "body": json.dumps({"message": "no services data"})
            }
        
        # 获取属性数据
        properties = services[0].get('properties', {})
        
        # 提取传感器数据
        temp = properties.get('temp')
        humi = properties.get('humi')
        soil = properties.get('soil')
        light_lux = properties.get('lightLux')
        eco2 = properties.get('eco2')
        tvoc = properties.get('tvoc')
        
        # 提取设备状态
        pump = properties.get('pump', False)
        fan = properties.get('fan', False)
        light = properties.get('light', False)
        manual = properties.get('manual', False)
        
        # 提取阈值设置
        light_threshold = properties.get('lightLuxThreshold')
        pump_threshold = properties.get('pumpDroughtThreshold')
        fan_temp_threshold = properties.get('fanTempThreshold')
        fan_co2_threshold = properties.get('fanCO2Threshold')
        
        print(f"解析数据: temp={temp}, humi={humi}, soil={soil}, lightLux={light_lux}, eCO2={eco2}")
        
        # 连接数据库
        conn = pymysql.connect(
            host=os.environ['DB_HOST'],
            port=int(os.environ.get('DB_PORT', 3306)),
            user=os.environ['DB_USER'],
            password=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        
        try:
            with conn.cursor() as cursor:
                # 插入环境数据
                sql = """
                INSERT INTO t_env_data 
                (sensor_id, temperature, humidity, soil_moisture, light_intensity, co2, tvoc, 
                 pump_state, fan_state, light_state, manual_mode, collect_time)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
                """
                
                # 土壤湿度ADC转百分比（根据技术文档公式）
                soil_percentage = None
                if soil is not None:
                    soil_percentage = max(0, min(100, 100 - ((soil - 2200) / (4000 - 2200)) * 100))
                
                cursor.execute(sql, (
                    1,  # sensor_id 默认为1
                    temp,
                    humi,
                    soil_percentage,  # 存储百分比
                    light_lux,
                    eco2,
                    tvoc,
                    pump,
                    fan,
                    light,
                    manual
                ))
                
                # 更新设备状态表（如果有）
                status_sql = """
                INSERT INTO t_device_status 
                (device_id, pump_state, fan_state, light_state, manual_mode, 
                 light_threshold, pump_threshold, fan_temp_threshold, fan_co2_threshold, 
                 update_time)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
                ON DUPLICATE KEY UPDATE
                pump_state=VALUES(pump_state),
                fan_state=VALUES(fan_state),
                light_state=VALUES(light_state),
                manual_mode=VALUES(manual_mode),
                light_threshold=VALUES(light_threshold),
                pump_threshold=VALUES(pump_threshold),
                fan_temp_threshold=VALUES(fan_temp_threshold),
                fan_co2_threshold=VALUES(fan_co2_threshold),
                update_time=NOW()
                """
                
                cursor.execute(status_sql, (
                    '69568516c00ccb6d4b302187_esp32-001',
                    pump,
                    fan,
                    light,
                    manual,
                    light_threshold,
                    pump_threshold,
                    fan_temp_threshold,
                    fan_co2_threshold
                ))
                
                conn.commit()
                
                print(f"数据已保存到数据库 - temp:{temp}°C, humi:{humi}%RH, soil:{soil_percentage}%")
                
                return {
                    "statusCode": 200,
                    "body": json.dumps({
                        "message": "数据保存成功",
                        "data": {
                            "temperature": temp,
                            "humidity": humi,
                            "soil_moisture": soil_percentage,
                            "light_intensity": light_lux,
                            "eco2": eco2,
                            "tvoc": tvoc
                        }
                    }, ensure_ascii=False)
                }
        
        finally:
            conn.close()
    
    except Exception as e:
        print(f"处理失败: {str(e)}")
        import traceback
        traceback.print_exc()
        
        return {
            "statusCode": 500,
            "body": json.dumps({
                "message": "处理失败",
                "error": str(e)
            }, ensure_ascii=False)
        }
