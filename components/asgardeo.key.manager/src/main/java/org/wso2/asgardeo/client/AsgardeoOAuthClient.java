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

package org.wso2.asgardeo.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feign.FeignException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.wso2.asgardeo.client.model.AsgardeoAPIResourceClient;
import org.wso2.asgardeo.client.model.AsgardeoAPIResourceListResponse;
import org.wso2.asgardeo.client.model.AsgardeoAPIResourceResponse;
import org.wso2.asgardeo.client.model.AsgardeoAPIResourceScopesClient;
import org.wso2.asgardeo.client.model.AsgardeoDCRAuthInterceptor;
import org.wso2.asgardeo.client.model.AsgardeoDCRClient;
import org.wso2.asgardeo.client.model.AsgardeoDCRClientInfo;
import org.wso2.asgardeo.client.model.AsgardeoIntrospectionClient;
import org.wso2.asgardeo.client.model.AsgardeoIntrospectionResponse;
import org.wso2.asgardeo.client.model.AsgardeoPatchRoleOperationInfo;
import org.wso2.asgardeo.client.model.AsgardeoRoleInfo;
import org.wso2.asgardeo.client.model.AsgardeoSCIMRolesClient;
import org.wso2.asgardeo.client.model.AsgardeoScopePatchRequest;
import org.wso2.asgardeo.client.model.AsgardeoScopeResponse;
import org.wso2.asgardeo.client.model.AsgardeoScopeUpdateRequest;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.AccessTokenRequest;
import org.wso2.carbon.apimgt.api.model.ApplicationConstants;
import org.wso2.carbon.apimgt.api.model.KeyManagerConfiguration;
import org.wso2.carbon.apimgt.api.model.KeyManagerConnectorConfiguration;
import org.wso2.carbon.apimgt.api.model.OAuthAppRequest;
import org.wso2.carbon.apimgt.api.model.OAuthApplicationInfo;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.AbstractKeyManager;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.kmclient.FormEncoder;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;
import org.wso2.carbon.apimgt.impl.kmclient.model.AuthClient;
import org.wso2.carbon.apimgt.impl.kmclient.model.TokenInfo;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides the implementation to use Asgardeo for managing
 * OAuth clients and Tokens needed by WSO2 API Manager.
 */
public class AsgardeoOAuthClient extends AbstractKeyManager {

    private static final Log log = LogFactory.getLog(AsgardeoOAuthClient.class);

    private AuthClient authClient;
    private AsgardeoDCRClient dcrClient;
    private AsgardeoIntrospectionClient introspectionClient;
    private AsgardeoAPIResourceClient apiResourceClient;
    private AsgardeoAPIResourceScopesClient apiResourceScopesClient;
    private AsgardeoSCIMRolesClient asgardeoSCIMRolesClient;

    private String mgmtClientId;
    private String mgmtClientSecret; //client id and secret of management app
    private String globalApiResourceId;
    private String globalApiResourceName;

    private boolean enableRoleCreation;

