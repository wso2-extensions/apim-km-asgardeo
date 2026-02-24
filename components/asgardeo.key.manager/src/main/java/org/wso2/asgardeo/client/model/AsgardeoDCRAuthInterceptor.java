/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.asgardeo.client.model;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.asgardeo.client.AsgardeoConstants;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;
import org.wso2.carbon.apimgt.impl.kmclient.model.AuthClient;
import org.wso2.carbon.apimgt.impl.kmclient.model.TokenInfo;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Represents Auth Interceptor that retrieves / refreshes access token required for Asgardeo Mgmt OAuth App Calls
 */
public class AsgardeoDCRAuthInterceptor implements RequestInterceptor {
    private static final Log log = LogFactory.getLog(AsgardeoDCRAuthInterceptor.class);

    private static final long REFRESH_SKEW_MS = 10000; // buffer time of 10 secs

    private TokenInfo cachedToken;
    private long createdAt;
    private AuthClient tokenClient;
    private String mgmtClientId;
    private String mgmtClientSecret;

    public AsgardeoDCRAuthInterceptor(
            AuthClient tokenClient, String consumerKey, String consumerSecret) {
        this.tokenClient = tokenClient;
        this.mgmtClientId = consumerKey;
        this.mgmtClientSecret = consumerSecret;
        getAccessToken();
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (cachedToken == null || tokenExpired()) {
            getAccessToken();
        }
        requestTemplate.header("Authorization", "Bearer ".concat(cachedToken.getToken()));
    }

    private boolean tokenExpired() {
        return System.currentTimeMillis() + REFRESH_SKEW_MS >
                (createdAt + cachedToken.getExpiry() * 1000);
    }


    /**
     * Renew the access token of the management API
     */
    private void getAccessToken() {
        try {
            String credentials = mgmtClientId + ":" + mgmtClientSecret;
            String authToken = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            TokenInfo accessTokenResponse =
                    tokenClient.generate(authToken, AsgardeoConstants.GRANT_TYPE_CLIENT_CREDENTIALS,
                            AsgardeoConstants.MANAGEMENT_SCOPES);
            if (accessTokenResponse != null) {
                this.cachedToken = accessTokenResponse;
                createdAt = System.currentTimeMillis();
            }
        } catch (KeyManagerClientException e) {
            log.error("Error while generating token for Management App with Client ID : " + this.mgmtClientId, e);
        }
    }
}
