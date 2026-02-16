# OAuth2 Client Credentials Integration Architecture

## Complete Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           SchedulerApp (Your Service)                         │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ @Scheduled Job (ScanScheduler.java)                                  │    │
│  │                                                                       │    │
│  │  @Scheduled(cron = "0 0 * * * *")                                    │    │
│  │  public void hourlyScanner() {                                       │    │
│  │      controllerAppRepo.getScansByStatus("PENDING");                  │    │
│  │      controllerAppRepo.launchScanCycle(123);                         │    │
│  │  }                                                                    │    │
│  └───────────────────────────┬───────────────────────────────────────────┘    │
│                              │                                                │
│                              ▼                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ ControllerAppRepo (Your Existing Code - UNCHANGED)                   │    │
│  │                                                                       │    │
│  │  public String getScansByStatus(String status) {                     │    │
│  │      return controllerAppRestTemplate.getForEntity(...);             │    │
│  │  }                                                                    │    │
│  │                                                                       │    │
│  │  public String launchScanCycle(long scanCycleId) {                   │    │
│  │      return controllerAppRestTemplate.postForEntity(...);            │    │
│  │  }                                                                    │    │
│  └───────────────────────────┬───────────────────────────────────────────┘    │
│                              │                                                │
│                              ▼                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ ControllerAppRestTemplate (Enhanced with Interceptor)                │    │
│  │                                                                       │    │
│  │  - Configured in ControllerAppRestTemplateConfig                     │    │
│  │  - OAuth2RestTemplateInterceptor attached                            │    │
│  └───────────────────────────┬───────────────────────────────────────────┘    │
│                              │                                                │
│                              ▼                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ OAuth2RestTemplateInterceptor (NEW - Auto Token Injection)           │    │
│  │                                                                       │    │
│  │  @Override                                                            │    │
│  │  public ClientHttpResponse intercept(...) {                          │    │
│  │      // 1. Get OAuth2 token                                          │    │
│  │      String token = tokenProvider.getAccessToken();                  │    │
│  │                                                                       │    │
│  │      // 2. Add Authorization header                                  │    │
│  │      request.headers.set("Authorization", "Bearer " + token);        │    │
│  │                                                                       │    │
│  │      // 3. Add Correlation ID                                        │    │
│  │      request.headers.set("X-Correlation-ID", MDC.get("correlationId"));│  │
│  │                                                                       │    │
│  │      // 4. Execute request                                           │    │
│  │      return execution.execute(request, body);                        │    │
│  │  }                                                                    │    │
│  └───────┬──────────────────────────────────────────────────────────────┘    │
│          │                                                                    │
│          │ Needs Token                                                        │
│          ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ ServiceTokenProvider (NEW - Token Management)                        │    │
│  │                                                                       │    │
│  │  public String getAccessToken() {                                    │    │
│  │      OAuth2AuthorizeRequest request = ...                            │    │
│  │      OAuth2AuthorizedClient client =                                 │    │
│  │          authorizedClientManager.authorize(request);    ────────────┐│    │
│  │      return client.getAccessToken().getTokenValue();                ││    │
│  │  }                                                                   ││    │
│  └──────────────────────────────────────────────────────────────────────┘│    │
│                                                                          │    │
└──────────────────────────────────────────────────────────────────────────┼────┘
                                                                           │
                    Token Request (if not cached or expired)              │
                                                                           ▼
              ┌────────────────────────────────────────────────────────────────┐
              │           Microsoft Entra ID (Azure AD)                        │
              │                                                                │
              │  POST /oauth2/v2.0/token                                       │
              │  Content-Type: application/x-www-form-urlencoded               │
              │                                                                │
              │  Body:                                                         │
              │    grant_type=client_credentials                              │
              │    client_id={schedulerapp-client-id}                         │
              │    client_secret={schedulerapp-secret}                        │
              │    scope=api://controller-app/.default                        │
              │                                                                │
              │  Response:                                                     │
              │  {                                                             │
              │    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGc...",              │
              │    "expires_in": 3599,                                         │
              │    "token_type": "Bearer"                                      │
              │  }                                                             │
              └────────────────────────────────────────────────────────────────┘
                                       │
                                       │ JWT Token Returned
                                       │
                    ┌──────────────────┘
                    │
                    │ Cached in OAuth2AuthorizedClientService
                    │ Auto-refreshed before expiry
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      HTTP Request to ControllerApp                           │
│                                                                              │
│  POST /controller/internal/scheduler/launch-scan/123                        │
│  Headers:                                                                    │
│    Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc...                          │
│    X-Correlation-ID: abc-123-def-456                                        │
│    Content-Type: application/json                                           │
└──────────────────────────────┬───────────────────────────────────────────────┘
                               │
                               ▼
              ┌────────────────────────────────────────────────────────────────┐
              │               ControllerApp (Target Service)                   │
              │                                                                │
              │  1. Spring Security OAuth2 Resource Server validates JWT      │
              │     - Verifies signature using Entra ID public keys           │
              │     - Checks issuer, audience, expiration                     │
              │     - Extracts service identity (appId)                       │
              │                                                                │
              │  2. SecurityContext populated with service principal          │
              │                                                                │
              │  3. Controller method executed                                │
              │                                                                │
              │  @PostMapping("/internal/scheduler/launch-scan/{id}")         │
              │  public ResponseEntity<String> launchScan(...) {              │
              │      // Service authenticated as "schedulerapp-service"       │
              │      // Business logic executes                               │
              │      return ResponseEntity.ok("Scan launched");               │
              │  }                                                             │
              │                                                                │
              │  4. Response returned to SchedulerApp                         │
              └────────────────────────────────────────────────────────────────┘
