package com.example.pavppdemo.controller;

import com.example.pavppdemo.config.ConfigManager;
import com.example.pavppdemo.config.RestClient;
import com.example.pavppdemo.jwt.JwtManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.net.URLDecoder;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ApiController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储最新的回调内容
    private String latestCallbackMessage = null;
    private String latestCallbackType = null;

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

    // 设置等待回调类型（FIDO/Init 或 FIDO/Challenge）
    @PostMapping("/api/setWaitingCallback")
    public ResponseEntity<Map<String, Object>> setWaitingCallback(@RequestBody Map<String, String> request) {
        try {
            String callbackType = request.get("type");
            this.latestCallbackMessage = null;
            this.latestCallbackType = callbackType;
            System.out.println("Set waiting callback type: " + callbackType);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Waiting callback type set to: " + callbackType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

    // PA Setup
    @PostMapping("/api/pa/setup")
    public ResponseEntity<Map<String, Object>> paSetup(@RequestBody Map<String, Object> request) {
        try {
            Map<String, String> config = ConfigManager.readConfig();
            RestClient restClient = new RestClient(config);

            boolean isSandbox = request.get("environment").equals("sandbox");
            String requestBody = objectMapper.writeValueAsString(request.get("data"));
            String endpoint = config.get("PA_SETUP");

            String response = restClient.sendPostRequest(endpoint, requestBody, isSandbox);

            Map<String, Object> responseData = objectMapper.readValue(response, Map.class);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", responseData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // PA Enroll
    @PostMapping("/api/pa/enroll")
    public ResponseEntity<Map<String, Object>> paEnroll(@RequestBody Map<String, Object> request) {
        try {
            Map<String, String> config = ConfigManager.readConfig();
            RestClient restClient = new RestClient(config);

            boolean isSandbox = request.get("environment").equals("sandbox");
            String requestBody = objectMapper.writeValueAsString(request.get("data"));
            String endpoint = config.get("PA_ENROLL");

            String response = restClient.sendPostRequest(endpoint, requestBody, isSandbox);

            Map<String, Object> responseData = objectMapper.readValue(response, Map.class);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", responseData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // PA Validate
    @PostMapping("/api/pa/validate")
    public ResponseEntity<Map<String, Object>> paValidate(@RequestBody Map<String, Object> request) {
        try {
            Map<String, String> config = ConfigManager.readConfig();
            RestClient restClient = new RestClient(config);

            boolean isSandbox = request.get("environment").equals("sandbox");
            String requestBody = objectMapper.writeValueAsString(request.get("data"));
            String endpoint = config.get("PA_VALIDATE");

            String response = restClient.sendPostRequest(endpoint, requestBody, isSandbox);

            Map<String, Object> responseData = objectMapper.readValue(response, Map.class);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", responseData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 获取IP地址
    @GetMapping("/api/ip")
    public ResponseEntity<Map<String, Object>> getIpAddress(javax.servlet.http.HttpServletRequest request) {
        try {
            String ip = getClientIpAddress(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ip", ip);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 获取客户端IP地址
    private String getClientIpAddress(javax.servlet.http.HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        String xRealIpHeader = request.getHeader("X-Real-IP");
        if (xRealIpHeader != null) {
            return xRealIpHeader;
        }
        return request.getRemoteAddr();
    }

    // 处理FIDO/Init回调
    @RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST}, consumes = {"*/*"})
    public String callback(
            @RequestParam(value = "Response", required = false) String responseParam,
            HttpServletRequest request) {
        System.out.println("=== Callback method called ===");
        System.out.println("Request method: " + request.getMethod());
        System.out.println("Current callback type: " + this.latestCallbackType);

        String response = responseParam;

        // 处理POST请求中的payload
        if ("POST".equals(request.getMethod())) {
            try {
                // 读取请求体
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String requestBody = sb.toString();
                System.out.println("POST request body: " + requestBody);

                // 尝试解析表单数据
                if (requestBody != null && !requestBody.isEmpty()) {
                    // 检查是否是表单数据格式
                    if (requestBody.contains("=")) {
                        String[] pairs = requestBody.split("&");
                        for (String pair : pairs) {
                            String[] keyValue = pair.split("=", 2);
                            if (keyValue.length == 2) {
                                System.out.println("Key: " + keyValue[0] + ", Value: " + keyValue[1]);
                                if ("Response".equals(keyValue[0])) {
                                    // 优先使用POST请求体中的Response参数
                                    response = URLDecoder.decode(keyValue[1], "UTF-8");
                                    System.out.println("Decoded Response: " + response);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error processing POST request: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Final response: " + response);

        // 存储最新的回调内容和类型
        this.latestCallbackMessage = response;
        System.out.println("Stored latest callback message: " + response + ", type: " + this.latestCallbackType);

        // 构建HTML页面
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<title>FIDO Callback</title>" +
                "<meta http-equiv='Content-Security-Policy' content='default-src *; script-src *; style-src *; img-src *; connect-src *; frame-src *;'>" +
                "<script>" +
                "window.onload = function() {" +
                "  console.log('Callback page loaded');" +
                "  var serverResponse = '" + (response != null ? response.replace("'", "\\'") : "") + "';" +
                "  console.log('Server response:', serverResponse);" +
                "  if (serverResponse) {" +
                "    console.log('Closing callback window');" +
                "    window.close();" +
                "  } else {" +
                "    console.log('No response found');" +
                "    window.location.href = '/';" +
                "  }" +
                "}" +
                "</script>" +
                "</head>" +
                "<body>" +
                "<h1>FIDO Callback</h1>" +
                "<p>Processing callback...</p>" +
                "<p>Response: " + (response != null ? response : "No response") + "</p>" +
                "</body>" +
                "</html>";

        return html;
    }

    // 处理OPTIONS预检请求
    @RequestMapping(value = "/callback", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }

    // 获取最新的回调内容
    @GetMapping("/getLatestMessage")
    public ResponseEntity<Map<String, Object>> getLatestMessage() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", this.latestCallbackMessage);
            response.put("type", this.latestCallbackType);
            System.out.println("=== getLatestMessage called ===");
            System.out.println("Returning message: " + this.latestCallbackMessage);
            System.out.println("Returning type: " + this.latestCallbackType);
            // 只有在消息不为null时才清空
            if (this.latestCallbackMessage != null) {
                System.out.println("Clearing callback message and type");
                this.latestCallbackMessage = null;
                this.latestCallbackType = null;
            } else {
                System.out.println("Message is null, keeping callback type: " + this.latestCallbackType);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
