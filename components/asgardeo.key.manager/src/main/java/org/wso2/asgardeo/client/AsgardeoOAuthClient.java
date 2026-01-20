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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.wso2.asgardeo.client.model.*;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.AbstractKeyManager;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.kmclient.FormEncoder;
import org.wso2.asgardeo.client.model.AsgardeoAccessTokenResponse;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * This class provides the implementation to use "Custom" Authorization Server for managing
 * OAuth clients and Tokens needed by WSO2 API Manager.
 */
public class AsgardeoOAuthClient extends AbstractKeyManager {

    private static final Log log = LogFactory.getLog(AsgardeoOAuthClient.class);
    private AsgardeoTokenClient tokenClient;
    private AsgardeoDCRClient dcrClient;

    /**
     * {@code APIManagerComponent} calls this method, passing KeyManagerConfiguration as a {@code String}.
     *
     * @param keyManagerConfiguration Configuration as a {@link KeyManagerConfiguration}
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public void loadConfiguration(KeyManagerConfiguration keyManagerConfiguration) throws APIManagementException {

        this.configuration = keyManagerConfiguration;

        String org = (String) configuration.getParameter(AsgardeoConstants.ORG_NAME);
        // COME BACK base url is hardcoded
        String baseURL = "https://api.asgardeo.io";

        String clientId = (String) configuration.getParameter(AsgardeoConstants.MGMT_CLIENT_ID);
        String clientSecret = (String) configuration.getParameter(AsgardeoConstants.MGMT_CLIENT_SECRET);

        String dcrEndpoint;
        if(configuration.getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT) != null)
            dcrEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT);
        else
            dcrEndpoint = baseURL + "/t/" + org + "/api/identity/oauth2/dcr/v1.1/register";

        String tokenEndpoint;
        if(configuration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT) != null)
            tokenEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT);
        else
            tokenEndpoint = baseURL + "/t/" + org + "/oauth2/token";


        tokenClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new FormEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .target(org.wso2.asgardeo.client.model.AsgardeoTokenClient.class, tokenEndpoint);

        AsgardeoDCRAuthInterceptor interceptor = new AsgardeoDCRAuthInterceptor(tokenClient, clientId, clientSecret);

        dcrClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoDCRClient.class, dcrEndpoint);
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


        String appName = in.getClientName();
        String keyType = (String) in.getParameter(ApplicationConstants.APP_KEY_TYPE);
        String user = (String) in.getParameter(ApplicationConstants.OAUTH_CLIENT_USERNAME);

        String clientName = (user != null ? user : "apim") + "_" + appName + (keyType != null ? "_" + keyType : "");

        AsgardeoDCRClientInfo body =  new AsgardeoDCRClientInfo();

        body.setClientName(clientName);

        List<String> grantTypes = new ArrayList<>();
        if (in.getParameter(APIConstants.JSON_GRANT_TYPES) != null) {
            grantTypes = Arrays.asList(((String) in.getParameter(APIConstants.JSON_GRANT_TYPES))
                    .split(","));
        }
        body.setGrantTypes(grantTypes);

        body.setRedirectUris(java.util.Collections.singletonList("https://localhost"));

        try {
            AsgardeoDCRClientInfo created = dcrClient.create(body);

            return createOAuthApplicationInfo(created);
        } catch (KeyManagerClientException e) {
           handleException("Cannot create OAuth Application: "+clientName+ " for Application "+appName, e);
           return null;
        }
    }

    @NotNull
    private static OAuthApplicationInfo createOAuthApplicationInfo(AsgardeoDCRClientInfo dcrClient) {
        OAuthApplicationInfo out = new OAuthApplicationInfo();
        out.setClientName(dcrClient.getClientName());
        out.setClientId(dcrClient.getClientId());
        out.setClientSecret(dcrClient.getClientSecret());
        out.addParameter(ApplicationConstants.OAUTH_CLIENT_ID, dcrClient.getClientId());
        out.addParameter(ApplicationConstants.OAUTH_CLIENT_SECRET, dcrClient.getClientSecret());

        if (dcrClient.getGrantTypes() != null && dcrClient.getGrantTypes().size() > 0) {
            out.addParameter(APIConstants.JSON_GRANT_TYPES, String.join(" ", dcrClient.getGrantTypes()));
        }
        return out;
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

        //todo update oauth app in the authorization server

        return null;
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

            if(retrieved == null)
                return null;

            return createOAuthApplicationInfo(retrieved);
        } catch (KeyManagerClientException e) {
            handleException("Cannot retrieve service provider for the given ID : "+clientId, e);
            return null;
        }

    }

    /**
     * Gets new access token and returns it in an AccessTokenInfo object.
     *
     * @param accessTokenRequest Info of the token needed.
     * @return AccessTokenInfo Info of the new token.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public AccessTokenInfo getNewApplicationAccessToken(AccessTokenRequest accessTokenRequest)
            throws APIManagementException {

        String clientID = accessTokenRequest.getClientId();
        String clientSecret = accessTokenRequest.getClientSecret();

        //COME BACK grant type default to client cred for mvp
        String grantType = accessTokenRequest.getGrantType() != null
                ? accessTokenRequest.getGrantType() : "client_credentials";

        //scopes handling if APIM passes a string[]
        String scope = "";
        if (accessTokenRequest.getScope() != null && accessTokenRequest.getScope().length > 0) {
            scope = String.join(" ", accessTokenRequest.getScope());
        }

        String basicCredentials = getEncodedCredentials(clientID, clientSecret);

        AsgardeoAccessTokenResponse retrievedToken = tokenClient.getAccessToken(grantType, scope, basicCredentials);

        if(retrievedToken == null || retrievedToken.getAccessToken() == null || retrievedToken.getAccessToken().isEmpty())
            throw new APIManagementException("Asgardeoeo token endpoint returned an empty token!");

        // mapping response to apim  model
        AccessTokenInfo response = new AccessTokenInfo();
        response.setConsumerKey(clientID);
        response.setConsumerSecret(clientSecret);
        response.setAccessToken(retrievedToken.getAccessToken());
        response.setValidityPeriod(retrievedToken.getExpiry());

        if (retrievedToken.getScope() != null && !retrievedToken.getScope().isBlank()) {
            response.setScope(retrievedToken.getScope().trim().split("\\s+"));
        }
        return response;
    }

    /**
     * This is used to build accesstoken request from OAuth application info.
     *
     * @param oAuthApplication OAuth application details.
     * @param tokenRequest     AccessTokenRequest that is need to be updated with addtional info.
     * @return AccessTokenRequest after adding OAuth application details.
     * @throws APIManagementException This is the custom exception class for API management.
     */
    @Override
    public AccessTokenRequest buildAccessTokenRequestFromOAuthApp(
            OAuthApplicationInfo oAuthApplication, AccessTokenRequest tokenRequest) throws APIManagementException {

        log.debug("Invoking buildAccessTokenRequestFromOAuthApp() method..");
        if (oAuthApplication == null) {
            return tokenRequest;
        }
        if (tokenRequest == null) {
            tokenRequest = new AccessTokenRequest();
        }
       // todo implement logic to build an access token request

        return tokenRequest;
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
// todo implemnt logic to get access token meta data from the introspect endpoint
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

        return oAuthAppRequest.getOAuthApplicationInfo();
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

        Map<String, Set<Scope>> apiToScopeMapping = new HashMap<>();
        ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
        Map<String, Set<String>> apiToScopeKeyMapping = apiMgtDAO.getScopesForAPIS(apiIdsString);
        for (String apiId : apiToScopeKeyMapping.keySet()) {
            Set<Scope> apiScopes = new LinkedHashSet<>();
            Set<String> scopeKeys = apiToScopeKeyMapping.get(apiId);
            for (String scopeKey : scopeKeys) {
                Scope scope = getScopeByName(scopeKey);
                apiScopes.add(scope);
            }
            apiToScopeMapping.put(apiId, apiScopes);
        }
        return apiToScopeMapping;
    }

    @Override
    public void registerScope(Scope scope) throws APIManagementException {

    }

    @Override
    public Scope getScopeByName(String name) throws APIManagementException {

        return null;
    }

    @Override
    public Map<String, Scope> getAllScopes() throws APIManagementException {

        return null;
    }

    @Override
    public void attachResourceScopes(API api, Set<URITemplate> uriTemplates) throws APIManagementException {

    }

    @Override
    public void updateResourceScopes(API api, Set<String> oldLocalScopeKeys, Set<Scope> newLocalScopes,
                                     Set<URITemplate> oldURITemplates, Set<URITemplate> newURITemplates)
            throws APIManagementException {

    }

    @Override
    public void detachResourceScopes(API api, Set<URITemplate> uriTemplates) throws APIManagementException {

    }

    @Override
    public void deleteScope(String scopeName) throws APIManagementException {

    }

    @Override
    public void updateScope(Scope scope) throws APIManagementException {

    }

    @Override
    public boolean isScopeExists(String scopeName) throws APIManagementException {

        return false;
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
}