```

## Key Integration Points

### 1. Zero Changes to Business Logic
```java
// ControllerAppRepo.java - Your existing code remains EXACTLY the same
public String getScansByStatus(String status) {
    ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
        "/controller/internal/scans/status?status={status}",
        String.class,
        queryStrings
    );
    return response.getBody();
}
```

### 2. OAuth2 Token Automatically Added
The `OAuth2RestTemplateInterceptor` intercepts EVERY call and:
- Fetches fresh token (or uses cached)
- Adds `Authorization: Bearer <token>` header
- Adds correlation ID for tracking

### 3. Token Lifecycle Managed Automatically
- First call: Token fetched from Entra ID
- Subsequent calls: Cached token reused
- Near expiry: Auto-refreshed
- You never touch token logic

### 4. ControllerApp Validation
```java
// ControllerApp application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/{tenant-id}/v2.0
          audiences: api://controller-app
```

ControllerApp automatically:
- Validates JWT signature
- Checks issuer and audience
- Extracts service identity
- Populates SecurityContext

## Configuration Summary

### SchedulerApp (application.yml)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          controllerapp-client:
            client-id: {schedulerapp-client-id}
            client-secret: {schedulerapp-secret}
            authorization-grant-type: client_credentials
            scope: api://controller-app/.default
        provider:
          controllerapp-client:
            token-uri: https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token

controllerapp:
  base-url: http://controller-app:8080
```

### Entra ID Setup
1. **Register SchedulerApp** in Entra ID → Get client ID and secret
2. **Grant API Permissions** to ControllerApp → Admin consent required
3. **ControllerApp exposes API** scope → Used in SchedulerApp scope config

## Audit Trail Example

Every service call generates an audit entry:

```json
{
  "service": "schedulerapp-service",
  "action": "LAUNCH_SCAN",
  "scanId": "123",
  "status": "SUCCESS",
  "timestamp": "2026-02-16T18:30:00Z",
  "correlationId": "abc-123-def-456",
  "details": "Scan launched via scheduler"
}
```

ControllerApp can extract service identity from JWT:
```java
String serviceIdentity = SecurityContextHolder.getContext()
    .getAuthentication()
    .getName();
// Returns: "schedulerapp-service-client-id"
```

## Benefits

✅ **Secure** - All calls authenticated with OAuth2 JWT tokens  
✅ **Transparent** - Zero changes to existing repository code  
✅ **Auditable** - Full correlation ID tracking and service audit logs  
✅ **Maintainable** - Token lifecycle fully managed by Spring Security  
✅ **Compliant** - Proper service identity for financial compliance  
