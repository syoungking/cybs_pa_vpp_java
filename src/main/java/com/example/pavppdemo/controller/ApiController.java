package com.example.pavppdemo.controller;

import com.example.pavppdemo.config.ConfigManager;
import com.example.pavppdemo.jwt.JwtManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ApiController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 获取配置
    @GetMapping("/api/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            Map<String, String> config = ConfigManager.readConfig();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 保存配置
    @PostMapping("/api/config")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody Map<String, String> config) {
        try {
            ConfigManager.writeConfig(config);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "配置保存成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 检查健康状态
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isComplete = ConfigManager.checkConfigComplete();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configComplete", isComplete);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 生成JWT
    @PostMapping("/api/jwt/generate")
    public ResponseEntity<Map<String, Object>> generateJwt() {
        try {
            Map<String, String> config = ConfigManager.readConfig();
            Map<String, Object> result = JwtManager.generateFidoInitJWT(config);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 验证JWT
    @PostMapping("/api/jwt/verify")
    public ResponseEntity<Map<String, Object>> verifyJwt(@RequestBody Map<String, String> request) {
        try {
            String jws = request.get("jws");
            Map<String, String> config = ConfigManager.readConfig();
            Claims decoded = JwtManager.verifyJWT(jws, config.get("JWT_SECRET"));
            Map<String, Object> parsed = JwtManager.parseJWTResponse(decoded);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", parsed);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 处理回调
    @GetMapping("/callback")
    public String callback(@RequestParam("JWT") String jwt) {
        try {
            if (jwt == null || jwt.isEmpty()) {
                return "Missing JWT parameter";
            }
            
            Map<String, String> config = ConfigManager.readConfig();
            Claims decoded = JwtManager.verifyJWT(jwt, config.get("JWT_SECRET"));
            Map<String, Object> parsed = JwtManager.parseJWTResponse(decoded);
            String parsedJson = objectMapper.writeValueAsString(parsed);
            
            return "<!DOCTYPE html>\n" +
                   "<html>\n" +
                   "<head>\n" +
                   "  <title>Callback</title>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "  <script>\n" +
                   "    window.parent.postMessage({ type: 'JWT_RESPONSE', data: " + parsedJson + ", jws: '" + jwt + "' }, '*');\n" +
                   "  </script>\n" +
                   "</body>\n" +
                   "</html>";
        } catch (Exception e) {
            return "Callback error: " + e.getMessage();
        }
    }

    // 处理DDC回调（GET方式）
    @GetMapping("/ddc-callback")
    public String ddcCallbackGet(@RequestParam("JWT") String jwt) {
        try {
            if (jwt == null || jwt.isEmpty()) {
                return "Missing JWT parameter";
            }
            
            Map<String, String> config = ConfigManager.readConfig();
            Claims decoded = JwtManager.verifyJWT(jwt, config.get("JWT_SECRET"));
            Map<String, Object> payload = new HashMap<>();
            payload.put("iss", decoded.get("iss"));
            payload.put("iat", decoded.get("iat"));
            payload.put("exp", decoded.get("exp"));
            payload.put("jti", decoded.get("jti"));
            payload.put("aud", decoded.get("aud"));
            payload.put("Payload", decoded.get("Payload"));
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            return "<!DOCTYPE html>\n" +
                   "<html>\n" +
                   "<head>\n" +
                   "  <title>DDC Callback</title>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "  <script>\n" +
                   "    window.parent.postMessage({ type: 'DDC_RESPONSE', payload: " + payloadJson + " }, '*');\n" +
                   "  </script>\n" +
                   "</body>\n" +
                   "</html>";
        } catch (Exception e) {
            return "DDC callback error: " + e.getMessage();
        }
    }

    // 处理DDC回调（POST方式）
    @PostMapping("/ddc-callback")
    public String ddcCallbackPost(@RequestBody Map<String, String> request) {
        try {
            String jwt = request.get("JWT");
            if (jwt == null || jwt.isEmpty()) {
                return "Missing JWT parameter";
            }
            
            Map<String, String> config = ConfigManager.readConfig();
            Claims decoded = JwtManager.verifyJWT(jwt, config.get("JWT_SECRET"));
            Map<String, Object> payload = new HashMap<>();
            payload.put("iss", decoded.get("iss"));
            payload.put("iat", decoded.get("iat"));
            payload.put("exp", decoded.get("exp"));
            payload.put("jti", decoded.get("jti"));
            payload.put("aud", decoded.get("aud"));
            payload.put("Payload", decoded.get("Payload"));
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            return "<!DOCTYPE html>\n" +
                   "<html>\n" +
                   "<head>\n" +
                   "  <title>DDC Callback</title>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "  <script>\n" +
                   "    window.parent.postMessage({ type: 'DDC_RESPONSE', payload: " + payloadJson + " }, '*');\n" +
                   "  </script>\n" +
                   "</body>\n" +
                   "</html>";
        } catch (Exception e) {
            return "DDC callback error: " + e.getMessage();
        }
    }

}