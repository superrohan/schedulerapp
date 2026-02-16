# SchedulerApp - OAuth2 Secured Service Communication

## Overview

SchedulerApp is a Spring Boot background service that securely communicates with ControllerApp using **Microsoft Entra ID OAuth2 Client Credentials** flow. This ensures proper authentication, authorization, and audit logging for all service-to-service API calls.

## Architecture

```
┌─────────────────┐                           ┌──────────────────┐
│  SchedulerApp   │                           │  Microsoft       │
│                 │  1. Request Token         │  Entra ID        │
│  @Scheduled     │ ───────────────────────>  │                  │
│  Jobs           │                           │  OAuth2 Token    │
│                 │  2. Return JWT Token      │  Endpoint        │
│                 │ <───────────────────────  │                  │
└────────┬────────┘                           └──────────────────┘
         │
         │ 3. API Call + Bearer Token
         │
         ▼
┌─────────────────────────────────────────────┐
│         ControllerApp                       │
│                                             │
│  - Validates JWT Token                      │
│  - Extracts Service Identity                │
│  - Executes Business Logic                  │
│  - Returns Response                         │
└─────────────────────────────────────────────┘
```

## Key Features

✅ **OAuth2 Client Credentials Flow** - Service-to-service authentication without user context  
✅ **Automatic Token Management** - Tokens are cached and auto-refreshed  
✅ **RestTemplate Interceptor** - OAuth2 token automatically injected into every request  
✅ **Correlation ID Tracking** - End-to-end request tracking across services  
✅ **Comprehensive Audit Logging** - All service actions logged with full context  
✅ **Zero Code Changes in Business Logic** - Existing `ControllerAppRepo` methods work as-is  

## How It Works

### 1. OAuth2 Token Acquisition

`ServiceTokenProvider` automatically:
- Requests access token from Entra ID using client credentials
- Caches token in memory
- Auto-refreshes before expiry
- Returns valid Bearer token

### 2. Automatic Token Injection

`OAuth2RestTemplateInterceptor` intercepts **every** `RestTemplate` request and:
- Fetches fresh token from `ServiceTokenProvider`
- Adds `Authorization: Bearer <token>` header
- Adds `X-Correlation-ID` header for tracking

### 3. No Changes to Business Code

Your existing `ControllerAppRepo` code **remains unchanged**:

```java
// This code works exactly as before - OAuth2 token is automatically added
String scans = controllerAppRepo.getScansByStatus("PENDING");
String response = controllerAppRepo.launchScanCycle(123);
```

The `OAuth2RestTemplateInterceptor` transparently secures all calls.

## Project Structure

```
schedulerapp/
├── config/
│   ├── OAuthClientConfig.java              # OAuth2 client configuration
│   └── ControllerAppRestTemplateConfig.java # RestTemplate with interceptor
├── security/
│   └── ServiceTokenProvider.java           # Token fetching and caching
├── interceptor/
│   └── OAuth2RestTemplateInterceptor.java  # Auto-inject OAuth2 token
├── repository/
│   └── ControllerAppRepo.java              # Enhanced with audit logging
├── scheduler/
│   └── ScanScheduler.java                  # Example scheduled jobs
├── audit/
│   └── ServiceAuditService.java            # Service action auditing
├── filter/
│   └── CorrelationIdFilter.java            # Request tracking
├── application.yml                          # Configuration
├── logback-spring.xml                       # Logging config
└── pom.xml                                  # Dependencies
```

## Configuration

### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          controllerapp-client:
            client-id: ${ENTRA_CLIENT_ID}
            client-secret: ${ENTRA_CLIENT_SECRET}
            authorization-grant-type: client_credentials
            scope: api://controller-app/.default
        
        provider:
          controllerapp-client:
            token-uri: https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token

controllerapp:
  base-url: http://controller-app-host:8080
