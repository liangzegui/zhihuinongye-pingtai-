package com.example.agribackend.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 华为云API签名工具类
 * 实现SDK-HMAC-SHA256签名算法
 * 参考文档: https://support.huaweicloud.com/devg-apisign/api-sign-sdk.html
 */
public class HuaweiCloudSigner {

    private static final String ALGORITHM = "SDK-HMAC-SHA256";
    private static final String TERMINATOR = "sdk_request";

    /**
     * 对请求进行签名
     *
     * @param method  HTTP方法 (GET, POST, PUT, DELETE)
     * @param host    主机名 (如 iotda.cn-south-1.myhuaweicloud.com)
     * @param uri     请求路径 (如 /v5/iot/xxx/devices/xxx/properties)
     * @param query   查询参数 (可为空)
     * @param headers 请求头 (会被添加签名相关头)
     * @param body    请求体 (可为空)
     * @param ak      Access Key
     * @param sk      Secret Key
     * @param region  区域 (如 cn-south-1)
     * @param service 服务名 (如 iotda)
     * @return 添加了签名的请求头
     */
    public static Map<String, String> sign(String method, String host, String uri, String query,
            Map<String, String> headers, String body,
            String ak, String sk, String region, String service) {
        if (headers == null) {
            headers = new HashMap<>();
        }

        // 1. 生成时间戳
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String date = timestamp.substring(0, 8);

        // 2. 添加必要的请求头
        headers.put("Host", host);
        headers.put("X-Sdk-Date", timestamp);
        headers.put("Content-Type", "application/json");

        // 3. 计算body哈希
        String bodyHash = sha256Hex(body != null ? body : "");

        // 4. 构建规范请求 (Canonical Request)
        String signedHeaders = "content-type;host;x-sdk-date";
        String canonicalHeaders = String.format("content-type:%s\nhost:%s\nx-sdk-date:%s\n",
                headers.get("Content-Type"), host, timestamp);

        String canonicalRequest = String.join("\n",
                method.toUpperCase(),
                uri,
                query != null ? query : "",
                canonicalHeaders,
                signedHeaders,
                bodyHash);

        // 5. 构建待签名字符串 (String to Sign)
        String credentialScope = String.format("%s/%s/%s/%s", date, region, service, TERMINATOR);
        String stringToSign = String.join("\n",
                ALGORITHM,
                timestamp,
                credentialScope,
                sha256Hex(canonicalRequest));

        // 6. 计算签名密钥
        byte[] kDate = hmacSha256(("SDK" + sk).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        byte[] kSigning = hmacSha256(kService, TERMINATOR);

        // 7. 计算签名
        String signature = bytesToHex(hmacSha256(kSigning, stringToSign));

        // 8. 构建Authorization头
        String authorization = String.format("%s Access=%s, SignedHeaders=%s, Signature=%s",
                ALGORITHM, ak, signedHeaders, signature);

        headers.put("Authorization", authorization);

        return headers;
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed", e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 calculation failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
