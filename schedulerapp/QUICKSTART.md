# Quick Start Guide - OAuth2 Integration for SchedulerApp

## TL;DR - What Changed

**Your existing `ControllerAppRepo` code needs ZERO changes!**

The OAuth2 security is implemented via a **RestTemplate interceptor** that automatically adds the Bearer token to every request.

## What Was Added

### 1. OAuth2 Configuration
- `OAuthClientConfig.java` - Configures OAuth2 client credentials flow
- `ServiceTokenProvider.java` - Fetches and caches tokens from Entra ID

### 2. Automatic Token Injection
- `OAuth2RestTemplateInterceptor.java` - Intercepts ALL RestTemplate calls
- Automatically adds `Authorization: Bearer <token>` header
- Automatically adds `X-Correlation-ID` header

### 3. Enhanced RestTemplate Configuration
- `ControllerAppRestTemplateConfig.java` - Attaches the OAuth2 interceptor
- Your existing `ControllerAppRestTemplate` now automatically secures all calls

### 4. Audit & Tracking
- `ServiceAuditService.java` - Logs all service actions
- `CorrelationIdFilter.java` - Generates correlation IDs
- Enhanced `ControllerAppRepo.java` - Adds audit logging

## How It Works

```
Your Code:
  controllerAppRepo.getScansByStatus("PENDING")
      ↓
RestTemplate (with OAuth2 interceptor):
      ↓
OAuth2RestTemplateInterceptor:
  - Fetches token from ServiceTokenProvider
  - Adds "Authorization: Bearer eyJ..."
  - Adds "X-Correlation-ID: abc-123"
      ↓
HTTP Request to ControllerApp:
  POST /controller/internal/scans/status
  Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc...
  X-Correlation-ID: abc-123-def-456
      ↓
ControllerApp validates JWT and executes
```

## Setup Steps

### 1. Azure Entra ID Configuration

```bash
# Register SchedulerApp in Entra ID
1. Go to Azure Portal → Entra ID → App Registrations
2. Create "SchedulerApp-Service"
3. Copy CLIENT_ID
4. Create client secret → Copy SECRET
5. API Permissions → Add → ControllerApp → Application permissions
6. Grant admin consent
```

### 2. Update application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          controllerapp-client:
            client-id: "<YOUR_CLIENT_ID>"
            client-secret: "<YOUR_CLIENT_SECRET>"
            authorization-grant-type: client_credentials
            scope: api://controller-app/.default
        provider:
          controllerapp-client:
            token-uri: "https://login.microsoftonline.com/<TENANT_ID>/oauth2/v2.0/token"

controllerapp:
  base-url: "http://controller-app:8080"
```

### 3. Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

### 4. Verify

Check logs for:
```
DEBUG c.u.b.s.s.ServiceTokenProvider - Access token retrieved successfully
DEBUG c.u.b.s.i.OAuth2RestTemplateInterceptor - Added OAuth2 token to request: POST http://controller-app:8080/controller/internal/scheduler/launch-scan/123
INFO  c.u.b.s.r.ControllerAppRepo - Scan cycle 123 launched successfully
```

## Migration from Existing Code

### Before (Unsecured)
```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate controllerAppRestTemplate() {
        return new RestTemplateBuilder()
            .rootUri(controllerAppBaseUrl)
            .build();
    }
}
```

### After (OAuth2 Secured)
```java
@Configuration
public class ControllerAppRestTemplateConfig {
    
    private final OAuth2RestTemplateInterceptor oAuth2Interceptor;
    
    @Bean
    public RestTemplate controllerAppRestTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
            .rootUri(controllerAppBaseUrl)
            .build();
        
        // Add OAuth2 interceptor - THIS IS THE ONLY CHANGE
        restTemplate.setInterceptors(List.of(oAuth2Interceptor));
        
        return restTemplate;
    }
}
```

## No Changes Needed In

✅ `ControllerAppRepo.java` methods - work exactly as before  
✅ Scheduler jobs - no changes needed  
✅ Service layer - no changes needed  
✅ Any existing code that uses `controllerAppRestTemplate`  

## What You Get

✅ **OAuth2 Authentication** - All calls secured with JWT tokens  
✅ **Automatic Token Management** - Caching and refresh handled automatically  
✅ **Correlation ID Tracking** - End-to-end request tracing  
✅ **Service Audit Logs** - Every action logged with full context  
✅ **Financial Compliance** - Proper service identity in audit trails  

## Testing

```java
// Your existing tests work as-is
@Test
public void testGetScans() {
    String scans = controllerAppRepo.getScansByStatus("PENDING");
    assertNotNull(scans);
}

// OAuth2 token is automatically mocked in test context
```

## Common Issues

### Issue: 401 Unauthorized
**Solution**: Check client ID, secret, and API permissions in Entra ID

### Issue: Token not added
**Solution**: Verify `OAuth2RestTemplateInterceptor` is in Spring context and attached to RestTemplate

### Issue: ControllerApp rejects token
**Solution**: Ensure ControllerApp has correct `issuer-uri` and `audiences` configuration

## Production Checklist

- [ ] Client secret stored in Azure Key Vault
- [ ] Separate app registrations for dev/staging/prod
- [ ] Admin consent granted for API permissions
- [ ] ControllerApp JWT validation configured
- [ ] Correlation ID propagation tested
- [ ] Audit logs reviewed
- [ ] Token refresh tested (wait for expiry)

## Support

Check these logs:
- `logs/schedulerapp.log` - Application logs with correlation IDs
- `logs/service-audit.log` - Service action audit trail

Enable debug logging:
```yaml
logging:
  level:
    org.springframework.security.oauth2: TRACE
```

## Files Structure

```
schedulerapp/
├── config/
│   ├── OAuthClientConfig.java              ← OAuth2 client setup
│   └── ControllerAppRestTemplateConfig.java ← RestTemplate with interceptor
├── security/
│   └── ServiceTokenProvider.java           ← Token fetching
├── interceptor/
│   └── OAuth2RestTemplateInterceptor.java  ← AUTO token injection
├── repository/
│   └── ControllerAppRepo.java              ← Your code (enhanced logs only)
├── scheduler/
│   └── ScanScheduler.java                  ← Example scheduled jobs
├── audit/
│   └── ServiceAuditService.java            ← Audit logging
├── filter/
│   └── CorrelationIdFilter.java            ← Correlation ID tracking
├── application.yml                          ← Configuration
└── pom.xml                                  ← Dependencies
```

---

**Remember**: Your business logic code doesn't change at all. The OAuth2 security is transparently added via the interceptor pattern.
