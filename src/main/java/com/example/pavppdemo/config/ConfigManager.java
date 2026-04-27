package com.example.pavppdemo.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    // 默认配置值
    private static final Map<String, String> defaultConfig = new HashMap<String, String>() {
        {
            put("SANDBOX_SITE", "https://centinelapistag.cardinalcommerce.com/");
            put("PROD_SITE", "https://centinelapi.cardinalcommerce.com/");
            put("FIDO_INIT", "V2/FIDO/Init");
            put("FIDO_CHALLENGE", "V2/FIDO/Challenge");
            put("JWT_API_KEY_ID", "61a79c46a8bd2d6dd6ab521a");
            put("JWT_ORG_UNIT_ID", "61a79c46a8bd2d6dd6ab5219");
            put("JWT_SECRET", "af3aeed1-bac4-41f6-93e8-050eed0e1484");
            put("merchant_id", "sean_sandbox_1730710277");
            put("key_id", "ba8642fe-2969-4fa4-bb0c-06b33b2181de");
            put("shared_secret_key", "BOl66V7NsFqJA099GcQE0JbUyb8Arh/hFfsPcdJbP3s=");
            put("CYBS_CAS_SITE", "apitest.cybersource.com");
            put("CYBS_PRD_SITE", "api.cybersource.com");
            put("PA_SETUP", "/risk/v1/authentication-setups");
            put("PA_ENROLL", "/risk/v1/authentications/");
            put("PA_VALIDATE", "/risk/v1/authentication-results/");
            put("MERCHANT_ORIGIN", "https://demo.sean.io");
            put("RETURN_URL", "https://localhost:8443/callback");
        }
    };

    // 内存中的配置存储
    private static Map<String, String> currentConfig = new HashMap<>(defaultConfig);

    // 读取配置
    public static Map<String, String> readConfig() {
        return new HashMap<>(currentConfig);
    }

    // 写入配置（仅在当前会话中保存）
    public static boolean writeConfig(Map<String, String> config) {
        try {
            currentConfig = new HashMap<>(config);
            return true;
        } catch (Exception e) {
            System.err.println("更新配置失败: " + e.getMessage());
            throw e;
        }
    }

    // 检查配置完整性
    public static boolean checkConfigComplete() {
        try {
            Map<String, String> config = readConfig();
            String[] requiredFields = {
                "SANDBOX_SITE", "PROD_SITE", "FIDO_INIT", "FIDO_CHALLENGE",
                "JWT_API_KEY_ID", "JWT_ORG_UNIT_ID", "JWT_SECRET", 
                "merchant_id", "key_id", "shared_secret_key",
                "CYBS_CAS_SITE", "CYBS_PRD_SITE", "PA_SETUP", "PA_ENROLL", "PA_VALIDATE",
                "MERCHANT_ORIGIN", "RETURN_URL"
            };
            
            for (String field : requiredFields) {
                if (!config.containsKey(field) || config.get(field) == null || config.get(field).isEmpty()) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}