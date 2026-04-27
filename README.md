# PA+VPP Demo Project

## Project Introduction

This project is a dynamic demo website for PA+VPP payment authentication flows, implemented using pure HTML + JavaScript frontend with Java backend. The project supports FIDO/Init, PA Setup, DDC (Device Data Collection), VPP Enrollment Flow, and VPP Authentication Flow processes.

## Technology Stack

- **Frontend**: HTML5, JavaScript (ES6+), CSS3
- **Backend**: Java, Spring Boot, Spring Security, JWT
- **Dependencies**: JWT (HS256 algorithm), REST API calls, cross-origin, iframe embedding

## Environment Setup

### 1. Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

### 2. Build the Project

```bash
mvn clean package
```

### Configuration

The configuration is managed in-memory by the `ConfigManager` class. The default configuration values are:

```
SANDBOX_SITE=https://centinelapistag.cardinalcommerce.com/
PROD_SITE=https://centinelapi.cardinalcommerce.com/
FIDO_INIT=V2/FIDO/Init
FIDO_CHALLENGE=V2/FIDO/Challenge
JWT_API_KEY_ID=61a79c46a8bd2d6dd6ab521a
JWT_ORG_UNIT_ID=61a79c46a8bd2d6dd6ab5219
JWT_SECRET=af3aeed1-bac4-41f6-93e8-050eed0e1484
merchant_id=sean_sandbox_1730710277
key_id=ba8642fe-2969-4fa4-bb0c-06b33b2181de
shared_secret_key=BOl66V7NsFqJA099GcQE0JbUyb8Arh/hFfsPcdJbP3s=
CYBS_CAS_SITE=apitest.cybersource.com
CYBS_PRD_SITE=api.cybersource.com
PA_SETUP=/risk/v1/authentication-setups
PA_ENROLL=/risk/v1/authentications/
PA_VALIDATE=/risk/v1/authentication-results/
MERCHANT_ORIGIN=https://demo.sean.io
RETURN_URL=https://localhost:8443/callback
```

**Note**: You can modify these configuration values through the configuration page in the frontend. Ensure that `MERCHANT_ORIGIN` and `RETURN_URL` are set correctly to match your environment.

## Startup Steps

### Start the Server

```bash
java -jar target/pa-vpp-demo-1.1.0.jar
```

The server will run on **HTTPS port 8443** and **HTTP port 8080** (automatically redirects to HTTPS).

### Access the Project

Open a browser and navigate to `https://localhost:8443`:

- The homepage displays the main functionality interface
- You can select the environment (Sandbox/Production) and follow the steps to execute the complete flow

## HTTPS Configuration

The project is configured with HTTPS support using a self-signed SSL certificate.

### SSL Certificate Details

- **Keystore Path**: `src/main/resources/ssl/keystore.jks`
- **Keystore Password**: `password`
- **Key Alias**: `tomcat`
- **Key Password**: `password`
- **Validity**: 365 days

### Port Configuration

- **HTTPS Port**: 8443 (main access port)
- **HTTP Port**: 8080 (redirects to HTTPS)

**Note**: When accessing the site for the first time, your browser will display a security warning because the certificate is self-signed. Click "Continue" or "Advanced" to proceed to the site.

## Feature Description

### 1. Configuration Management

- View and edit all configuration items
- Configuration is stored in memory during the server runtime
- Configuration page is available for modifying settings at any time

### 2. Card Information Management

- Support for manual input of card number and expiration date
- Real-time card number format validation and Luhn check
- Automatic removal of non-numeric characters from card numbers

### 3. FIDO/Init Flow

1. Select environment (Sandbox/Production)
2. Click "FIDO Init" button
3. System generates JWT and submits via iframe to Cardinal Commerce
4. Receive / Paste callback and verify JWT (with local tunneling tool)
5. Display request and response information
6. Parse ReferenceId from Response

### 4. PA Setup Flow

1. Parse FIDO/Init Response to get ReferenceId
2. Click "Generate PA Setup request" button
3. System generates PA Setup request
4. Receive PA Setup Response and show in text box
   1. fidoFlowType=ENROLLMENT, move to VPP Enrollment Flow
   2. fidoFlowType=AUTHENTICATION, move to VPP Authentication Flow

### 5. DDC (Device Data Collection) Flow

1. Click "Run DDC" button
2. System submits form via hidden iframe to Cardinal Commerce
3. System sets 10-second timeout, displays "Device information collection timeout" if exceeded
4. After receiving callback, determine based on ActionCode:
   - ActionCode=SUCCESS: Display "Device information collection successful"
   - Other cases: Display "Device information collection failed"

### 6. VPP Enrollment Flow