    @Override
    public void loadConfiguration(KeyManagerConfiguration keyManagerConfiguration) throws APIManagementException {

        this.configuration = keyManagerConfiguration;

        String org = (String) configuration.getParameter(AsgardeoConstants.ORG_NAME);
        String baseURL = AsgardeoConstants.ASGARDEO_BASE_URL;

        mgmtClientId = (String) configuration.getParameter(AsgardeoConstants.MGMT_CLIENT_ID);

        if (mgmtClientId == null || mgmtClientId.isBlank()) {
            throw new APIManagementException(
                    "The Asgardeo application client ID in the configuration is missing or empty");
        }

        mgmtClientSecret = (String) configuration.getParameter(AsgardeoConstants.MGMT_CLIENT_SECRET);

        if (mgmtClientSecret == null || mgmtClientSecret.isBlank()) {
            throw new APIManagementException(
                    "The Asgardeo application client secret in the configuration is missing or empty");
        }

        if (configuration.getParameter(AsgardeoConstants.ENABLE_ROLE_CREATION) instanceof Boolean) {
            enableRoleCreation = (Boolean) configuration.getParameter(AsgardeoConstants.ENABLE_ROLE_CREATION);
        } else {
            enableRoleCreation = false;
        }

        String dcrEndpoint;
        if (configuration.getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT) != null) {
            dcrEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT);
        } else {
            dcrEndpoint = baseURL + "/t/" + org + "/api/identity/oauth2/dcr/v1.1/register";
        }

        String tokenEndpoint;
        if (configuration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT) != null) {
            tokenEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT);
        } else {
            tokenEndpoint = baseURL + "/t/" + org + "/oauth2/token";
        }

        String introspectionEndpoint;
        if (configuration.getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT) != null) {
            introspectionEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT);
        } else {
            introspectionEndpoint = baseURL + "/t/" + org + "/oauth2/introspect";
        }

        String rolesEndpoint;
        if (configuration.getParameter(AsgardeoConstants.ROLES_MANAGEMENT_ENDPOINT) != null) {
            rolesEndpoint = (String) configuration.getParameter(AsgardeoConstants.ROLES_MANAGEMENT_ENDPOINT);
        } else {
            rolesEndpoint = baseURL + "/t/" + org + "/scim2/v2/Roles";
        }

        String apiResourceServerBase;
        if (configuration.getParameter(AsgardeoConstants.RESOURCE_MANAGEMENT_ENDPOINT) != null) {
            apiResourceServerBase = (String) configuration.getParameter(AsgardeoConstants.RESOURCE_MANAGEMENT_ENDPOINT);
        } else {
            apiResourceServerBase = baseURL + "/t/" + org + "/api/server/v1/api-resources";
        }

        if (configuration.getParameter(AsgardeoConstants.GLOBAL_API_RESOURCE_NAME) != null) {
            globalApiResourceName = (String) configuration.getParameter(AsgardeoConstants.GLOBAL_API_RESOURCE_NAME);
        } else {
            globalApiResourceName = AsgardeoConstants.GLOBAL_API_RESOURCE_NAME;
        }

        authClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new FormEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .target(AuthClient.class, tokenEndpoint);

        introspectionClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new FormEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .target(AsgardeoIntrospectionClient.class, introspectionEndpoint);

        AsgardeoDCRAuthInterceptor interceptor = new AsgardeoDCRAuthInterceptor(authClient,
                mgmtClientId, mgmtClientSecret);

        dcrClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoDCRClient.class, dcrEndpoint);

        apiResourceClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoAPIResourceClient.class, apiResourceServerBase);

        apiResourceScopesClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoAPIResourceScopesClient.class, apiResourceServerBase);

        asgardeoSCIMRolesClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoSCIMRolesClient.class, rolesEndpoint);

        globalApiResourceId = getAPIResourceId(globalApiResourceName);
    }

    @Override
    public OAuthApplicationInfo createApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {

        OAuthApplicationInfo oAuthApplicationInfo = oAuthAppRequest.getOAuthApplicationInfo();

        AsgardeoDCRClientInfo body = getClientInfo(oAuthApplicationInfo);
        setAdditionalPropertiesToClient(oAuthApplicationInfo, body);
        AsgardeoDCRClientInfo created;

        try {
            created = dcrClient.create(body);
        } catch (feign.FeignException e) {
            handleException("Cannot create OAuth Application: " + body.getClientName() + " for Application "
                    + oAuthApplicationInfo.getClientName(), e);
            return null;
        }
        return createOAuthApplicationInfo(created);
    }

    @NotNull
    private AsgardeoDCRClientInfo getClientInfo(OAuthApplicationInfo in) {

        String appName = in.getClientName();
        String appId = in.getApplicationUUID();
        String keyType = (String) in.getParameter(ApplicationConstants.APP_KEY_TYPE);
        String user = (String) in.getParameter(ApplicationConstants.OAUTH_CLIENT_USERNAME);

        String clientName = (user != null ? user : "apim") + "_" + appName + "_" + appId.substring(0, 7) +
                (keyType != null ? "_" + keyType : StringUtils.EMPTY);

        AsgardeoDCRClientInfo body = new AsgardeoDCRClientInfo();
        body.setClientName(clientName);

        List<String> grantTypes = getGrantTypesFromOAuthApp(in);
        body.setGrantTypes(grantTypes);

        if (org.apache.commons.lang3.StringUtils.isNotEmpty(in.getCallBackURL())) {
            String callBackURL = in.getCallBackURL();
            String[] callbackURLs = callBackURL.trim().split("\\s*,\\s*");
            body.setRedirectUris(Arrays.asList(callbackURLs));
        }

        body.setTokenTypeAsJWT();

        return body;
    }

    @NotNull
    private List<String> getGrantTypesFromOAuthApp(OAuthApplicationInfo in) {

        List<String> grantTypes = new ArrayList<>();

        if (in.getParameter(APIConstants.JSON_GRANT_TYPES) != null) {
            grantTypes = Arrays.asList(((String) in.getParameter(APIConstants.JSON_GRANT_TYPES))
                    .split(","));
        }
        return grantTypes;
    }

    @NotNull
    private OAuthApplicationInfo createOAuthApplicationInfo(AsgardeoDCRClientInfo dcrClient) {

        OAuthApplicationInfo oAuthApplicationInfo = new OAuthApplicationInfo();
        oAuthApplicationInfo.setClientName(dcrClient.getClientName());
        oAuthApplicationInfo.setClientId(dcrClient.getClientId());
        oAuthApplicationInfo.setClientSecret(dcrClient.getClientSecret());
        oAuthApplicationInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_ID, dcrClient.getClientId());
        oAuthApplicationInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_SECRET, dcrClient.getClientSecret());
        oAuthApplicationInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_NAME, dcrClient.getClientName());

        if (dcrClient.getGrantTypes() != null && !dcrClient.getGrantTypes().isEmpty()) {
            oAuthApplicationInfo.addParameter(
                    APIConstants.JSON_GRANT_TYPES, String.join(" ", dcrClient.getGrantTypes()));
        }

        if (dcrClient.getRedirectUris() != null && !dcrClient.getRedirectUris().isEmpty()) {
            oAuthApplicationInfo.setCallBackURL(String.join(",", dcrClient.getRedirectUris()));
        }

        Map<String, Object> additionalProperties = getAdditionalPropertiesFromClient(dcrClient);
        oAuthApplicationInfo.addParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES, additionalProperties);

        return oAuthApplicationInfo;
    }

    @NotNull
    private Map<String, Object> getAdditionalPropertiesFromClient(AsgardeoDCRClientInfo dcrClient) {

        Map<String, Object> additionalProperties = new HashMap<>();

        additionalProperties.put(AsgardeoConstants.APPLICATION_TOKEN_LIFETIME,
                dcrClient.getApplicationTokenLifetime());
        additionalProperties.put(AsgardeoConstants.USER_TOKEN_LIFETIME,
                dcrClient.getUserTokenLifetime());
        additionalProperties.put(AsgardeoConstants.REFRESH_TOKEN_LIFETIME,
                dcrClient.getRefreshTokenLifetime());
        additionalProperties.put(AsgardeoConstants.ID_TOKEN_LIFETIME, dcrClient.getIdTokenLifetime());
        additionalProperties.put(AsgardeoConstants.PKCE_MANDATORY, dcrClient.isPkceMandatory());
        additionalProperties.put(AsgardeoConstants.PKCE_SUPPORT_PLAIN, dcrClient.isPkcePlainText());
        additionalProperties.put(AsgardeoConstants.PUBLIC_CLIENT, dcrClient.isPublicClient());

        return additionalProperties;
    }

    /**
     * This method will update an existing OAuth client in Custom Authorization Server.
     *
     * @param oAuthAppRequest Parameters to be passed to Authorization Server,
     *                        encapsulated as an {@code OAuthAppRequest}
     * @return Details of updated OAuth Client.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public OAuthApplicationInfo updateApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {

        OAuthApplicationInfo oAuthApplicationInfo = oAuthAppRequest.getOAuthApplicationInfo();
        AsgardeoDCRClientInfo body = getClientInfo(oAuthApplicationInfo);
        setAdditionalPropertiesToClient(oAuthApplicationInfo, body);

        try {
            AsgardeoDCRClientInfo updatedAsgardeoDCRClientInfo = dcrClient.update(
                    oAuthApplicationInfo.getClientId(), body);

            return createOAuthApplicationInfo(updatedAsgardeoDCRClientInfo);
        } catch (feign.FeignException e) {
            handleException("Could not update service provider with ID: " + oAuthApplicationInfo.getClientId(), e);
            return null;
        }
    }

    private void setAdditionalPropertiesToClient(OAuthApplicationInfo applicationInfo, AsgardeoDCRClientInfo body)
            throws APIManagementException {

        Object parameter = applicationInfo.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);

        Map<String, Object> additionalProperties = new Gson().fromJson((String) parameter, Map.class);

        if (additionalProperties.containsKey(AsgardeoConstants.APPLICATION_TOKEN_LIFETIME)) {
            Object expiryTimeObject =
                    additionalProperties.get(AsgardeoConstants.APPLICATION_TOKEN_LIFETIME);
            if (expiryTimeObject instanceof String) {
                if (!APIConstants.KeyManager.NOT_APPLICABLE_VALUE.equals(expiryTimeObject)) {
                    try {
                        long expiry = Long.parseLong((String) expiryTimeObject);

                        if (expiry < 0) {
                            throw new APIManagementException("Invalid application token lifetime given for "
                                    + applicationInfo.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setApplicationTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring application token lifetime value as it is not a valid number");
                        }
                    }
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.USER_TOKEN_LIFETIME)) {
            Object expiryTimeObject =
                    additionalProperties.get(AsgardeoConstants.USER_TOKEN_LIFETIME);
            if (expiryTimeObject instanceof String) {
                if (!APIConstants.KeyManager.NOT_APPLICABLE_VALUE.equals(expiryTimeObject)) {
                    try {
                        long expiry = Long.parseLong((String) expiryTimeObject);

                        if (expiry < 0) {
                            throw new APIManagementException("Invalid application token lifetime given for "
                                    + applicationInfo.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setUserTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring user token lifetime value as it is not a valid number");
                        }
                    }
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.REFRESH_TOKEN_LIFETIME)) {
            Object expiryTimeObject =
                    additionalProperties.get(AsgardeoConstants.REFRESH_TOKEN_LIFETIME);
            if (expiryTimeObject instanceof String) {
                if (!APIConstants.KeyManager.NOT_APPLICABLE_VALUE.equals(expiryTimeObject)) {
                    try {
                        long expiry = Long.parseLong((String) expiryTimeObject);

                        if (expiry < 0) {
                            throw new APIManagementException("Invalid application token lifetime given for "
                                    + applicationInfo.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setRefreshTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring refresh token lifetime value as it is not a valid number");
                        }
                    }
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.ID_TOKEN_LIFETIME)) {
            Object expiryTimeObject =
                    additionalProperties.get(AsgardeoConstants.ID_TOKEN_LIFETIME);
            if (expiryTimeObject instanceof String) {
                if (!APIConstants.KeyManager.NOT_APPLICABLE_VALUE.equals(expiryTimeObject)) {
                    try {
                        long expiry = Long.parseLong((String) expiryTimeObject);

                        if (expiry < 0) {
                            throw new APIManagementException("Invalid application token lifetime given for "
                                    + applicationInfo.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setIdTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring ID token lifetime value as it is not a valid number");
                        }
                    }
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.PKCE_MANDATORY)) {
            Object pkceMandatoryValue =
                    additionalProperties.get(AsgardeoConstants.PKCE_MANDATORY);
            if (pkceMandatoryValue instanceof String) {
                if (!AsgardeoConstants.PKCE_MANDATORY.equals(pkceMandatoryValue)) {
                    boolean pkceMandatory = Boolean.parseBoolean((String) pkceMandatoryValue);
                    body.setPkceMandatory(pkceMandatory);
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.PKCE_SUPPORT_PLAIN)) {
            Object pkceSupportPlainValue =
                    additionalProperties.get(AsgardeoConstants.PKCE_SUPPORT_PLAIN);
            if (pkceSupportPlainValue instanceof String) {
                if (!AsgardeoConstants.PKCE_SUPPORT_PLAIN.equals(pkceSupportPlainValue)) {
                    boolean pkceSupportPlain = Boolean.parseBoolean((String) pkceSupportPlainValue);
                    body.setPkceSupportPlain(pkceSupportPlain);
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.PUBLIC_CLIENT)) {
            Object publicClientValue =
                    additionalProperties.get(AsgardeoConstants.PUBLIC_CLIENT);
            if (publicClientValue instanceof String) {
                if (!AsgardeoConstants.PUBLIC_CLIENT.equals(publicClientValue)) {
                    boolean pkceSupportPlain = Boolean.parseBoolean((String) publicClientValue);
                    body.setPublicClient(pkceSupportPlain);
                }
            }
        }
    }

    @Override
    public OAuthApplicationInfo updateApplicationOwner(OAuthAppRequest appInfoDTO, String owner)
            throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Application owner update is not supported for this key manager");
        }

        return null; //implementation is not applicable
    }

    @Override
    public void deleteApplication(String clientId) throws APIManagementException {

        try {
            dcrClient.delete(clientId);
        } catch (feign.FeignException e) {
            handleException("Cannot remove service provider for the given ID : " + clientId, e);
        }
    }

    /**
     * This method retrieves OAuth application details by given consumer key.
     *
     * @param clientId consumer key of the OAuth Client.
     * @return an {@code OAuthApplicationInfo} having all the details of an OAuth Client.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public OAuthApplicationInfo retrieveApplication(String clientId) throws APIManagementException {

        try {
            AsgardeoDCRClientInfo retrievedAsgardeoDCRClientInfo = dcrClient.get(clientId);

            if (retrievedAsgardeoDCRClientInfo == null) {
                return null;
            }

            return createOAuthApplicationInfo(retrievedAsgardeoDCRClientInfo);
        } catch (feign.FeignException e) {
            handleException("Cannot retrieve service provider for the given ID : " + clientId, e);
            return null;
        }
    }

    /**
     * Gets new access token and returns it in an AccessTokenInfo object.
     * <p>
     * Mirrors getNewApplicationAccessToken method in WSO2IS7KeyManager
     *
     * @param tokenRequest Info of the token needed.
     * @return AccessTokenInfo Info of the new token.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public AccessTokenInfo getNewApplicationAccessToken(AccessTokenRequest tokenRequest)
            throws APIManagementException {

        AccessTokenInfo tokenInfo;

        if (tokenRequest == null) {
            log.warn("No information available to generate Token.");
            return null;
        }

        //We do not revoke the previously obtained token anymore since we do not possess the access token.

        // When validity time set to a negative value, a token is considered never to expire.
        if (tokenRequest.getValidityPeriod() == OAuthConstants.UNASSIGNED_VALIDITY_PERIOD) {
            // Setting a different -ve value if the set value is -1 (-1 will be ignored by TokenValidator)
            tokenRequest.setValidityPeriod(-2L);
        }

        //Generate New Access Token
        String scopes = null;

        if (tokenRequest.getScope() != null) {
            scopes = String.join(" ", tokenRequest.getScope());
        }
        TokenInfo tokenResponse;

        try {
            String credentials = tokenRequest.getClientId() + ':' + tokenRequest.getClientSecret();
            String authToken = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            if (APIConstants.OAuthConstants.TOKEN_EXCHANGE.equals(tokenRequest.getGrantType())) {
                tokenResponse = authClient.generate(tokenRequest.getClientId(), tokenRequest.getClientSecret(),
                        tokenRequest.getGrantType(), scopes, (String) tokenRequest.getRequestParam(APIConstants
                                .OAuthConstants.SUBJECT_TOKEN), APIConstants.OAuthConstants.JWT_TOKEN_TYPE);
            } else {
                tokenResponse = authClient.generate(authToken, "client_credentials", scopes);
            }

        } catch (KeyManagerClientException e) {
            throw new APIManagementException("Error occurred while calling token endpoint - " + e.getReason(), e);
        }

        tokenInfo = new AccessTokenInfo();

        if (org.apache.commons.lang3.StringUtils.isNotEmpty(tokenResponse.getScope())) {
            tokenInfo.setScope(tokenResponse.getScope().split(" "));
        } else {
            tokenInfo.setScope(new String[0]);
        }
        tokenInfo.setAccessToken(tokenResponse.getToken());
        tokenInfo.setValidityPeriod(tokenResponse.getExpiry());

        return tokenInfo;
    }

    /**
     * This is used to get the metadata of the access token.
     *
     * @param accessToken AccessToken.
     * @return The metadata details of access token.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public AccessTokenInfo getTokenMetaData(String accessToken) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug(String.format("Getting access token metadata from authorization server. Access token %s",
                    accessToken));
        }

        AccessTokenInfo tokenInfo = new AccessTokenInfo();

        tokenInfo.setAccessToken(accessToken);
        tokenInfo.setKeyManager(getType());

        String credentials = mgmtClientId + ":" + mgmtClientSecret;
        String authToken = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        AsgardeoIntrospectionResponse response;

        try {
            response = introspectionClient.introspect(accessToken, authToken);
        } catch (feign.FeignException e) {
            tokenInfo.setTokenValid(false);
            tokenInfo.setTokenState(AsgardeoConstants.TOKEN_STATE_INTROSPECTION_FAILED);
            tokenInfo.setErrorcode(APIConstants.KeyValidationStatus.API_AUTH_GENERAL_ERROR);
            tokenInfo.addParameter("httpStatus", e.status());
            tokenInfo.addParameter("errorBody", e.contentUTF8());
            handleException("Introspection Failed!", e);
            return tokenInfo;
        }

        if (response == null || !response.isActive()) {
            tokenInfo.setTokenValid(false);
            tokenInfo.setTokenState(AsgardeoConstants.TOKEN_STATE_INACTIVE);
            tokenInfo.setErrorcode(APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
            return tokenInfo;
        }

        //token is active if hits here
        tokenInfo.setTokenValid(true);
        tokenInfo.setTokenState(AsgardeoConstants.TOKEN_STATE_ACTIVE);
        tokenInfo.setApplicationToken(AsgardeoConstants.TOKEN_APPLICATION.equalsIgnoreCase(response.getAut()));

        if (response.getClientId() != null) {
            tokenInfo.setConsumerKey(response.getClientId());
        }

        if (response.getScope() != null && !StringUtils.isBlank(response.getScope())) {
            tokenInfo.setScope(response.getScope().trim().split("\\s+"));
        } else {
            tokenInfo.setScope(new String[0]);
        }

        if (response.getIat() != null) {
            tokenInfo.setIssuedTime(response.getIat() * 1000L);
        }

        if (response.getExp() != null) { //expiry
            long validityMillis = Math.max(0, response.getExp() * 1000L - System.currentTimeMillis());
            tokenInfo.setValidityPeriod(validityMillis);
            tokenInfo.addParameter("exp", response.getExp() * 1000L);
        }

        tokenInfo.addParameter("token_type", response.getTokenType());
        if (response.getIss() != null) {
            tokenInfo.addParameter("iss", response.getIss());
        }
        if (response.getAud() != null) {
            tokenInfo.addParameter("aud", response.getAud());
        }
        if (response.getSub() != null) {
            tokenInfo.addParameter("sub", response.getSub());
        }
        if (response.getJti() != null) {
            tokenInfo.addParameter("jti", response.getJti());
        }

        return tokenInfo;
    }

    @Override
    public KeyManagerConfiguration getKeyManagerConfiguration() throws APIManagementException {

        return configuration;
    }

    @Override
    public OAuthApplicationInfo buildFromJSON(String s) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Building OAuth application information from JSON is not supported for this key manager.");
        }

        return null;
    }

    @Override
    public OAuthApplicationInfo mapOAuthApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {

        OAuthApplicationInfo toMap = oAuthAppRequest.getOAuthApplicationInfo();
        String clientId = toMap.getClientId();
        String clientSecret = toMap.getClientSecret();
        OAuthApplicationInfo retrieved = retrieveApplication(clientId);

        if (!retrieved.getClientSecret().equals(clientSecret)) {
            throw new APIManagementException("Error when mapping.");
        }
        return retrieved;
    }

    @Override
    public boolean registerNewResource(API api, Map resourceAttributes) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("New resource registration is not supported for this key manager");
        }

        return true;
    }

    @Override
    public Map getResourceByApiId(String apiId) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Resource retrieval by API ID is not supported for this key manager");
        }

        return null;
    }

    @Override
    public boolean updateRegisteredResource(API api, Map resourceAttributes) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Registered resource update is not supported for this key manager");
        }

        return true;
    }

    @Override
    public void deleteRegisteredResourceByAPIId(String apiID) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Resource deletion by API ID is not supported for this key manager");
        }
    }

    @Override
    public void deleteMappedApplication(String clientId) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Mapped application deletion is not supported for this key manager");
        }
    }

    @Override
    public Set<String> getActiveTokensByConsumerKey(String consumerKey) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Retrieving active tokens by consumer key is not supported for this key manager.");
        }

        return Collections.emptySet();
    }

    @Override
    public AccessTokenInfo getAccessTokenByConsumerKey(String consumerKey) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Retrieving access token information by consumer key is not supported for this key manager.");
        }

        return null;
    }

    @Override
    public String getNewApplicationConsumerSecret(AccessTokenRequest accessTokenRequest) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Generating a new application consumer secret is not supported for this key manager.");
        }

        return null;
    }

    @Override
    public Map<String, Set<Scope>> getScopesForAPIS(String apiIdsString) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Retrieving scopes for APIs is not supported for this key manager.");
        }

        return null;
    }

    @Override
    public void registerScope(Scope scope) throws APIManagementException {

        createScopesInAsgardeoResource(Collections.singleton(scope));
        createAsgardeoRoleToScopeBindings(Collections.singleton(scope));
    }

    @Override
    public Scope getScopeByName(String name) throws APIManagementException {

        return getAllScopes().get(name);
    }

    @Override
    public Map<String, Scope> getAllScopes() throws APIManagementException {

        Map<String, Scope> map = new HashMap<>();

        try {
            List<AsgardeoScopeResponse> scopes = apiResourceScopesClient.listScopes(globalApiResourceId);

            for (AsgardeoScopeResponse s : scopes) {
                Scope scope = new Scope();
                scope.setKey((s.getName()));
                scope.setName(s.getDisplayName());
                scope.setDescription(s.getDescription());
                map.put(scope.getKey(), scope);
            }

            return map;
        } catch (feign.FeignException e) {
            handleException("Error while retrieving scopes!", e);
        }
        return null;
    }

    @Override
    public void attachResourceScopes(API api, Set<URITemplate> uriTemplates) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Attaching resource scopes is not supported for this key manager.");
        }
    }

    @Override
    public void updateResourceScopes(API api, Set<String> oldLocalScopeKeys, Set<Scope> newLocalScopes,
                                     Set<URITemplate> oldURITemplates, Set<URITemplate> newURITemplates)
            throws APIManagementException {

        if (globalApiResourceId != null) {
            for (String oldScope : oldLocalScopeKeys) {
                deleteScope(oldScope);
            }
        }

        createScopesInAsgardeoResource(newLocalScopes);
        createAsgardeoRoleToScopeBindings(newLocalScopes);
    }

    private void createScopesInAsgardeoResource(Set<Scope> newLocalScopes) throws APIManagementException {

        if (globalApiResourceId == null) {
            try {
                globalApiResourceId = getAPIResourceId(globalApiResourceName);
            } catch (APIManagementException e) {
                log.error("Couldn't find global resource", e);
                return;
            }
        }

        AsgardeoScopePatchRequest scopesToBeUpdated = new AsgardeoScopePatchRequest(newLocalScopes);

        try {
            apiResourceClient.addScopes(globalApiResourceId, scopesToBeUpdated);
        } catch (FeignException e) {
            handleException("Failed to add scopes to Global API Resource", e);
        }
    }

    @Override
    public void detachResourceScopes(API api, Set<URITemplate> uriTemplates) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Detaching resource scopes is not supported for this key manager.");
        }
    }

    @Override
    public void deleteScope(String scopeName) throws APIManagementException {

        try {
            apiResourceScopesClient.deleteScope(globalApiResourceId, scopeName);
        } catch (feign.FeignException e) {
            handleException("Failed to delete scope: " + scopeName + " from Asgardeo API Resource: " +
                    AsgardeoConstants.GLOBAL_API_RESOURCE_NAME, e);
        }
    }

    @Override
    public void updateScope(Scope scope) throws APIManagementException {

        try {
            if (globalApiResourceId == null) {
                globalApiResourceId = getAPIResourceId(globalApiResourceName);
            }

            if (globalApiResourceId != null) {
                AsgardeoScopeUpdateRequest scopeInfo = new AsgardeoScopeUpdateRequest();
                scopeInfo.setDisplayName(scope.getName());
                scopeInfo.setDescription(scope.getDescription());

                try {
                    apiResourceScopesClient.updateScope(globalApiResourceId, scope.getKey(),
                            scopeInfo);
                } catch (feign.FeignException e) {
                    handleException("Failed to update scope: " + scope.getName() + " Asgardeo API Resource: " +
                            AsgardeoConstants.GLOBAL_API_RESOURCE_NAME, e);
                }
            }
            syncRoleBindingsToScope(scope);
        } catch (KeyManagerClientException e) {
            handleException("Failed to update scope: " + scope.getName(), e);
        }
    }

    @Override
    public boolean isScopeExists(String scopeName) throws APIManagementException {

        return getScopeByName(scopeName) != null;
    }

    @Override
    public void validateScopes(Set<Scope> scopes) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Scopes validation is not supported for this key manager.");
        }
    }

    @Override
    public String getType() {

        return AsgardeoConstants.ASGARDEO_TYPE;
    }

    private String getAPIResourceId(String globalApiResourceName) throws APIManagementException {

        try {
            AsgardeoAPIResourceListResponse page = apiResourceClient
                    .listAPIResources(globalApiResourceName);
            if (page.getApiResources() != null) {
                for (AsgardeoAPIResourceResponse r : page.getApiResources()) {
                    if (globalApiResourceName.equals(r.getName())) {
                        log.info("Asgardeo Global API Resource discovered successfully");
                        return r.getId();
                    }
                }
            }
        } catch (feign.FeignException e) {
            handleException("Failed to fetch API Resources from Asgardeo", e);
        }

        globalApiResourceId = null;

        throw new APIManagementException("Failed to locate global API Resource on Asgardeo");

    }

    /**
     * Converts an API Manager role name into the corresponding Asgardeo role name
     *
     * @param roleName The role name received from WSO2 API Manager.
     * @return The transformed role name compatible with Asgardeo.
     * @throws APIManagementException If the role name violates the expected naming rules.
     */
    private String toAsgardeoRoleName(String roleName) throws APIManagementException {

        if (!enableRoleCreation) {
            return roleName;
        }
        // When role creation is enabled, conventions of the WSO2 IS7 migration client are followed for roles.
        if (roleName.startsWith("Internal/")) {
            return roleName.replace("Internal/", StringUtils.EMPTY);
        } else if (roleName.startsWith("Application/")) {
            throw new APIManagementException("Role: " + roleName + " is invalid.");
        }
        return "apim_primary_" + roleName;
    }

    /**
     * Retrieves the Asgardeo role ID corresponding to the given role display name.
     * <p>
     * Mirrors the getWSO2IS7RoleId method in the WSO2IS7KeyManager implementation
     *
     * @param roleDisplayName The display name of the role to search for in Asgardeo.
     * @return The ID of the matching role, or null if no role is found.
     * @throws APIManagementException If an error occurs while communicating with Asgardeo.
     */
    private String getAsgardeoRoleIdByName(String roleDisplayName) throws APIManagementException {

        String filter = "displayName eq " + roleDisplayName;
        try {
            JsonArray roles = searchRoles(filter);

            if (roles != null && !roles.isJsonNull() && !roles.isEmpty()) {
                return roles.get(0).getAsJsonObject().get("id").getAsString();
            }
        } catch (feign.FeignException e) {
            handleException("Error when searching for roles", e);
        }
        return null;
    }

    /**
     * Executes a SCIM role search request against Asgardeo using the given filter.
     * <p>
     * Mirrors searchRoles on WSO2IS7KeyManager
     * <p>
     * This method constructs a SCIM 2.0 search request payload, including the
     * required schema identifier and the provided filter expression. It then
     * invokes the Asgardeo SCIM Roles client to retrieve matching roles.
     *
     * @param filter The SCIM filter expression used to query roles. If null,
     *               the search request is sent without a filter.
     * @return A JsonArray containing the matching role resources.
     * @throws feign.FeignException If an error occurs while invoking the
     *                              Asgardeo SCIM API.
     */
    private JsonArray searchRoles(String filter) throws feign.FeignException {

        JsonObject payload = new JsonObject();
        JsonArray schemas = new JsonArray();
        schemas.add(AsgardeoConstants.SEARCH_REQUEST_SCHEMA);
        payload.add("schemas", schemas);
        if (filter != null) {
            payload.addProperty("filter", filter);
        }
        JsonObject rolesResponse = asgardeoSCIMRolesClient.searchRoles(payload);
        return rolesResponse.getAsJsonArray("Resources");
    }

    /**
     * Extracts the list of role names associated with the given scope.
     * <p>
     * Mirrors the getRoles method in the WSO2IS7KeyManager implementation.
     *
     * @param scope The scope containing role information as a comma-separated string.
     * @return A list of role names, or an empty list if no roles are defined.
     */
    private List<String> getRoles(Scope scope) {

        if (org.apache.commons.lang3.StringUtils.isNotBlank(scope.getRoles()) &&
                scope.getRoles().trim().split(",").length > 0) {
            return Arrays.asList(scope.getRoles().trim().split(","));
        }
        return Collections.emptyList();
    }

    /**
     * Creates a new role in Asgardeo with the given display name and permissions.
     * <p>
     * Mirrors the createWSO2IS7Role method in the WSO2IS7KeyManager implementation.
     *
     * @param displayName The display name of the role to be created.
     * @param scopes      The list of permission mappings to be associated with the role.
     * @throws APIManagementException If role creation fails while communicating with Asgardeo.
     */
    private void createAsgardeoRole(String displayName, List<Map<String, String>> scopes)
            throws APIManagementException {

        AsgardeoRoleInfo role = new AsgardeoRoleInfo();
        role.setDisplayName(displayName);
        role.setPermissions(scopes);
        try {
            asgardeoSCIMRolesClient.createRole(role);
        } catch (feign.FeignException e) {
            handleException("Failed to create role: " + displayName, e);
        }
    }

    /**
     * Adds the given scope as a permission to the specified Asgardeo role.
     * <p>
     * Mirrors the addScopeToWSO2IS7Role method in the WSO2IS7KeyManager implementation.
     * <p>
     * Retrieves the existing role, preserves its current permissions, adds the
     * new scope as an additional permission, and updates the role accordingly.
     *
     * @param scope  The scope to be added to the role as a permission.
     * @param roleId The ID of the role to be updated.
     * @throws APIManagementException If updating the role fails while communicating with Asgardeo.
     */
    private void addScopeToAsgardeoRole(Scope scope, String roleId) throws APIManagementException {

        try {
            AsgardeoRoleInfo role = asgardeoSCIMRolesClient.getRole(roleId);
            List<Map<String, String>> permissions = role.getPermissions();

            List<AsgardeoPatchRoleOperationInfo.Permission> allPermissions = new ArrayList<>();
            for (Map<String, String> existingPermission : permissions) {
                AsgardeoPatchRoleOperationInfo.Permission permission = new AsgardeoPatchRoleOperationInfo.Permission();
                permission.setValue(existingPermission.get("value"));
                permission.setDisplay(existingPermission.get("display"));
                allPermissions.add(permission);
            }
            AsgardeoPatchRoleOperationInfo.Permission addedPermission = new AsgardeoPatchRoleOperationInfo.Permission();
            addedPermission.setValue(scope.getKey());
            addedPermission.setDisplay(scope.getName());
            allPermissions.add(addedPermission);

            updateAsgardeoRoleWithScopes(roleId, allPermissions);
        } catch (feign.FeignException e) {
            handleException("Failed to add scope: " + scope.getKey() + " to the role with ID: " + roleId, e);
        }
    }

    /**
     * Replaces the permissions of the specified Asgardeo role with the provided list of scopes.
     * <p>
     * Mirrors the updateWSO2IS7RoleWithScopes method in the WSO2IS7KeyManager implementation.
     *
     * @param roleId The ID of the role to be updated.
     * @param scopes The complete list of permissions to be set on the role.
     * @throws feign.FeignException If updating the role fails while communicating with Asgardeo.
     */
    private void updateAsgardeoRoleWithScopes(String roleId, List<AsgardeoPatchRoleOperationInfo.Permission> scopes)
            throws feign.FeignException {

        AsgardeoPatchRoleOperationInfo.Value value = new AsgardeoPatchRoleOperationInfo.Value();
        value.setPermissions(scopes);

        AsgardeoPatchRoleOperationInfo.Operation replaceOperation =
                new AsgardeoPatchRoleOperationInfo.Operation();
        replaceOperation.setOp("replace");
        replaceOperation.setValue(value);

        AsgardeoPatchRoleOperationInfo patchOperationInfo = new AsgardeoPatchRoleOperationInfo();
        patchOperationInfo.setOperations(Collections.singletonList(replaceOperation));
        asgardeoSCIMRolesClient.patchRole(roleId, patchOperationInfo);
    }

    /**
     * Removes the given scope from the specified roles in Asgardeo.
     * <p>
     * Mirrors the removeWSO2IS7RoleToScopeBindings method in the WSO2IS7KeyManager implementation.
     * <p>
     * For each role, retrieves the existing permissions, excludes the specified
     * scope, and updates the role with the remaining permissions.
     *
     * @param scopeName The name (value) of the scope to be removed.
     * @param roles     The list of role names from which the scope should be removed.
     * @throws APIManagementException If updating any role fails while communicating with Asgardeo.
     */
    private void removeAsgardeoRoleToScopeBindings(String scopeName, List<String> roles) throws APIManagementException {

        for (String role : roles) {
            try {
                String roleName = toAsgardeoRoleName(role);
                String roleId = getAsgardeoRoleIdByName(roleName);
                if (roleId != null) {
                    AsgardeoRoleInfo roleInfo = asgardeoSCIMRolesClient.getRole(roleId);
                    List<Map<String, String>> existingScopes = roleInfo.getPermissions();

                    // Update the role with all the existing scopes(permissions) except the given scope(permission)
                    List<AsgardeoPatchRoleOperationInfo.Permission> permissions = new ArrayList<>();
                    for (Map<String, String> existingScope : existingScopes) {
                        if (!scopeName.equals(existingScope.get("value"))) {
                            AsgardeoPatchRoleOperationInfo.Permission permission =
                                    new AsgardeoPatchRoleOperationInfo.Permission();
                            permission.setValue(existingScope.get("value"));
                            permissions.add(permission);
                        }
                    }
                    updateAsgardeoRoleWithScopes(roleId, permissions);
                }
            } catch (feign.FeignException e) {
                handleException("Failed to remove role-to-scope bindings for role: " + role, e);
            }
        }
    }

    /**
     * Creates role-to-scope bindings in Asgardeo for the given set of scopes.
     * <p>
     * Mirrors the createWSO2IS7RoleToScopeBindings method in the WSO2IS7KeyManager implementation.
     * <p>
     * For each scope, iterates through its associated roles. If the role exists
     * in Asgardeo, the scope is added as a permission. If the role does not exist
     * and role creation is enabled, a new role is created with the scope as its
     * initial permission.
     *
     * @param scopes The set of scopes containing role bindings to be synchronized.
     * @throws APIManagementException If role lookup, creation, or update fails while
     *                                communicating with Asgardeo.
     */
    private void createAsgardeoRoleToScopeBindings(Set<Scope> scopes) throws APIManagementException {

        for (Scope scope : scopes) {
            List<String> roles = getRoles(scope);
            for (String apimRole : roles) {
                String asgardeoRoleName = toAsgardeoRoleName(apimRole);
                try {
                    String roleId = getAsgardeoRoleIdByName(asgardeoRoleName);
                    if (roleId != null) {
                        // Add this scope(permission) to existing role
                        addScopeToAsgardeoRole(scope, roleId);
                    } else if (enableRoleCreation) {
                        // Create new role with this scope(permission)
                        Map<String, String> asgardeoScope = new HashMap<>();
                        asgardeoScope.put("value", scope.getKey());
                        asgardeoScope.put("display", scope.getName());
                        createAsgardeoRole(asgardeoRoleName, Collections.singletonList(asgardeoScope));
                    }
                } catch (feign.FeignException e) {
                    handleException("Failed to get the role ID for role: " + apimRole, e);
                }
            }
        }
    }

    /**
     * Retrieves the list of Asgardeo role names that contain the given scope as a permission.
     * <p>
     * Mirrors the getWSO2IS7RolesHavingScope method in the WSO2IS7KeyManager implementation.
     * <p>
     * Iterates through the provided roles and returns the display names of roles
     * that include the specified scope in their permissions.
     *
     * @param scopeName The name (value) of the scope to check within role permissions.
     * @param roles     The JsonArray of role objects retrieved from Asgardeo.
     * @return A list of role display names that have the specified scope.
     */
    private List<String> getAsgardeoRolesHavingScope(String scopeName, JsonArray roles) {

        List<String> scopeRoles = new ArrayList<>();
        if (roles != null && !roles.isJsonNull()) {
            for (JsonElement role : roles) {
                JsonArray permissions = role.getAsJsonObject().getAsJsonArray("permissions");
                if (permissions != null && !permissions.isJsonNull()) {
                    for (JsonElement permission : permissions) {
                        if (scopeName.equals(permission.getAsJsonObject().get("value").getAsString())) {
                            // This role has the given scope(permission)
                            scopeRoles.add(role.getAsJsonObject().get("displayName").getAsString());
                            break;
                        }
                    }
                }
            }
        }
        return scopeRoles;
    }

    /**
     * Converts Asgardeo role names into their corresponding API Manager role names.
     * <p>
     * Mirrors the getAPIMRolesFromIS7Roles method in the WSO2IS7KeyManager implementation.
     * <p>
     * Removes the "apim_primary_" prefix from roles created by API Manager.
     * For other roles, prefixes them with "Internal/" to match API Manager
     * role naming conventions.
     *
     * @param asgardeoRoles The list of role names retrieved from Asgardeo.
     * @return A list of role names formatted for API Manager.
     */
    private List<String> toAPIMRolesNames(List<String> asgardeoRoles) {

        return asgardeoRoles.stream()
                .map(roleName -> roleName.startsWith("apim_primary_")
                        ? roleName.replaceFirst("^apim_primary_", StringUtils.EMPTY)
                        : "Internal/" + roleName)
                .collect(Collectors.toList());
    }

    /**
     * Synchronizes role-to-scope bindings in Asgardeo with the current state of the given scope.
     * <p>
     * Mirrors the role-binding update logic in the updateScopes method of the
     * WSO2IS7KeyManager implementation.
     * <p>
     * Determines which role bindings need to be added or removed by comparing
     * the roles defined in API Manager with the existing bindings in Asgardeo,
     * then updates Asgardeo accordingly.
     *
     * @param scope The scope whose role bindings should be synchronized.
     * @throws KeyManagerClientException If a client-side error occurs during synchronization.
     * @throws APIManagementException    If role lookup or update fails while communicating with Asgardeo.
     */
    private void syncRoleBindingsToScope(Scope scope) throws KeyManagerClientException, APIManagementException {

        JsonArray allRoles = searchRoles(null);
        List<String> existingAPIMRoles = toAPIMRolesNames(
                getAsgardeoRolesHavingScope(scope.getKey(), allRoles));

        // Add new scope-to-role bindings
        List<String> apimScopeRoles = getRoles(scope);

        List<String> apimRoleBindingsToAdd = new ArrayList<>(apimScopeRoles);
        apimRoleBindingsToAdd.removeAll(existingAPIMRoles);

        if (!apimRoleBindingsToAdd.isEmpty()) {
            Scope addableScope = new Scope();
            addableScope.setKey(scope.getKey());
            addableScope.setName(scope.getName());
            addableScope.setDescription(scope.getDescription());
            addableScope.setRoles(String.join(",", apimRoleBindingsToAdd));
            createAsgardeoRoleToScopeBindings(Collections.singleton(addableScope));
        }

        // Remove old scope-to-role bindings
        List<String> roleBindingsToRemove = new ArrayList<>(existingAPIMRoles);
        roleBindingsToRemove.removeAll(apimScopeRoles);
        if (!roleBindingsToRemove.isEmpty()) {
            removeAsgardeoRoleToScopeBindings(scope.getKey(), roleBindingsToRemove);
        }
    }

    //copied from WSO2IS7KeyManager
    @Override
    protected void validateOAuthAppCreationProperties(OAuthApplicationInfo oAuthApplicationInfo)
            throws APIManagementException {

        super.validateOAuthAppCreationProperties(oAuthApplicationInfo);

        String type = getType();
        KeyManagerConnectorConfiguration keyManagerConnectorConfiguration = ServiceReferenceHolder.getInstance()
                .getKeyManagerConnectorConfiguration(type);
        if (keyManagerConnectorConfiguration != null) {
            Object additionalProperties = oAuthApplicationInfo.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
            if (additionalProperties != null) {
                JsonObject additionalPropertiesJson = (JsonObject) new JsonParser()
                        .parse((String) additionalProperties);
                for (Map.Entry<String, JsonElement> entry : additionalPropertiesJson.entrySet()) {
                    String additionalProperty = entry.getValue().getAsString();
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(additionalProperty) &&
                            !org.apache.commons.lang3.StringUtils
                                    .equals(additionalProperty, APIConstants.KeyManager.NOT_APPLICABLE_VALUE)) {
                        try {
                            if (AsgardeoConstants.PKCE_MANDATORY.equals(entry.getKey()) ||
                                    AsgardeoConstants.PKCE_SUPPORT_PLAIN.equals(entry.getKey()) ||
                                    AsgardeoConstants.PUBLIC_CLIENT.equals(entry.getKey())) {

                                if (!(additionalProperty.equalsIgnoreCase(Boolean.TRUE.toString()) ||
                                        additionalProperty.equalsIgnoreCase(Boolean.FALSE.toString()))) {
                                    String errMsg = "Application configuration values cannot have negative values.";
                                    throw new APIManagementException(errMsg, ExceptionCodes
                                            .from(ExceptionCodes.INVALID_APPLICATION_ADDITIONAL_PROPERTIES, errMsg));
                                }
                            } else {
                                long longValue = Long.parseLong(additionalProperty);
                                if (longValue < 0) {
                                    String errMsg = "Application configuration values cannot have negative values.";
                                    throw new APIManagementException(errMsg, ExceptionCodes
                                            .from(ExceptionCodes.INVALID_APPLICATION_ADDITIONAL_PROPERTIES, errMsg));
                                }
                            }
                        } catch (NumberFormatException e) {
                            String errMsg = "Application configuration values cannot have string values.";
                            throw new APIManagementException(errMsg, ExceptionCodes
                                    .from(ExceptionCodes.INVALID_APPLICATION_ADDITIONAL_PROPERTIES, errMsg));
                        }
                    }
                }
            }
        }
    }
}
