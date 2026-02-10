/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.asgardeo.client;

import com.google.gson.*;
import feign.FeignException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.wso2.asgardeo.client.model.*;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.AbstractKeyManager;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.kmclient.FormEncoder;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;
import org.wso2.carbon.apimgt.impl.kmclient.model.AuthClient;
import org.wso2.carbon.apimgt.impl.kmclient.model.TokenInfo;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class provides the implementation to use "Custom" Authorization Server for managing
 * OAuth clients and Tokens needed by WSO2 API Manager.
 */
public class AsgardeoOAuthClient extends AbstractKeyManager {

    private static final Log log = LogFactory.getLog(AsgardeoOAuthClient.class);

    private AuthClient authClient;
    private AsgardeoTokenClient tokenClient;
    private AsgardeoDCRClient dcrClient;
    private AsgardeoAppClient appClient;
    // private AsgardeoOIDCInboundClient oidcInboundClient;
    private AsgardeoIntrospectionClient introspectionClient;
    private AsgardeoAPIResourceClient apiResourceClient;
    private AsgardeoAPIResourceScopesClient apiResourceScopesClient;
    private AsgardeoSCIMRolesClient asgardeoSCIMRolesClient;

    private Map<String, String> appIdMap;
    private final Map<String, String> scopeNameToIdMap = new ConcurrentHashMap<>();


    private String mgmtClientId, mgmtClientSecret; //client id and secret of app management api authorized
    private String globalApiResourceId;

    private boolean issueJWTTokens;
    private boolean enableRoleCreation;