1. Click "Generate PA Enroll request for VPP Enrollment Flow" button
2. System generates PA Enroll request
3. Receive PA Enroll Response and show in text box
4. Click "StepUp" button
5. System submits StepUp request via iframe
6. Click "Generate PA Validate request" button
7. System generates PA Validate request
8. Receive PA Validate Response and show in text box
9. If authentication is successful, display "Authentication Successful" and show "FIDO Challenge" button
10. Click "FIDO Challenge" button to execute FIDO Challenge flow
11. Receive / Paste callback and verify JWT (with local tunneling tool)

### 7. VPP Authentication Flow

1. Click "Execute FIDO Challenge" button
2. System executes FIDO Challenge flow
3. Receive / Paste callback and verify JWT (with local tunneling tool)
4. Click "Generate PA Enroll Request" button
5. System generates PA Enroll request
6. Receive PA Enroll Response and show in text box

### 8. Error Handling

- JWT generation failure: Display error message
- API call timeout/failure: Display error message
- JWT verification failure: Display error message
- Callback reception exception: Display error message
- Configuration missing: Prompt user to enter configuration page

### 9. Polling Mechanism

- Implements a polling mechanism to check for callback responses
- Maximum polling attempts: 60 times (approximately 2 minutes)
- Displays timeout message if no callback is received within the limit

### 10. Popup Window Management

- Displays success notification popup when callback is received
- Automatically closes FIDO Challenge popup window after callback
- Popup windows have animated transitions for better user experience

## Project Structure

```
pa-vpp-demo-java/
├── src/
│   ├── main/
│   │   ├── java/com/example/pavppdemo/  # Java source code
│   │   │   ├── config/                  # Configuration classes
│   │   │   │   ├── ConfigManager.java   # Configuration management
│   │   │   │   ├── HttpsConfig.java     # HTTPS configuration
│   │   │   │   ├── SecurityConfig.java  # Security configuration
│   │   │   │   └── WebConfig.java       # Web MVC configuration
│   │   │   ├── controller/              # API controllers
│   │   │   │   └── ApiController.java   # REST API endpoints
│   │   │   ├── jwt/                     # JWT management
│   │   │   │   └── JwtManager.java      # JWT operations
│   │   │   └── PaVppDemoApplication.java # Application entry point
│   │   └── resources/
│   │       ├── ssl/                     # SSL certificates
│   │       │   └── keystore.jks         # Self-signed certificate
│   │       ├── static/                  # Frontend static files
│   │       │   ├── css/                 # Style files
│   │       │   ├── js/                  # JavaScript files
│   │       │   ├── index.html           # Main page
│   │       │   ├── ddc-form.html        # DDC form page
│   │       │   └── fido-form.html       # FIDO form page
│   │       └── application.properties   # Application configuration
├── pom.xml                              # Maven configuration
└── README.md                            # Project documentation
```

## Troubleshooting

### Common Issues and Solutions

1. **Callback Failure**
   - Ensure `MERCHANT_ORIGIN` and `RETURN_URL` are correctly configured
   - Check that the server is accessible from the internet if using external services
   - Verify that HTTPS is properly configured
2. **SSL Certificate Warnings**
   - The project uses a self-signed certificate for development
   - In production, use a certificate from a trusted CA
   - To bypass the warning in development, click "Advanced" and "Proceed" in your browser
3. **JWT Verification Failures**
   - Ensure the `JWT_SECRET` is correctly configured
   - Verify that the JWT format is correct
   - Check the system time on the server (JWT has expiration times)
4. **API Connection Issues**
   - Verify network connectivity to Cardinal Commerce and CyberSource endpoints
   - Check firewall settings that may block outbound requests
   - Ensure correct environment (sandbox/production) is selected
5. **Device Data Collection (DDC) Timeouts**
   - The system has a 10-second timeout for DDC
   - If consistently timing out, check network connectivity
   - Verify that the DDC URL is accessible

## Notes

1. This project uses a self-signed SSL certificate for HTTPS. In production environments, use official certificates from a trusted Certificate Authority (CA).
2. Ensure `MERCHANT_ORIGIN` and `RETURN_URL` are configured correctly, otherwise callback failures may occur.
3. This project is for demo purposes only. Use appropriate security measures in production environments.
4. When entering card numbers, the system automatically removes non-numeric characters and performs Luhn validation to ensure correct card number format.
5. If you need to automatically receive callback information from external services (like Cardinal Commerce), you should use a local tunneling tool such as ngrok, frp, or localtunnel to expose your local server to the internet. This will allow external services to send callbacks to your local development environment.
6. If you need to regenerate the SSL certificate, use the following keytool command:

```bash
keytool -genkey -alias tomcat -keyalg RSA -keystore src/main/resources/ssl/keystore.jks -keysize 2048 -validity 365 -dname "CN=localhost, OU=Development, O=Demo, L=Beijing, ST=Beijing, C=CN" -storepass password -keypass password
```

<br />

## Version

1.1.0
