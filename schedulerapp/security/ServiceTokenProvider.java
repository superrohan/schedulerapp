package com.ubs.bigid.schedulerapp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

/**
 * Provides OAuth2 access tokens for service-to-service communication
 * Handles token fetching, caching, and automatic refresh
 */
@Component
public class ServiceTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(ServiceTokenProvider.class);
    private static final String CLIENT_REGISTRATION_ID = "controllerapp-client";
    private static final String PRINCIPAL_NAME = "schedulerapp-service";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public ServiceTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    /**
     * Retrieves a valid access token for ControllerApp
     * Token is automatically cached and refreshed by Spring Security
     * 
     * @return Bearer token string
     */
    public String getAccessToken() {
        logger.debug("Requesting access token for client registration: {}", CLIENT_REGISTRATION_ID);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal(PRINCIPAL_NAME)
                .build();

        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient == null) {
            logger.error("Failed to authorize client. Check client credentials configuration.");
            throw new IllegalStateException("Unable to authorize client: " + CLIENT_REGISTRATION_ID);
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        logger.debug("Access token retrieved successfully. Expires at: {}", 
                    authorizedClient.getAccessToken().getExpiresAt());

        return accessToken;
    }

    /**
     * Returns the full Authorization header value
     * 
     * @return "Bearer <token>"
     */
    public String getAuthorizationHeader() {
        return "Bearer " + getAccessToken();
    }
}