    /**
     * {@code APIManagerComponent} calls this method, passing KeyManagerConfiguration as a {@code String}.
     *
     * @param keyManagerConfiguration Configuration as a {@link KeyManagerConfiguration}
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public void loadConfiguration(KeyManagerConfiguration keyManagerConfiguration) throws APIManagementException {

        this.configuration = keyManagerConfiguration;

        appIdMap = new ConcurrentHashMap<>(); //map for app id
        String org = (String) configuration.getParameter(AsgardeoConstants.ORG_NAME);
        // COME BACK base url is hardcoded
        String baseURL = "https://api.asgardeo.io";

        mgmtClientId = (String) configuration.getParameter(AsgardeoConstants.MGMT_CLIENT_ID);
        mgmtClientSecret = (String) configuration.getParameter(AsgardeoConstants.MGMT_CLIENT_SECRET);

        if (configuration.getParameter(AsgardeoConstants.ACCESS_TOKEN_TYPE) != null)
            issueJWTTokens = (boolean) configuration.getParameter(AsgardeoConstants.ACCESS_TOKEN_TYPE);
        else issueJWTTokens = false; //default to opaque

        if (configuration.getParameter(AsgardeoConstants.ENABLE_ROLE_CREATION) instanceof Boolean) {
            enableRoleCreation = (Boolean) configuration.getParameter(AsgardeoConstants.ENABLE_ROLE_CREATION);
        } else enableRoleCreation = false;

        String dcrEndpoint;
        if (configuration.getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT) != null)
            dcrEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT);
        else
            dcrEndpoint = baseURL + "/t/" + org + "/api/identity/oauth2/dcr/v1.1/register";

        String tokenEndpoint;
        if (configuration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT) != null)
            tokenEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT);
        else
            tokenEndpoint = baseURL + "/t/" + org + "/oauth2/token";

        addKeyManagerConfigsAsSystemProperties(tokenEndpoint);

        String introspectionEndpoint;
        if (configuration.getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT) != null)
            introspectionEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT);
        else
            introspectionEndpoint = baseURL + "/t/" + org + "/oauth2/introspect";

        String rolesEndpoint;
        if (configuration.getParameter(AsgardeoConstants.ROLES_MANAGEMENT_ENDPOINT) != null) {
            rolesEndpoint = (String) configuration.getParameter(AsgardeoConstants.ROLES_MANAGEMENT_ENDPOINT);
        } else {
            rolesEndpoint = baseURL + "/t/" + org + "/scim2/v2/Roles";
        }

        //  Application management API endpoint and API  resource endpoint
        String applicationsServerBase;
        if (configuration.getParameter(AsgardeoConstants.APPLICATION_MANAGEMENT_ENDPOINT) != null) {
            applicationsServerBase = (String) configuration.getParameter(AsgardeoConstants.APPLICATION_MANAGEMENT_ENDPOINT);
        } else
            applicationsServerBase = baseURL + "/t/" + org + "/api/server/v1/application";

        String apiResourceServerBase;
        if (configuration.getParameter(AsgardeoConstants.RESOURCE_MANAGEMENT_ENDPOINT) != null) {
            apiResourceServerBase = (String) configuration.getParameter(AsgardeoConstants.RESOURCE_MANAGEMENT_ENDPOINT);
        } else
            apiResourceServerBase = baseURL + "/t/" + org + "/api/server/v1/api-resources";

        tokenClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new FormEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .target(AsgardeoTokenClient.class, tokenEndpoint);

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


        AsgardeoDCRAuthInterceptor interceptor = new AsgardeoDCRAuthInterceptor(tokenClient, mgmtClientId, mgmtClientSecret);

        dcrClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoDCRClient.class, dcrEndpoint);

        appClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoAppClient.class, applicationsServerBase);

        // not needed as JWT token type can be set through a parameter in the DCR payload. but kept this in case we need
        // to use this client in the future
//        oidcInboundClient = feign.Feign.builder()
//                .client(new feign.okhttp.OkHttpClient())
//                .encoder(new feign.gson.GsonEncoder())
//                .decoder(new feign.gson.GsonDecoder())
//                .logger(new feign.slf4j.Slf4jLogger())
//                .requestInterceptor(interceptor)
//                .target(AsgardeoOIDCInboundClient.class, applicationsServerBase);

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

        globalApiResourceId = doesAPIResourceExistAndGetId();
    }

    private void addKeyManagerConfigsAsSystemProperties(String serviceUrl) {

        URL keyManagerURL;
        try {
            keyManagerURL = new URL(serviceUrl);
            String hostname = keyManagerURL.getHost();

            int port = keyManagerURL.getPort();
            if (port == -1) {
                if (APIConstants.HTTPS_PROTOCOL.equals(keyManagerURL.getProtocol())) {
                    port = APIConstants.HTTPS_PROTOCOL_PORT;
                } else {
                    port = APIConstants.HTTP_PROTOCOL_PORT;
                }
            }
            System.setProperty(APIConstants.KEYMANAGER_PORT, String.valueOf(port));

            if (hostname.equals(System.getProperty(APIConstants.CARBON_LOCALIP))) {
                System.setProperty(APIConstants.KEYMANAGER_HOSTNAME, "localhost");
            } else {
                System.setProperty(APIConstants.KEYMANAGER_HOSTNAME, hostname);
            }
            //Since this is the server startup.Ignore the exceptions,invoked at the server startup
        } catch (MalformedURLException e) {
            log.error("Exception While resolving KeyManager Server URL or Port " + e.getMessage(), e);
        }
    }

    /**
     * This method will Register an OAuth client in Custom Authorization Server.
     *
     * @param oAuthAppRequest This object holds all parameters required to register an OAuth client.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public OAuthApplicationInfo createApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {

        OAuthApplicationInfo in = oAuthAppRequest.getOAuthApplicationInfo();


        AsgardeoDCRClientInfo body = getClientInfo(in);
        setAdditionalPropertiesToClient(in, body);
        AsgardeoDCRClientInfo created;
        try {

            created = dcrClient.create(body);

        } catch (KeyManagerClientException e) {
            handleException("Cannot create OAuth Application: " + body.getClientName() + " for Application " + in.getClientName(), e);
            return null;
        }

        try {
            authorizeAPItoApp(created);
        } catch (APIManagementException e) {
            handleException("Couldn't authorize scopes API resource to OAuthApplication: " + body.getClientName(), e);
        }
        return createOAuthApplicationInfo(created);
    }

    @NotNull
    private AsgardeoDCRClientInfo getClientInfo(OAuthApplicationInfo in) {
        String appName = in.getClientName();
        String appId = in.getApplicationUUID();
        String keyType = (String) in.getParameter(ApplicationConstants.APP_KEY_TYPE);
        String user = (String) in.getParameter(ApplicationConstants.OAUTH_CLIENT_USERNAME);

        String clientName = (user != null ? user : "apim") + "_" + appName + "_" + appId.substring(0, 7) + (keyType != null ? "_" + keyType : "");
        AsgardeoDCRClientInfo body = new AsgardeoDCRClientInfo();

        body.setClientName(clientName);

        List<String> grantTypes = getGrantTypesFromOAuthApp(in);
        body.setGrantTypes(grantTypes);
        //  log.info("APIM Callback uRL : "+in.getCallBackURL());
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(in.getCallBackURL())) {
            String callBackURL = in.getCallBackURL();
            String[] callbackURLs = callBackURL.trim().split("\\s*,\\s*");
            body.setRedirectUris(Arrays.asList(callbackURLs));
        }

        if (issueJWTTokens)
            body.setTokenTypeAsJWT();
        return body;
    }

    private void authorizeAPItoApp(AsgardeoDCRClientInfo created) throws APIManagementException {
        String appId = resolveAppIdByClientId(created.getClientId());

        try {
            appClient.authorizeAPItoApp(appId, new AsgardeoAPIAuthRequest(globalApiResourceId));
        } catch (FeignException e) {
            handleException("Couldn't Authorize Resource API to OAuth Application with Application Id: " + appId, e);
        }
    }

    // no longer required as DCR call can do this
//    private void tryChangeAccessTokenToJWT(AsgardeoDCRClientInfo created, OAuthAppRequest oAuthAppRequest) throws APIManagementException {
//        try{
//            String appId = resolveAppIdByClientId(created.getClientId());
//
//            AsgardeoOIDCInboundRequest inboundRequest = buildInboundPayload(created, oAuthAppRequest);
//
//            try {
//                oidcInboundClient.updateOidcInbound(appId, inboundRequest);
//            }catch(feign.FeignException e){
//                log.warn("Failed to update OIDC config to JWT for client ID : "+created.getClientId()
//                + " (app ID : "+appId+"). Falling back to Opaque type. HTTP "+e.status() +" body=" +e.contentUTF8());
//            }
//
//            created.setId(appId);
//        }catch(APIManagementException e){
//            handleException("Could not change access token of service provider with ID : "+created.getClientId(), e);
//        }
//    }

//    private AsgardeoOIDCInboundRequest buildInboundPayload(AsgardeoDCRClientInfo created, OAuthAppRequest oAuthAppRequest) {
//        AsgardeoOIDCInboundRequest toBeBuilt = new AsgardeoOIDCInboundRequest();
//
//        toBeBuilt.setClientId(created.getClientId());
//        toBeBuilt.setGrantTypes(created.getGrantTypes());
//        toBeBuilt.setAllowedOrigins(Collections.emptyList());
//
//        AsgardeoOIDCInboundRequest.AccessToken accessToken = new AsgardeoOIDCInboundRequest.AccessToken("JWT", 3600, 3600);
//
//        toBeBuilt.setAccessToken(accessToken);
//        return toBeBuilt;
//    }

    // this method also provides fast lookup if the app id exists
    private String resolveAppIdByClientId(String clientId) throws APIManagementException {
        int limit = 300;
        int offset = 0;

        String appId = appIdMap.get(clientId); //first go through the map to see if it exists

        if (appId != null)
            return appId;

        // will loop through the results 300 at a time to be safe
        while (true) {
            AsgardeoApplicationsResponse page = appClient.list(limit, "clientId", offset);

            if (page.getApplications() != null) {
                for (AsgardeoApplicationsResponse.App app : page.getApplications()) {

                    String retrievedClientId = app.getClientId();
                    appIdMap.put(retrievedClientId, app.getId());

                    if (clientId.equals(retrievedClientId)) {
                        return app.getId();
                    }
                }
            }

            int returnedResultsCount = page.getCount();
            if (returnedResultsCount <= 0) break;

            offset += returnedResultsCount;
        }

        throw new APIManagementException("Could not find Asgardeo Application ID for Client ID : " + clientId);
    }

    @NotNull
    private static List<String> getGrantTypesFromOAuthApp(OAuthApplicationInfo in) {
        List<String> grantTypes = new ArrayList<>();
        if (in.getParameter(APIConstants.JSON_GRANT_TYPES) != null) {
            grantTypes = Arrays.asList(((String) in.getParameter(APIConstants.JSON_GRANT_TYPES))
                    .split(","));
        }
        return grantTypes;
    }

    @NotNull
    private static OAuthApplicationInfo createOAuthApplicationInfo(AsgardeoDCRClientInfo dcrClient) {
        OAuthApplicationInfo out = new OAuthApplicationInfo();
        out.setClientName(dcrClient.getClientName());
        out.setClientId(dcrClient.getClientId());
        out.setClientSecret(dcrClient.getClientSecret());
        out.addParameter(ApplicationConstants.OAUTH_CLIENT_ID, dcrClient.getClientId());
        out.addParameter(ApplicationConstants.OAUTH_CLIENT_SECRET, dcrClient.getClientSecret());
        out.addParameter(ApplicationConstants.OAUTH_CLIENT_NAME, dcrClient.getClientName());

        if (dcrClient.getGrantTypes() != null && dcrClient.getGrantTypes().size() > 0) {
            out.addParameter(APIConstants.JSON_GRANT_TYPES, String.join(" ", dcrClient.getGrantTypes()));
        }

        if (dcrClient.getRedirectUris() != null && !dcrClient.getRedirectUris().isEmpty()) {
            out.setCallBackURL(String.join(",", dcrClient.getRedirectUris()));
        }

        Map<String, Object> additionalProperties = getAdditionalPropertiesFromClient(dcrClient);

        additionalProperties.put(AsgardeoConstants.APPLICATION_TOKEN_LIFETIME, dcrClient.getApplicationTokenLifetime());
        additionalProperties.put(AsgardeoConstants.USER_TOKEN_LIFETIME, dcrClient.getUserTokenLifetime());
        additionalProperties.put(AsgardeoConstants.ID_TOKEN_LIFETIME, dcrClient.getIdTokenLifetime());
        additionalProperties.put(AsgardeoConstants.REFRESH_TOKEN_LIFETIME, dcrClient.getRefreshTokenLifetime());

        additionalProperties.put(AsgardeoConstants.PKCE_MANDATORY, dcrClient.isPkceMandatory());
        additionalProperties.put(AsgardeoConstants.PKCE_SUPPORT_PLAIN, dcrClient.isPkcePlainText());

        additionalProperties.put(AsgardeoConstants.PUBLIC_CLIENT, dcrClient.isPublicClient());

        out.addParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES, additionalProperties);

        return out;
    }

    @NotNull
    private static Map<String, Object> getAdditionalPropertiesFromClient(AsgardeoDCRClientInfo dcrClient) {
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put(AsgardeoConstants.APPLICATION_TOKEN_LIFETIME,
                dcrClient.getApplicationTokenLifetime());
        additionalProperties.put(AsgardeoConstants.USER_TOKEN_LIFETIME,
                dcrClient.getUserTokenLifetime());
        additionalProperties.put(AsgardeoConstants.REFRESH_TOKEN_LIFETIME,
                dcrClient.getRefreshTokenLifetime());
        additionalProperties.put(AsgardeoConstants.ID_TOKEN_LIFETIME, dcrClient.getIdTokenLifetime());

        //put app id in if needed later
        if (dcrClient.getId() != null)
            additionalProperties.put("asgardeoAppId", dcrClient.getId());
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

        OAuthApplicationInfo in = oAuthAppRequest.getOAuthApplicationInfo();

        AsgardeoDCRClientInfo body = getClientInfo(in);

        setAdditionalPropertiesToClient(in, body);


        try {
            AsgardeoDCRClientInfo updated = dcrClient.update(in.getClientId(), body);

//            try {
//                String appId = resolveAppIdByClientId(in.getClientId());
//                updated.setId(appId);
//            }catch (APIManagementException e){
//                System.out.println("Couldn't find app Id");
//            }

            return createOAuthApplicationInfo(updated);
        } catch (KeyManagerClientException e) {
            handleException("Could not update service provider with ID: " + in.getClientId(), e);
            return null;
        }

    }

    private void setAdditionalPropertiesToClient(OAuthApplicationInfo in, AsgardeoDCRClientInfo body) throws APIManagementException {
        Object parameter = in.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);

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
                                    + in.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setApplicationTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        // No need to throw as its due to not a number sent.
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
                                    + in.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setUserTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        // No need to throw as its due to not a number sent.
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
                                    + in.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setRefreshTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        // No need to throw as its due to not a number sent.
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
                                    + in.getClientName(), ExceptionCodes.INVALID_APPLICATION_PROPERTIES);
                        }
                        body.setIdTokenLifetime(expiry);
                    } catch (NumberFormatException e) {
                        // No need to throw as its due to not a number sent.
                    }
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.PKCE_MANDATORY)) {
            Object pkceMandatoryValue =
                    additionalProperties.get(AsgardeoConstants.PKCE_MANDATORY);
            if (pkceMandatoryValue instanceof String) {
                if (!AsgardeoConstants.PKCE_MANDATORY.equals(pkceMandatoryValue)) {
                    try {
                        Boolean pkceMandatory = Boolean.parseBoolean((String) pkceMandatoryValue);
                        body.setPkceMandatory(pkceMandatory);
                    } catch (NumberFormatException e) {
                        // No need to throw as its due to not a number sent.
                    }
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.PKCE_SUPPORT_PLAIN)) {
            Object pkceSupportPlainValue =
                    additionalProperties.get(AsgardeoConstants.PKCE_SUPPORT_PLAIN);
            if (pkceSupportPlainValue instanceof String) {
                if (!AsgardeoConstants.PKCE_SUPPORT_PLAIN.equals(pkceSupportPlainValue)) {
                    try {
                        Boolean pkceSupportPlain = Boolean.parseBoolean((String) pkceSupportPlainValue);
                        body.setPkceSupportPlain(pkceSupportPlain);
                    } catch (NumberFormatException e) {
                        // No need to throw as its due to not a number sent.
                    }
                }
            }
        }

        if (additionalProperties.containsKey(AsgardeoConstants.PUBLIC_CLIENT)) {
            Object publicClientValue =
                    additionalProperties.get(AsgardeoConstants.PUBLIC_CLIENT);
            if (publicClientValue instanceof String) {
                if (!AsgardeoConstants.PUBLIC_CLIENT.equals(publicClientValue)) {
                    try {
                        Boolean pkceSupportPlain = Boolean.parseBoolean((String) publicClientValue);
                        body.setPublicClient(pkceSupportPlain);
                    } catch (NumberFormatException e) {
                        // No need to throw as its due to not a number sent.
                    }
                }
            }
        }
    }

    @Override
    public OAuthApplicationInfo updateApplicationOwner(OAuthAppRequest appInfoDTO, String owner)
            throws APIManagementException {

        return null;
    }

    /**
     * Deletes OAuth Client from Authorization Server.
     *
     * @param clientId consumer key of the OAuth Client.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public void deleteApplication(String clientId) throws APIManagementException {
        try {
            dcrClient.delete(clientId);
        } catch (KeyManagerClientException e) {
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
            AsgardeoDCRClientInfo retrieved = dcrClient.get(clientId);

            if (retrieved == null)
                return null;

//            try {
//                String appId = resolveAppIdByClientId(clientId);
//                retrieved.setId(appId);
//            }catch (APIManagementException e){
//                System.out.println("Couldn't find app Id");
//            }

            return createOAuthApplicationInfo(retrieved);
        } catch (KeyManagerClientException e) {
            handleException("Cannot retrieve service provider for the given ID : " + clientId, e);
            return null;
        }

    }

    /**
     * Gets new access token and returns it in an AccessTokenInfo object.
     *
     * @param tokenRequest Info of the token needed.
     * @return AccessTokenInfo Info of the new token.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public AccessTokenInfo getNewApplicationAccessToken(AccessTokenRequest tokenRequest)
            throws APIManagementException {

//        String clientID = accessTokenRequest.getClientId();
//        String clientSecret = accessTokenRequest.getClientSecret();
//
//        //COME BACK grant type default to client cred for mvp
//        String grantType = accessTokenRequest.getGrantType() != null
//                ? accessTokenRequest.getGrantType() : "client_credentials";
//
//        //scopes handling if APIM passes a string[]
//        String scope = "";
//        if (accessTokenRequest.getScope() != null && accessTokenRequest.getScope().length > 0) {
//            scope = String.join(" ", accessTokenRequest.getScope());
//        }
//
//        String basicCredentials = getEncodedCredentials(clientID, clientSecret);
//
//        AsgardeoAccessTokenResponse retrievedToken = tokenClient.getAccessToken(grantType, scope, basicCredentials);
//
//        if(retrievedToken == null || retrievedToken.getAccessToken() == null || retrievedToken.getAccessToken().isEmpty())
//            throw new APIManagementException("Asgardeo token endpoint returned an empty token!");
//
//        // mapping response to apim  model
//        AccessTokenInfo response = new AccessTokenInfo();
//        response.setConsumerKey(clientID);
//        response.setConsumerSecret(clientSecret);
//        response.setAccessToken(retrievedToken.getAccessToken());
//        response.setValidityPeriod(retrievedToken.getExpiry());
//        response.setKeyManager(getType());
//
//        if (retrievedToken.getScope() != null && !StringUtils.isBlank(retrievedToken.getScope())) {
//            response.setScope(retrievedToken.getScope().trim().split("\\s+"));
//        }
//        return response;
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
        if (tokenRequest.getScope() != null)
            scopes = String.join(" ", tokenRequest.getScope());
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
     * This is used to get the meta data of the accesstoken.
     *
     * @param accessToken AccessToken.
     * @return The meta data details of accesstoken.
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
        tokenInfo.setTokenState("UNKNOWN"); //default value changes later

        String basicCredentials = getEncodedCredentials(mgmtClientId, mgmtClientSecret);

        AsgardeoIntrospectionResponse response;

        try {
            response = introspectionClient.introspect(accessToken, basicCredentials);
        } catch (KeyManagerClientException e) {
            throw new APIManagementException("Error occurred in token introspection!", e);
        } catch (FeignException e) {

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

        if (response.getClientId() != null)
            tokenInfo.setConsumerKey(response.getClientId());

        if (response.getScope() != null && !StringUtils.isBlank(response.getScope()))
            tokenInfo.setScope(response.getScope().trim().split("\\s+"));
        else
            tokenInfo.setScope(new String[0]);

        if (response.getIat() != null) //issued at
            tokenInfo.setIssuedTime(response.getIat() * 1000L);

        if (response.getExp() != null) { //expiry
            long validityMillis = Math.max(0, response.getExp() * 1000L - System.currentTimeMillis());
            tokenInfo.setValidityPeriod(validityMillis);
            tokenInfo.addParameter("exp", response.getExp() * 1000L); //keeping it in parameters optionally
        }

        tokenInfo.addParameter("token_type", response.getTokenType());
        if (response.getIss() != null) tokenInfo.addParameter("iss", response.getIss());
        if (response.getAud() != null) tokenInfo.addParameter("aud", response.getAud());
        if (response.getSub() != null) tokenInfo.addParameter("sub", response.getSub());
        if (response.getJti() != null) tokenInfo.addParameter("jti", response.getJti());

        return tokenInfo;
    }

    @Override
    public KeyManagerConfiguration getKeyManagerConfiguration() throws APIManagementException {

        return configuration;
    }

    @Override
    public OAuthApplicationInfo buildFromJSON(String s) throws APIManagementException {

        return null;
    }

    /**
     * This method will be called when mapping existing OAuth Clients with Application in API Manager
     *
     * @param oAuthAppRequest Details of the OAuth Client to be mapped.
     * @return {@code OAuthApplicationInfo} with the details of the mapped client.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public OAuthApplicationInfo mapOAuthApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {

        OAuthApplicationInfo toMap = oAuthAppRequest.getOAuthApplicationInfo();
        String clientId = toMap.getClientId();
        String clientSecret = toMap.getClientSecret();
        OAuthApplicationInfo retrieved = retrieveApplication(clientId);

        if (!retrieved.getClientSecret().equals(clientSecret))
            throw new APIManagementException("Error when mapping.");
        return retrieved;
    }

    @Override
    public boolean registerNewResource(API api, Map resourceAttributes) throws APIManagementException {

        // invoke APIResource registration endpoint of the authorization server and creates a new resource.

        return true;
    }

    @Override
    public Map getResourceByApiId(String apiId) throws APIManagementException {

        //  retrieves the registered resource by the given API ID from the  APIResource registration endpoint.

        return null;
    }

    @Override
    public boolean updateRegisteredResource(API api, Map resourceAttributes) throws APIManagementException {

        return true;
    }

    @Override
    public void deleteRegisteredResourceByAPIId(String apiID) throws APIManagementException {
    }

    @Override
    public void deleteMappedApplication(String clientId) throws APIManagementException {
    }

    @Override
    public Set<String> getActiveTokensByConsumerKey(String s) throws APIManagementException {

        return Collections.emptySet();
    }

    @Override
    public AccessTokenInfo getAccessTokenByConsumerKey(String s) throws APIManagementException {

        return null;
    }

    @Override
    public String getNewApplicationConsumerSecret(AccessTokenRequest accessTokenRequest) throws APIManagementException {

        return null;
    }

    @Override
    public Map<String, Set<Scope>> getScopesForAPIS(String apiIdsString) throws APIManagementException {

//        Map<String, Set<Scope>> apiToScopeMapping = new HashMap<>();
//        ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
//        Map<String, Set<String>> apiToScopeKeyMapping = apiMgtDAO.getScopesForAPIS(apiIdsString);
//        for (String apiId : apiToScopeKeyMapping.keySet()) {
//            Set<Scope> apiScopes = new LinkedHashSet<>();
//            Set<String> scopeKeys = apiToScopeKeyMapping.get(apiId);
//            for (String scopeKey : scopeKeys) {
//                Scope scope = getScopeByName(scopeKey);
//                apiScopes.add(scope);
//            }
//            apiToScopeMapping.put(apiId, apiScopes);
//        }
//        return apiToScopeMapping;
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

        List<AsgardeoScopeResponse> scopes = apiResourceScopesClient.listScopes(globalApiResourceId);

        for (AsgardeoScopeResponse s : scopes) {
            scopeNameToIdMap.put((s.getName()), s.getId());

            Scope scope = new Scope();
            scope.setKey((s.getName()));
            scope.setName(s.getDisplayName());
            scope.setDescription(s.getDescription());
            map.put(scope.getKey(), scope);
        }

        return map;
    }

    @Override
    public void attachResourceScopes(API api, Set<URITemplate> uriTemplates) throws APIManagementException {

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

    private void createScopesInAsgardeoResource(Set<Scope> newLocalScopes) {
        if (globalApiResourceId == null) {
            try {
                globalApiResourceId = doesAPIResourceExistAndGetId();
            } catch (APIManagementException e) {
                log.error("Couldn't find global resource", e);
                return;
            }
        }
        // List<AsgardeoScopeResponse> fetchedScopes = apiResourceScopesClient.listScopes(globalApiResourceId);

        AsgardeoScopePatchRequest scopesToBeUpdated = new AsgardeoScopePatchRequest(newLocalScopes);

        apiResourceClient.addScopes(globalApiResourceId, scopesToBeUpdated);

//        if (created != null && created.getId() != null) {
//            scopeNameToIdMap.put(created.getName(), created.getId());
//        }
    }


//    private String addScopePrefix(String scope){
//        return AsgardeoConstants.SCOPE_PREFIX.concat(scope);
//    }
//
//    private String removeScopePrefix(String scope){
//        return scope.split(AsgardeoConstants.SCOPE_PREFIX)[1];
//    }

    @Override
    public void detachResourceScopes(API api, Set<URITemplate> uriTemplates) throws APIManagementException {

    }

    @Override
    public void deleteScope(String scopeName) throws APIManagementException {
        try {
            apiResourceScopesClient.deleteScope(globalApiResourceId, scopeName);
            scopeNameToIdMap.remove(scopeName);
        } catch (KeyManagerClientException e) {
            handleException("Failed to delete scope: " + scopeName + " from WSO2 IS7 API Resource: " +
                    AsgardeoConstants.GLOBAL_API_RESOURCE_NAME, e);
        }

        // cleaner: remove the scope (permission) from roles too
//        try {
//            JsonArray roles = searchRoles(null);
//            List<String> filteredRoles = getAsgardeoRolesHavingScope(scopeName, roles);
//            removeWSO2IS7RoleToScopeBindings(scopeName, filteredRoles);
//        } catch (KeyManagerClientException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void updateScope(Scope scope) throws APIManagementException {

        try {
            if (globalApiResourceId == null)
                globalApiResourceId = doesAPIResourceExistAndGetId();
            if (globalApiResourceId != null) {
                AsgardeoScopeUpdateRequest scopeInfo = new AsgardeoScopeUpdateRequest();
                scopeInfo.setDisplayName(scope.getName());
                scopeInfo.setDescription(scope.getDescription());
                try {
                    apiResourceScopesClient.updateScope(globalApiResourceId, scope.getKey(),
                            scopeInfo);
                } catch (KeyManagerClientException e) {
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

    }

    @Override
    public String getType() {

        return AsgardeoConstants.ASGARDEO_TYPE;
    }

    public static String getEncodedCredentials(String clientId, String clientSecret) throws APIManagementException {

        String encodedCredentials;
        try {
            encodedCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret)
                    .getBytes(AsgardeoConstants.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new APIManagementException(AsgardeoConstants.ERROR_ENCODING_METHOD_NOT_SUPPORTED, e);
        }

        return encodedCredentials;
    }

    private String doesAPIResourceExistAndGetId() throws APIManagementException {
        int limit = 100;

        AsgardeoAPIResourceListResponse page = apiResourceClient.listAPIResources(limit, AsgardeoConstants.GLOBAL_API_RESOURCE_NAME);
        if (page.getApiResources() != null)
            for (AsgardeoAPIResourceResponse r : page.getApiResources())
                if (AsgardeoConstants.GLOBAL_API_RESOURCE_NAME.equals(r.getName()))
                    return r.getId();

        //if not found, create it
        AsgardeoAPIResourceCreateRequest req = new AsgardeoAPIResourceCreateRequest();
        req.setName(AsgardeoConstants.GLOBAL_API_RESOURCE_NAME);
        req.setIdentifier(AsgardeoConstants.GLOBAL_API_RESOURCE_IDENTIFIER);

        AsgardeoAPIResourceResponse created = apiResourceClient.createAPIResource(req);
        if (created == null || created.getId() == null) {
            throw new APIManagementException("Failed to create global API resource in Asgardeo.");
        }

        return created.getId();
    }

    //Roles related

    //Name + lookup helpers

    //mirrors getWSO2IS7RoleName
    // naming convention
    private String toAsgardeoRoleName(String roleName) throws APIManagementException {
        if (!enableRoleCreation) {
            return roleName;
        }
        // When role creation is enabled, conventions of the WSO2 IS7 migration client are followed for roles.
        if (roleName.startsWith("Internal/")) {
            return roleName.replace("Internal/", "");
        } else if (roleName.startsWith("Application/")) {
            throw new APIManagementException("Role: " + roleName + " is invalid.");
        }
        return "apim_primary_" + roleName;
    }

    //mirrors getWSO2IS7RoleId
    // search for an Asgardeo Role with a filter on the Display name
    private String getAsgardeoRoleIdByName(String roleDisplayName) throws KeyManagerClientException {

        String filter = "displayName eq " + roleDisplayName;
        JsonArray roles = searchRoles(filter);
        if (roles != null && !roles.isJsonNull() && roles.size() > 0) {
            return roles.get(0).getAsJsonObject().get("id").getAsString();
        }
        return null;
    }

    //copied from IS 7 KM
    // searches Asgardeo Roles adding a filter if given
    private JsonArray searchRoles(String filter) throws KeyManagerClientException {

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

    //copied from IS7 KM
    // gets the roles from a scope as a list of strings
    private List<String> getRoles(Scope scope) {

        if (org.apache.commons.lang3.StringUtils.isNotBlank(scope.getRoles()) && scope.getRoles().trim().split(",").length > 0) {
            return Arrays.asList(scope.getRoles().trim().split(","));
        }
        return Collections.emptyList();
    }

    //role creation and permission primitives

    //mirrors createWSO2IS7Role
    // creates a role in Asgardeo
    private void createAsgardeoRole(String displayName, List<Map<String, String>> scopes) throws APIManagementException {

        AsgardeoRoleInfo role = new AsgardeoRoleInfo();
        role.setDisplayName(displayName);
        role.setPermissions(scopes);
        try {
            asgardeoSCIMRolesClient.createRole(role);
        } catch (KeyManagerClientException e) {
            handleException("Failed to create role: " + displayName, e);
        }
    }

    //mirrors addScopeToWSO2IS7Role
    // fetches existing permissions as scopes from the role found by role id and creates a new list of permissions including
    // the scope to add. then updates
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
        } catch (KeyManagerClientException e) {
            handleException("Failed to add scope: " + scope.getKey() + " to the role with ID: " + roleId, e);
        }
    }

    //mirrors updateWSO2IS7RoleWithScopes
    //replaces the permissions with the list of new permissions
    private void updateAsgardeoRoleWithScopes(String roleId, List<AsgardeoPatchRoleOperationInfo.Permission> scopes)
            throws KeyManagerClientException {
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

    //mirrors removeWSO2IS7RoleToScopeBindings
    // fetches existing permissions as scopes from the role and creates a new list of permissions excluding the one to remove
    //then updates
    private void removeWSO2IS7RoleToScopeBindings(String scopeName, List<String> roles) throws APIManagementException {
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
            } catch (KeyManagerClientException e) {
                handleException("Failed to remove role-to-scope bindings for role: " + role, e);
            }
        }
    }

    // binding roles and scopes

    //mirrors createWSO2IS7RoleToScopeBindings
    // looks at every role inside every scope passed to this.
    // then if Asgardeo has that role, adds scope to the role on Asgardeo
    // if Asgardeo does not have that role, create the role setting the scope as a permission if role creation is enabled
    private void createAsgardeoRoleToScopeBindings(Set<Scope> scopes) throws APIManagementException {

        for (Scope scope : scopes) {
            List<String> roles = getRoles(scope);
            for (String apimRole : roles) {
                String is7RoleName = toAsgardeoRoleName(apimRole);
                try {
                    String roleId = getAsgardeoRoleIdByName(is7RoleName);
                    if (roleId != null) {
                        // Add this scope(permission) to existing role
                        addScopeToAsgardeoRole(scope, roleId);
                    } else if (enableRoleCreation) {
                        // Create new role with this scope(permission)
                        Map<String, String> wso2IS7Scope = new HashMap<>();
                        wso2IS7Scope.put("value", scope.getKey());
                        wso2IS7Scope.put("display", scope.getName());
                        createAsgardeoRole(is7RoleName, Collections.singletonList(wso2IS7Scope));
                    }
                } catch (KeyManagerClientException e) {
                    handleException("Failed to get the role ID for role: " + apimRole, e);
                }
            }
        }
    }

    // update reconciliation

    //mirrors getWSO2IS7RolesHavingScope
    // finds and returns a list of roles from Asgardeo having the scope (permission) passed to the function
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

    //mirrors getAPIMRolesFromIS7Roles
    private List<String> toAPIMRolesNames(List<String> is7Roles) {
        return is7Roles.stream()
                .map(roleName -> roleName.startsWith("apim_primary_")
                        ? roleName.replaceFirst("^apim_primary_", "")
                        : "Internal/" + roleName)
                .collect(Collectors.toList());
    }

    //mirrors part of the method updateScopes
    // if roles of a scope have changed, they will be updated and bound on Asgardeo
    // likewise, remove the scope-role bindings of the roles that were removed from the scope
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
            removeWSO2IS7RoleToScopeBindings(scope.getKey(), roleBindingsToRemove);
        }
    }

    //copied from IS7 key manager
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
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(additionalProperty) && !org.apache.commons.lang3.StringUtils
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
                                Long longValue = Long.parseLong(additionalProperty);
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