```

### Environment Variables

Set these before running:

```bash
export ENTRA_CLIENT_ID="your-schedulerapp-client-id"
export ENTRA_CLIENT_SECRET="your-schedulerapp-client-secret"
export ENTRA_TOKEN_URI="https://login.microsoftonline.com/your-tenant-id/oauth2/v2.0/token"
export CONTROLLER_APP_BASE_URL="http://controller-app:8080"
```

## Integration Steps

### Step 1: Register SchedulerApp in Entra ID

1. Go to **Azure Portal** → **Entra ID** → **App Registrations**
2. Click **New Registration**
   - Name: `SchedulerApp-Service`
   - Supported account types: Single tenant
3. After creation, note the **Application (client) ID**
4. Go to **Certificates & Secrets** → Create new **Client Secret**
5. Copy the secret value (only shown once)

### Step 2: Configure API Permissions

1. In SchedulerApp registration, go to **API Permissions**
2. Click **Add a permission** → **My APIs**
3. Select **ControllerApp** (must be already registered)
4. Select **Application permissions** (not Delegated)
5. Check the exposed scope (e.g., `ControllerApp.Execute`)
6. Click **Grant admin consent**

### Step 3: Verify ControllerApp Configuration

ControllerApp must validate the JWT token. In `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/{tenant-id}/v2.0
          audiences: api://controller-app
```

### Step 4: Update application.yml in SchedulerApp

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          controllerapp-client:
            client-id: "<CLIENT_ID_FROM_STEP_1>"
            client-secret: "<CLIENT_SECRET_FROM_STEP_1>"
            authorization-grant-type: client_credentials
            scope: api://controller-app/.default
        
        provider:
          controllerapp-client:
            token-uri: "https://login.microsoftonline.com/<TENANT_ID>/oauth2/v2.0/token"

controllerapp:
  base-url: "http://localhost:8080"
```

### Step 5: Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

## Usage Examples

### Example 1: Scheduled Job

```java
@Scheduled(cron = "0 0 * * * *")
public void hourlyScanner() {
    // Token is automatically fetched and added
    String scans = controllerAppRepo.getScansByStatus("PENDING");
    
    // Process and launch scans
    // All calls are automatically secured with OAuth2
}
```

### Example 2: Manual Trigger

```java
public void launchScan(long scanCycleId) {
    // OAuth2 token automatically injected
    String response = controllerAppRepo.launchScanCycle(scanCycleId);
}
```

### Example 3: Audit Logging

Every call is automatically logged:

```json
{
  "service": "schedulerapp-service",
  "action": "LAUNCH_SCAN",
  "scanId": "123",
  "status": "SUCCESS",
  "timestamp": "2026-02-16T18:30:00Z",
  "correlationId": "abc-123-def-456"
}
```

## Security Flow Detail

### Request Flow

```
1. Scheduler Job Triggers
   └─> ControllerAppRepo.launchScanCycle(123)
       └─> RestTemplate.postForEntity(...)
           └─> OAuth2RestTemplateInterceptor.intercept()
               ├─> ServiceTokenProvider.getAccessToken()
               │   ├─> Check cache
               │   ├─> If expired/missing: Request new token from Entra ID
               │   └─> Return valid token
               ├─> Add "Authorization: Bearer <token>"
               ├─> Add "X-Correlation-ID: <uuid>"
               └─> Execute HTTP request
                   └─> ControllerApp validates JWT
                       └─> Executes business logic
                           └─> Returns response
```

### Token Lifecycle

1. **First Request**: Token fetched from Entra ID
2. **Subsequent Requests**: Cached token reused
3. **Near Expiry**: Automatically refreshed
4. **No Manual Management**: Handled by Spring Security OAuth2

## Audit Logging

### Service Audit Logs

Located in `logs/service-audit.log`:

```
2026-02-16 18:30:00 [abc-123] - {"service":"schedulerapp-service","action":"LAUNCH_SCAN","scanId":"123","status":"STARTED","timestamp":"2026-02-16T18:30:00Z","correlationId":"abc-123"}
2026-02-16 18:30:01 [abc-123] - {"service":"schedulerapp-service","action":"LAUNCH_SCAN","scanId":"123","status":"SUCCESS","timestamp":"2026-02-16T18:30:01Z","correlationId":"abc-123"}
```

### Application Logs

Located in `logs/schedulerapp.log` with correlation ID:

```
2026-02-16 18:30:00 [main] [abc-123] INFO  c.u.b.s.r.ControllerAppRepo - Scan cycle 123 launched successfully
```

## Testing

### Test Token Acquisition

```java
@Autowired
private ServiceTokenProvider tokenProvider;

@Test
public void testTokenAcquisition() {
    String token = tokenProvider.getAccessToken();
    assertNotNull(token);
}
```

### Test Secured API Call

```java
@Autowired
private ControllerAppRepo repo;

@Test
public void testSecuredCall() {
    String response = repo.getScansByStatus("PENDING");
    assertNotNull(response);
}
```

## Troubleshooting

### Issue: "Unable to authorize client"

**Cause**: Invalid client credentials or token URI  
**Solution**: Verify `client-id`, `client-secret`, and `token-uri` in application.yml

### Issue: 401 Unauthorized from ControllerApp

**Cause**: ControllerApp not validating tokens correctly  
**Solution**: Check ControllerApp's `spring.security.oauth2.resourceserver` configuration

### Issue: Token not added to request

**Cause**: Interceptor not registered with RestTemplate  
**Solution**: Verify `ControllerAppRestTemplateConfig` has the interceptor configured

### Issue: No correlation ID in logs

**Cause**: MDC not populated  
**Solution**: Ensure `CorrelationIdFilter` is registered and scheduler jobs call `ensureCorrelationId()`

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## Security Best Practices

✅ Store client secrets in **Azure Key Vault** or environment variables  
✅ Use **managed identities** in Azure for zero-secret authentication  
✅ Rotate client secrets regularly  
✅ Monitor audit logs for anomalous behavior  
✅ Use separate client registrations for dev/staging/prod  
✅ Implement rate limiting on ControllerApp  

## Production Deployment

### Azure App Service

```bash
az webapp config appsettings set \
  --name schedulerapp \
  --resource-group myResourceGroup \
  --settings \
    ENTRA_CLIENT_ID="your-client-id" \
    ENTRA_CLIENT_SECRET="@Microsoft.KeyVault(SecretUri=https://myvault.vault.azure.net/secrets/ClientSecret/)" \
    CONTROLLER_APP_BASE_URL="https://controller-app.azurewebsites.net"
```

### Kubernetes

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: schedulerapp-oauth2
type: Opaque
stringData:
  client-id: "your-client-id"
  client-secret: "your-client-secret"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: schedulerapp
spec:
  template:
    spec:
      containers:
      - name: schedulerapp
        env:
        - name: ENTRA_CLIENT_ID
          valueFrom:
            secretKeyRef:
              name: schedulerapp-oauth2
              key: client-id
        - name: ENTRA_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: schedulerapp-oauth2
              key: client-secret
```

## Migration from Existing Code

### Before (Unsecured)

```java
// ControllerAppRepo.java - Original
ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
    "/controller/internal/scans/status?status={status}",
    String.class,
    queryStrings
);
```

### After (OAuth2 Secured)

```java
// ControllerAppRepo.java - No changes needed!
// OAuth2RestTemplateInterceptor automatically adds token
ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
    "/controller/internal/scans/status?status={status}",
    String.class,
    queryStrings
);
```

**Zero code changes required in repository methods!**

## Support

For issues or questions:
- Check logs in `logs/schedulerapp.log` and `logs/service-audit.log`
- Enable TRACE logging for OAuth2: `logging.level.org.springframework.security.oauth2: TRACE`
- Review Entra ID app registration permissions

## License

Internal UBS Project - Confidential
