# SchedulerApp - OAuth2 Integration (Simplified)

## Overview

This is a **minimal** OAuth2 Client Credentials integration for SchedulerApp that:
- ✅ Adds OAuth2 Bearer tokens to all ControllerApp API calls
- ✅ Integrates directly into your existing `ControllerAppRestTemplate` class
- ✅ Uses your existing logging (no MDC, correlation IDs, or audit service)
- ✅ Requires **zero changes** to your existing `ControllerAppRepo` methods

## What's Different from Full Version

This simplified version:
- **NO** ServiceAuditService
- **NO** CorrelationIdFilter
- **NO** MDC logging
- **NO** Separate interceptor class
- **YES** OAuth2 token injection (the core requirement)
- **YES** Uses your existing logger

## Architecture

```
SchedulerApp (Your Existing Code)
│
├── ControllerAppRepo
│   └── Uses controllerAppRestTemplate (NO CHANGES)
│
└── ControllerAppRestTemplate (MODIFIED)
    ├── Inner class: OAuth2BearerTokenInterceptor
    │   └── Intercepts every request
    │       └── Adds "Authorization: Bearer <token>"
    └── ServiceTokenProvider
        └── Fetches token from Entra ID
```

## Key Files

### 1. ControllerAppRestTemplate.java (Modified)
```java
@Configuration
public class ControllerAppRestTemplate {
    
    private final ServiceTokenProvider tokenProvider;
    
    @Bean
    public RestTemplate controllerAppRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Add OAuth2 interceptor
        restTemplate.setInterceptors(
            Collections.singletonList(new OAuth2BearerTokenInterceptor())
        );
        
        return restTemplate;
    }
    
    // Inner class that adds Bearer token to every request
    private class OAuth2BearerTokenInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(...) {
            String accessToken = tokenProvider.getAccessToken();
            request.getHeaders().set("Authorization", "Bearer " + accessToken);
            return execution.execute(request, body);
        }
    }
}
```

### 2. ServiceTokenProvider.java (New)
Fetches and caches OAuth2 tokens from Microsoft Entra ID.

### 3. OAuthClientConfig.java (New)
Configures OAuth2 client credentials flow.

### 4. ControllerAppRepo.java (Unchanged)
Your existing repository methods work exactly as before:
```java
public String getScansByStatus(String status) {
    // Token automatically added by interceptor
    ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(...);
    return response.getBody();
}
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
  base-url: http://controller-app:8080
```

### Environment Variables
```bash
export ENTRA_CLIENT_ID="your-schedulerapp-client-id"
export ENTRA_CLIENT_SECRET="your-schedulerapp-client-secret"
```

## Setup Steps

### 1. Azure Entra ID Setup
1. Go to **Azure Portal** → **Entra ID** → **App Registrations**
2. Create new registration: "SchedulerApp-Service"
3. Copy **Application (client) ID**
4. **Certificates & Secrets** → Create new secret → Copy value
5. **API Permissions** → Add → My APIs → ControllerApp
6. Select **Application permissions** → Grant admin consent

### 2. Update Your Code
Replace your existing `ControllerAppRestTemplate.java` with the provided version.

### 3. Add Dependencies
Already in `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### 4. Run
```bash
mvn clean install
mvn spring-boot:run
```

## How It Works

### Before (Without OAuth2)
```
ControllerAppRepo.getScansByStatus("PENDING")
    ↓
RestTemplate.getForEntity("/scans/status?status=PENDING")
    ↓
HTTP GET http://controller-app:8080/scans/status?status=PENDING
NO Authorization header
```

### After (With OAuth2)
```
ControllerAppRepo.getScansByStatus("PENDING")
    ↓
RestTemplate.getForEntity("/scans/status?status=PENDING")
    ↓
OAuth2BearerTokenInterceptor intercepts
    ↓
ServiceTokenProvider.getAccessToken() → "eyJ0eXAiOiJKV1Qi..."
    ↓
Adds header: Authorization: Bearer eyJ0eXAiOiJKV1Qi...
    ↓
HTTP GET http://controller-app:8080/scans/status?status=PENDING
Headers: Authorization: Bearer eyJ0eXAiOiJKV1Qi...
```

## Logging

Uses your existing logger:
```
DEBUG c.u.b.s.c.ControllerAppRestTemplate - Added OAuth2 Bearer token to request: POST http://controller-app:8080/controller/internal/scheduler/launch-scan/123
INFO  c.u.b.s.r.ControllerAppRepo - Scan cycle 123 launched successfully
```

## Token Management

- **First call**: Token fetched from Entra ID
- **Subsequent calls**: Cached token reused
- **Near expiry**: Automatically refreshed by Spring Security
- **You never touch token logic**

## Troubleshooting

### Issue: 401 Unauthorized
Check:
1. Client ID and secret are correct in `application.yml`
2. API permissions granted in Entra ID
3. ControllerApp is validating JWT correctly

### Issue: Token not added
Check:
1. `ServiceTokenProvider` bean is created
2. `OAuth2BearerTokenInterceptor` is registered with RestTemplate
3. Debug logs show: "Added OAuth2 Bearer token to request"

### Enable Debug Logging
```yaml
logging:
  level:
    com.ubs.bigid.schedulerapp: DEBUG
    org.springframework.security.oauth2: DEBUG
```

## Testing

Your existing tests continue to work. In test context, you can mock the token provider:

```java
@MockBean
private ServiceTokenProvider tokenProvider;

@Test
public void testSecuredCall() {
    when(tokenProvider.getAccessToken()).thenReturn("mock-token");
    
    String result = controllerAppRepo.getScansByStatus("PENDING");
    assertNotNull(result);
}
```

## File Structure

```
schedulerapp/
├── config/
│   ├── OAuthClientConfig.java               ← OAuth2 configuration
│   └── ControllerAppRestTemplate.java       ← Modified with interceptor
├── security/
│   └── ServiceTokenProvider.java            ← Token management
├── repository/
│   └── ControllerAppRepo.java               ← Unchanged
├── scheduler/
│   └── ScanScheduler.java                   ← Example jobs (simplified)
├── application.yml                           ← Configuration
└── pom.xml                                   ← Dependencies
```

## What You DON'T Need

- ❌ ServiceAuditService
- ❌ CorrelationIdFilter
- ❌ MDC logging
- ❌ Separate OAuth2RestTemplateInterceptor class
- ❌ Custom correlation ID tracking

## What You DO Need

- ✅ OAuthClientConfig
- ✅ ServiceTokenProvider
- ✅ Modified ControllerAppRestTemplate (with inner interceptor class)
- ✅ application.yml with OAuth2 config
- ✅ Entra ID app registration

## Production Deployment

Store secrets securely:
```bash
# Azure Key Vault
az webapp config appsettings set \
  --settings \
    ENTRA_CLIENT_SECRET="@Microsoft.KeyVault(SecretUri=https://vault.azure.net/secrets/ClientSecret/)"
```

---

**Summary**: This is the minimal OAuth2 integration. Your existing `ControllerAppRepo` code doesn't change at all. OAuth2 tokens are transparently added via an interceptor in the `ControllerAppRestTemplate` class.
