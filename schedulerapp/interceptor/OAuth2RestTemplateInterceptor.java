package com.ubs.bigid.schedulerapp.interceptor;

import com.ubs.bigid.schedulerapp.security.ServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestTemplate interceptor that automatically adds OAuth2 Bearer token
 * to all outgoing requests to ControllerApp
 */
@Component
public class OAuth2RestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2RestTemplateInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final ServiceTokenProvider tokenProvider;

    public OAuth2RestTemplateInterceptor(ServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, 
            byte[] body, 
            ClientHttpRequestExecution execution) throws IOException {

        // Add OAuth2 Bearer token
        String authHeader = tokenProvider.getAuthorizationHeader();
        request.getHeaders().set(AUTHORIZATION_HEADER, authHeader);
        
        logger.debug("Added OAuth2 token to request: {} {}", request.getMethod(), request.getURI());

        // Add correlation ID from MDC if present
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            request.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
            logger.debug("Added correlation ID to request: {}", correlationId);
        }

        // Execute the request
        return execution.execute(request, body);
    }
}
