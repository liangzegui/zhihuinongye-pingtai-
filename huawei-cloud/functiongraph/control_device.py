# -*- coding:utf-8 -*-
"""
华为云FunctionGraph函数 - 调用IoTDA北向API下发控制命令到ESP32

环境变量配置（在FunctionGraph控制台设置）:
- IOTDA_ENDPOINT: https://iotda.cn-south-1.myhuaweicloud.com
- IOTDA_PROJECT_ID: 华为云项目ID
- IOTDA_AK: AccessKey
- IOTDA_SK: SecretKey
- DEVICE_ID: 69568516c00ccb6d4b302187_esp32-001

触发器: API Gateway (前端调用)
"""

import json
import os
import hmac
import hashlib
import datetime
import requests
from urllib.parse import quote

def handler(event, context):
    """
    接收前端控制请求，下发到IoTDA
    
    请求格式:
    {
        "action": "control",  // 或 "setThreshold"
        "pump": true,         // 可选
        "fan": false,         // 可选
        "light": true,        // 可选
        "manual": true,       // 可选
        "fanTempThreshold": 28,      // 可选
        "fanCO2Threshold": 800,       // 可选
        "pumpDroughtThreshold": 3000, // 可选
        "lightLuxThreshold": 1000     // 可选
    }
    """
    try:
        # 解析请求
        if isinstance(event.get('body'), str):
            body = json.loads(event['body'])
        else:
            body = event.get('body', {})
        
        print(f"收到控制请求: {json.dumps(body, ensure_ascii=False)}")
        
        # 构建属性设置
        properties = {}
        
        # 设备控制
        if 'pump' in body:
            properties['pump'] = body['pump']
        if 'fan' in body:
            properties['fan'] = body['fan']
        if 'light' in body:
            properties['light'] = body['light']
        if 'manual' in body:
            properties['manual'] = body['manual']
        
        # 阈值设置
        if 'fanTempThreshold' in body:
            properties['fanTempThreshold'] = int(body['fanTempThreshold'])
        if 'fanCO2Threshold' in body:
            properties['fanCO2Threshold'] = int(body['fanCO2Threshold'])
        if 'pumpDroughtThreshold' in body:
            properties['pumpDroughtThreshold'] = int(body['pumpDroughtThreshold'])
        if 'lightLuxThreshold' in body:
            properties['lightLuxThreshold'] = int(body['lightLuxThreshold'])
        
        if not properties:
            return {
                "statusCode": 400,
                "body": json.dumps({"message": "未提供控制参数"}, ensure_ascii=False)
            }
        
        # 调用IoTDA北向API
        device_id = os.environ['DEVICE_ID']
        result = set_device_properties(device_id, properties)
        
        if result['success']:
            return {
                "statusCode": 200,
                "body": json.dumps({
                    "message": "控制命令已下发",
                    "properties": properties
                }, ensure_ascii=False)
            }
        else:
            return {
                "statusCode": 500,
                "body": json.dumps({
                    "message": "下发失败",
                    "error": result['error']
                }, ensure_ascii=False)
            }
    
    except Exception as e:
        print(f"控制失败: {str(e)}")
        import traceback
        traceback.print_exc()
        
        return {
            "statusCode": 500,
            "body": json.dumps({
                "message": "控制失败",
                "error": str(e)
            }, ensure_ascii=False)
        }


def set_device_properties(device_id, properties):
    """
    调用IoTDA北向API设置设备属性
    """
    try:
        endpoint = os.environ['IOTDA_ENDPOINT']
        project_id = os.environ['IOTDA_PROJECT_ID']
        ak = os.environ['IOTDA_AK']
        sk = os.environ['IOTDA_SK']
        
        # 构建请求
        method = 'PUT'
        uri = f'/v5/iot/{project_id}/devices/{device_id}/properties'
        url = f"{endpoint}{uri}"
        
        # 请求体
        payload = {
            "services": [{
                "service_id": "default",
                "properties": properties
            }]
        }
        
        body = json.dumps(payload)
        
        # 生成华为云签名
        headers = generate_huawei_signature(method, uri, ak, sk, body)
        headers['Content-Type'] = 'application/json'
        
        print(f"调用IoTDA API: {url}")
        print(f"请求体: {body}")
        
        # 发送请求
        response = requests.request(
            method=method,
            url=url,
            headers=headers,
            data=body,
            timeout=10
        )
        
        print(f"IoTDA响应: {response.status_code} - {response.text}")
        
        if response.status_code == 200:
            return {"success": True, "data": response.json()}
        else:
            return {"success": False, "error": response.text}
    
    except Exception as e:
        print(f"API调用异常: {str(e)}")
        return {"success": False, "error": str(e)}


def generate_huawei_signature(method, uri, ak, sk, body=''):
    """
    生成华为云API签名（简化版）
    实际生产环境建议使用华为云官方SDK
    """
    # 时间戳
    now = datetime.datetime.utcnow()
    timestamp = now.strftime('%Y%m%dT%H%M%SZ')
    date = now.strftime('%Y%m%d')
    
    # Canonical Request
    canonical_headers = f'content-type:application/json\nhost:iotda.cn-south-1.myhuaweicloud.com\nx-sdk-date:{timestamp}\n'
    signed_headers = 'content-type;host;x-sdk-date'
    
    # Body hash
    body_hash = hashlib.sha256(body.encode('utf-8')).hexdigest()
    
    canonical_request = f"{method}\n{uri}\n\n{canonical_headers}\n{signed_headers}\n{body_hash}"
    
    # String to Sign
    algorithm = 'SDK-HMAC-SHA256'
    credential_scope = f'{date}/cn-south-1/iotda/sdk_request'
    canonical_request_hash = hashlib.sha256(canonical_request.encode('utf-8')).hexdigest()
    string_to_sign = f"{algorithm}\n{timestamp}\n{credential_scope}\n{canonical_request_hash}"
    
    # Signature
    k_date = hmac.new(f'SDK{sk}'.encode('utf-8'), date.encode('utf-8'), hashlib.sha256).digest()
    k_region = hmac.new(k_date, 'cn-south-1'.encode('utf-8'), hashlib.sha256).digest()
    k_service = hmac.new(k_region, 'iotda'.encode('utf-8'), hashlib.sha256).digest()
    k_signing = hmac.new(k_service, 'sdk_request'.encode('utf-8'), hashlib.sha256).digest()
    signature = hmac.new(k_signing, string_to_sign.encode('utf-8'), hashlib.sha256).hexdigest()
    
    # Authorization Header
    authorization = f'{algorithm} Access={ak}, SignedHeaders={signed_headers}, Signature={signature}'
    
    return {
        'Authorization': authorization,
        'X-Sdk-Date': timestamp,
        'Host': 'iotda.cn-south-1.myhuaweicloud.com'
    }
