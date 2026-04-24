package com.example.pavppdemo.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JwtManager {

    // 生成FIDO_INIT请求的JWT
    public static Map<String, Object> generateFidoInitJWT(Map<String, String> config) {
        try {
            // 生成请求UUID
            String jti = UUID.randomUUID().toString();
            // 获取当前时间戳（秒级）
            long iat = System.currentTimeMillis() / 1000;
            
            // JWT Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("iss", config.get("JWT_API_KEY_ID"));
            payload.put("jti", jti);
            payload.put("iat", iat);
            payload.put("OrgUnitId", config.get("JWT_ORG_UNIT_ID"));
            payload.put("ReturnUrl", config.get("RETURN_URL"));
            payload.put("ObjectifyPayload", true);
            
            Map<String, String> innerPayload = new HashMap<>();
            innerPayload.put("MerchantOrigin", config.get("MERCHANT_ORIGIN"));
            payload.put("Payload", innerPayload);
            
            // 生成JWS（签名）
            String jws = Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setHeaderParam("alg", "HS256")
                    .setClaims(payload)
                    .signWith(SignatureAlgorithm.HS256, config.get("JWT_SECRET").getBytes())
                    .compact();
            
            Map<String, Object> result = new HashMap<>();
            result.put("jws", jws);
            result.put("payload", payload);
            result.put("jti", jti);
            
            return result;
        } catch (Exception e) {
            System.err.println("生成JWT失败: " + e.getMessage());
            throw e;
        }
    }

    // 验证JWT应答
    public static Claims verifyJWT(String jws, String secret) {
        try {
            // 验签
            Claims decoded = Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(jws)
                    .getBody();
            return decoded;
        } catch (Exception e) {
            System.err.println("JWT验签失败: " + e.getMessage());
            throw e;
        }
    }

    // 解析JWT应答Payload
    public static Map<String, Object> parseJWTResponse(Claims decoded) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("iss", decoded.get("iss"));
            result.put("iat", decoded.get("iat"));
            result.put("exp", decoded.get("exp"));
            result.put("jti", decoded.get("jti"));
            result.put("aud", decoded.get("aud"));
            result.put("Payload", decoded.get("Payload"));
            
            return result;
        } catch (Exception e) {
            System.err.println("解析JWT应答失败: " + e.getMessage());
            throw e;
        }
    }

}