package com.example.pavppdemo.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RestClient {

    private final Map<String, String> config;

    public RestClient(Map<String, String> config) {
        this.config = config;
    }

    public String sendPostRequest(String endpoint, String requestBody, boolean isSandbox) throws Exception {
        String url = buildUrl(endpoint, isSandbox);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Generate request headers
        String date = getRfc1123Date();
        String host = isSandbox ? config.get("CYBS_CAS_SITE") : config.get("CYBS_PRD_SITE");
        String merchantId = config.get("merchant_id");
        String digest = generateDigest(requestBody);
        String signature = generateSignature("POST", endpoint, date, host, merchantId, digest);

        // Set headers
        connection.setRequestProperty("v-c-merchant-id", merchantId);
        connection.setRequestProperty("Date", date);
        connection.setRequestProperty("Host", host);
        connection.setRequestProperty("Digest", digest);
        connection.setRequestProperty("Signature", signature);

        // Send request body
        connection.getOutputStream().write(requestBody.getBytes("UTF-8"));

        // Read response
        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300 ? 
            connection.getInputStream() : connection.getErrorStream();
        String response = readInputStream(inputStream);

        connection.disconnect();
        return response;
    }

    public String sendGetRequest(String endpoint, boolean isSandbox) throws Exception {
        String url = buildUrl(endpoint, isSandbox);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        // Generate request headers
        String date = getRfc1123Date();
        String host = isSandbox ? config.get("CYBS_CAS_SITE") : config.get("CYBS_PRD_SITE");
        String merchantId = config.get("merchant_id");
        String signature = generateSignature("GET", endpoint, date, host, merchantId, null);

        // Set headers
        connection.setRequestProperty("v-c-merchant-id", merchantId);
        connection.setRequestProperty("Date", date);
        connection.setRequestProperty("Host", host);
        connection.setRequestProperty("Signature", signature);

        // Read response
        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300 ? 
            connection.getInputStream() : connection.getErrorStream();
        String response = readInputStream(inputStream);

        connection.disconnect();
        return response;
    }

    private String buildUrl(String endpoint, boolean isSandbox) {
        String baseUrl = isSandbox ? config.get("CYBS_CAS_SITE") : config.get("CYBS_PRD_SITE");
        return "https://" + baseUrl + endpoint;
    }

    private String getRfc1123Date() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    private String generateDigest(String requestBody) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(requestBody.getBytes());
        String base64Hash = Base64.getEncoder().encodeToString(hash);
        return "SHA-256=" + base64Hash;
    }

    private String generateSignature(String method, String endpoint, String date, String host, String merchantId, String digest) throws Exception {
        String keyId = config.get("key_id");
        String sharedSecret = config.get("shared_secret_key");

        // Build signing string
        StringBuilder signingString = new StringBuilder();
        signingString.append("host: ").append(host).append("\n");
        signingString.append("date: ").append(date).append("\n");
        signingString.append("request-target: " + method.toLowerCase() + " " + endpoint).append("\n");
        if (digest != null) {
            signingString.append("digest: ").append(digest).append("\n");
        }
        signingString.append("v-c-merchant-id: " + merchantId);

        // Generate HMAC-SHA256 signature
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(sharedSecret), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(signingString.toString().getBytes("UTF-8"));
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        // Build signature header
        StringBuilder signatureHeader = new StringBuilder();
        signatureHeader.append("keyid=\"").append(keyId).append("\", ");
        signatureHeader.append("algorithm=\"HmacSHA256\", ");
        if (digest != null) {
            signatureHeader.append("headers=\"host date request-target digest v-c-merchant-id\", ");
        } else {
            signatureHeader.append("headers=\"host date request-target v-c-merchant-id\", ");
        }
        signatureHeader.append("signature=\"").append(signatureBase64).append("\"");

        return signatureHeader.toString();
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
}
