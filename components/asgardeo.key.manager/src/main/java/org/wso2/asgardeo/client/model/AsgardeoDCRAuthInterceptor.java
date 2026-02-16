package org.wso2.asgardeo.client.model;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.asgardeo.client.AsgardeoConstants;
import org.wso2.asgardeo.client.AsgardeoOAuthClient;
import org.wso2.carbon.apimgt.api.APIManagementException;

/**
 * Represents Auth Interceptor that retrieves / refreshes access token required for Asgardeo MGMT OAuth App Calls
 */
public class AsgardeoDCRAuthInterceptor implements RequestInterceptor {
    private static final Log log = LogFactory.getLog(AsgardeoDCRAuthInterceptor.class);

    private AsgardeoAccessTokenResponse cachedToken;
    private AsgardeoTokenClient tokenClient;
    private String mgmtClientId;
    private String mgmtClientSecret;

    public AsgardeoDCRAuthInterceptor(
            AsgardeoTokenClient asgardeoTokenClient, String consumerKey, String consumerSecret) {
        this.tokenClient = asgardeoTokenClient;
        this.mgmtClientId = consumerKey;
        this.mgmtClientSecret = consumerSecret;
        getAccessToken();
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (cachedToken == null || tokenExpired()) {
            getAccessToken();
        }
        requestTemplate.header("Authorization", "Bearer ".concat(cachedToken.getAccessToken()));
    }

    // extracted for readability
    private boolean tokenExpired() {
        return System.currentTimeMillis() >
                (cachedToken.getCreatedAt() + cachedToken.getExpiry() * 1000);
    }


    /**
     * Renew the access token of the management API
     */
    private void getAccessToken() {
        try {
            String basicCredentials = AsgardeoOAuthClient.getEncodedCredentials(
                    this.mgmtClientId, this.mgmtClientSecret);
            AsgardeoAccessTokenResponse accessTokenResponse =
                    tokenClient.getAccessToken(
                            AsgardeoConstants.GRANT_TYPE_CLIENT_CREDENTIALS, AsgardeoConstants.DCR_SCOPES,
                            basicCredentials);
            if (accessTokenResponse != null) {
                this.cachedToken = accessTokenResponse;
                this.cachedToken.setCreatedAt(System.currentTimeMillis());
            }
        } catch (APIManagementException e) {
            log.error("Error while encoding credentials for client ID : " + this.mgmtClientId, e);
        }
    }
}
