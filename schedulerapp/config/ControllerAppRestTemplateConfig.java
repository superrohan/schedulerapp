package com.ubs.bigid.schedulerapp.config;

import com.ubs.bigid.schedulerapp.interceptor.OAuth2RestTemplateInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for ControllerAppRestTemplate with OAuth2 security
 */
@Configuration
public class ControllerAppRestTemplateConfig {

    @Value("${controllerapp.base-url}")
    private String controllerAppBaseUrl;

    @Value("${controllerapp.timeout.connect:10000}")
    private int connectTimeout;

    @Value("${controllerapp.timeout.read:30000}")
    private int readTimeout;

    private final OAuth2RestTemplateInterceptor oAuth2Interceptor;

    public ControllerAppRestTemplateConfig(OAuth2RestTemplateInterceptor oAuth2Interceptor) {
        this.oAuth2Interceptor = oAuth2Interceptor;
    }

    @Bean
    public RestTemplate controllerAppRestTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .rootUri(controllerAppBaseUrl)
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();

        // Add OAuth2 interceptor to automatically inject tokens
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(oAuth2Interceptor);
        restTemplate.setInterceptors(interceptors);

        // Use buffering request factory for better error handling
        restTemplate.setRequestFactory(
            new BufferingClientHttpRequestFactory(
                new SimpleClientHttpRequestFactory()
            )
        );

        return restTemplate;
    }
}
