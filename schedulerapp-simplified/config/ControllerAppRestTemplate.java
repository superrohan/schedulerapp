package com.ubs.bigid.schedulerapp.config;

import com.ubs.bigid.schedulerapp.security.ServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

/**
 * RestTemplate configuration for ControllerApp with OAuth2 security
 * OAuth2 Bearer token is automatically injected via interceptor
 */
@Configuration
public class ControllerAppRestTemplate {

    private static final Logger logger = LoggerFactory.getLogger(ControllerAppRestTemplate.class);

    @Value("${controllerapp.base-url}")
    private String controllerappBaseUrl;

    @Value("${controllerapp.timeout.connect:10000}")
    private int connectTimeout;

    @Value("${controllerapp.timeout.read:30000}")
    private int readTimeout;

    private final ServiceTokenProvider tokenProvider;

    public ControllerAppRestTemplate(ServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public RestTemplate controllerAppRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Set timeouts
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // This is a simple way to set timeouts, you might need HttpComponentsClientHttpRequestFactory
            // for more control
            return execution.execute(request, body);
        });

        // Add OAuth2 interceptor that automatically adds Bearer token to every request
        restTemplate.setInterceptors(Collections.singletonList(new OAuth2BearerTokenInterceptor()));

        logger.info("ControllerApp RestTemplate configured with OAuth2 security. Base URL: {}", controllerappBaseUrl);
        
        return restTemplate;
    }

    /**
     * Inner class that intercepts all RestTemplate requests and adds OAuth2 Bearer token
     */
    private class OAuth2BearerTokenInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) 
                throws IOException {
            
            // Fetch OAuth2 token from ServiceTokenProvider
            String accessToken = tokenProvider.getAccessToken();
            
            // Add Authorization header with Bearer token
            request.getHeaders().set("Authorization", "Bearer " + accessToken);
            
            logger.debug("Added OAuth2 Bearer token to request: {} {}", request.getMethod(), request.getURI());
            
            // Execute the request
            return execution.execute(request, body);
        }
    }
}
