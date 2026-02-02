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

import feign.FeignException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.wso2.asgardeo.client.model.*;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.AbstractKeyManager;
import org.wso2.carbon.apimgt.impl.kmclient.FormEncoder;
import org.wso2.asgardeo.client.model.AsgardeoAccessTokenResponse;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides the implementation to use "Custom" Authorization Server for managing
 * OAuth clients and Tokens needed by WSO2 API Manager.
 */
public class AsgardeoOAuthClient extends AbstractKeyManager {

    private static final Log log = LogFactory.getLog(AsgardeoOAuthClient.class);

    private AsgardeoTokenClient tokenClient;
    private AsgardeoDCRClient dcrClient;
    private AsgardeoAppListClient appListClient;
   // private AsgardeoOIDCInboundClient oidcInboundClient;
    private AsgardeoIntrospectionClient introspectionClient;
    private AsgardeoAPIResourceClient apiResourceClient;
    private AsgardeoAPIResourceScopesClient apiResourceScopesClient;

    private Map<String, String> appIdMap;
    private final Map<String, String> scopeNameToIdMap = new ConcurrentHashMap<>();


    private String mgmtClientId, mgmtClientSecret; //client id and secret of app management api authorized
    private String globalApiResourceId;

    private boolean issueJWTTokens;

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

        if(configuration.getParameter(AsgardeoConstants.ACCESS_TOKEN_TYPE) != null)
            issueJWTTokens = (boolean) configuration.getParameter(AsgardeoConstants.ACCESS_TOKEN_TYPE);
        else issueJWTTokens = false; //default to opaque
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

        String introspectionEndpoint;
        if(configuration.getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT) != null)
            introspectionEndpoint = (String) configuration.getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT);
        else
            introspectionEndpoint = baseURL + "/t/" + org + "/oauth2/introspect";

        // for JWT conversion - Application management API endpoint and API  resource endpoint
        String applicationsServerBase = baseURL + "/t/" + org + "/api/server/v1";

        tokenClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new FormEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .target(AsgardeoTokenClient.class, tokenEndpoint);

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

        appListClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoAppListClient.class, applicationsServerBase);

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
                .target(AsgardeoAPIResourceClient.class, applicationsServerBase);

        apiResourceScopesClient = feign.Feign.builder()
                .client(new feign.okhttp.OkHttpClient())
                .encoder(new feign.gson.GsonEncoder())
                .decoder(new feign.gson.GsonDecoder())
                .logger(new feign.slf4j.Slf4jLogger())
                .requestInterceptor(interceptor)
                .target(AsgardeoAPIResourceScopesClient.class, applicationsServerBase);

        globalApiResourceId = doesAPIResourceExistAndGetId();
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
        String appId = in.getApplicationUUID();
        String keyType = (String) in.getParameter(ApplicationConstants.APP_KEY_TYPE);
        String user = (String) in.getParameter(ApplicationConstants.OAUTH_CLIENT_USERNAME);

        String clientName = (user != null ? user : "apim") + "_" + appName + "_" + appId.substring(0, 7)+ (keyType != null ? "_" + keyType : "");

        AsgardeoDCRClientInfo body =  new AsgardeoDCRClientInfo();

        body.setClientName(clientName);

        List<String> grantTypes = getGrantTypesFromOAuthApp(in);
        body.setGrantTypes(grantTypes);
      //  log.info("APIM Callback uRL : "+in.getCallBackURL());
        // TODO this is still hardcoded
        body.setRedirectUris(java.util.Collections.singletonList("https://localhost:9443"));

        try {

            if (issueJWTTokens)
                body.setTokenTypeAsJWT();

            AsgardeoDCRClientInfo created = dcrClient.create(body);

            authorizeAPItoApp(created);

            return createOAuthApplicationInfo(created);
        } catch (KeyManagerClientException e) {
           handleException("Cannot create OAuth Application: "+clientName+ " for Application "+appName, e);
           return null;
        }
    }

    private void authorizeAPItoApp(AsgardeoDCRClientInfo created) throws APIManagementException {
        String appId = resolveAppIdByClientId(created.getClientId());

        try {
            apiResourceClient.authorizeAPItoApp(appId, new AsgardeoAPIAuthRequest(globalApiResourceId));
        }catch (FeignException e){
            handleException("Couldn't Authorize Resource API to OAuth Application with Application Id: "+appId, e);
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
    private String resolveAppIdByClientId(String clientId) throws APIManagementException{
        int limit = 300;
        int offset = 0;

        String appId = appIdMap.get(clientId); //first go through the map to see if it exists

        if(appId != null)
            return appId;

        // will loop through the results 300 at a time to be safe
        while(true){
            AsgardeoApplicationsResponse page = appListClient.list(limit, "clientId", offset);

            if (page.getApplications() != null){
                for(AsgardeoApplicationsResponse.App app : page.getApplications()){

                    String retrievedClientId = app.getClientId();
                    appIdMap.put(retrievedClientId, app.getId());

                    if(clientId.equals(retrievedClientId)){
                        return app.getId();
                    }
                }
            }

            int returnedResultsCount = page.getCount();
            if(returnedResultsCount <= 0) break;

            offset += returnedResultsCount;
        }

        throw new APIManagementException("Could not find Asgardeo Application ID for Client ID : "+clientId);
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

        if (dcrClient.getGrantTypes() != null && dcrClient.getGrantTypes().size() > 0) {
            out.addParameter(APIConstants.JSON_GRANT_TYPES, String.join(" ", dcrClient.getGrantTypes()));
        }

        if(dcrClient.getRedirectUris() != null && !dcrClient.getRedirectUris().isEmpty()){
            out.setCallBackURL(String.join(",", dcrClient.getRedirectUris()));
        }

        //put app id in OAuthApplicationInfo if needed later
        if(dcrClient.getId() != null)
            out.addParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES, new HashMap<>().put("asgardeoAppId", dcrClient.getId()));
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

        OAuthApplicationInfo in = oAuthAppRequest.getOAuthApplicationInfo();

        AsgardeoDCRClientInfo body = new AsgardeoDCRClientInfo();

        body.setGrantTypes(getGrantTypesFromOAuthApp(in));

        if(in.getCallBackURL() != null && !StringUtils.isBlank(in.getCallBackURL())){
            body.setRedirectUris(Arrays.asList(in.getCallBackURL().split(",")));
        }

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
            handleException("Could not update service provider with ID: "+in.getClientId(), e);
            return null;
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

            if(retrieved == null)
                return null;

//            try {
//                String appId = resolveAppIdByClientId(clientId);
//                retrieved.setId(appId);
//            }catch (APIManagementException e){
//                System.out.println("Couldn't find app Id");
//            }

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
            throw new APIManagementException("Asgardeo token endpoint returned an empty token!");

        // mapping response to apim  model
        AccessTokenInfo response = new AccessTokenInfo();
        response.setConsumerKey(clientID);
        response.setConsumerSecret(clientSecret);
        response.setAccessToken(retrievedToken.getAccessToken());
        response.setValidityPeriod(retrievedToken.getExpiry());
        response.setKeyManager(getType());

        if (retrievedToken.getScope() != null && !StringUtils.isBlank(retrievedToken.getScope())) {
            response.setScope(retrievedToken.getScope().trim().split("\\s+"));
        }
        return response;
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
        } catch(FeignException e){

            tokenInfo.setTokenValid(false);
            tokenInfo.setTokenState(AsgardeoConstants.TOKEN_STATE_INTROSPECTION_FAILED);
            tokenInfo.setErrorcode(APIConstants.KeyValidationStatus.API_AUTH_GENERAL_ERROR);
            tokenInfo.addParameter("httpStatus", e.status());
            tokenInfo.addParameter("errorBody", e.contentUTF8());
            handleException("Introspection Failed!", e);
            return tokenInfo;
        }

        if(response == null || !response.isActive()){
            tokenInfo.setTokenValid(false);
            tokenInfo.setTokenState(AsgardeoConstants.TOKEN_STATE_INACTIVE);
            tokenInfo.setErrorcode(APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
            return tokenInfo;
        }

        //token is active if hits here
        tokenInfo.setTokenValid(true);
        tokenInfo.setTokenState(AsgardeoConstants.TOKEN_STATE_ACTIVE);
        tokenInfo.setApplicationToken(AsgardeoConstants.TOKEN_APPLICATION.equalsIgnoreCase(response.getAut()));

        if(response.getClientId() != null)
            tokenInfo.setConsumerKey(response.getClientId());

        if(response.getScope() != null && !StringUtils.isBlank(response.getScope()))
            tokenInfo.setScope(response.getScope().trim().split("\\s+"));
        else
            tokenInfo.setScope(new String[0]);

        if(response.getIat() != null) //issued at
            tokenInfo.setIssuedTime(response.getIat() * 1000L);

        if(response.getExp() != null){ //expiry
            long validityMillis = Math.max(0, response.getExp()*1000L - System.currentTimeMillis());
            tokenInfo.setValidityPeriod(validityMillis);
            tokenInfo.addParameter("exp", response.getExp()*1000L); //keeping it in parameters optionally
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
        AsgardeoScopeCreateRequest req = new AsgardeoScopeCreateRequest();
        req.setName((scope.getKey()));
        req.setDisplayName(scope.getName());
        req.setDescription(scope.getDescription());

        AsgardeoScopeResponse created = apiResourceScopesClient.createScope(globalApiResourceId, Collections.singletonList(req));
        if (created != null && created.getId() != null) {
            scopeNameToIdMap.put(created.getName(), created.getId());
        }
    }

    @Override
    public Scope getScopeByName(String name) throws APIManagementException {

        return getAllScopes().get(name);
    }

    @Override
    public Map<String, Scope> getAllScopes() throws APIManagementException {
        Map<String, Scope> map = new HashMap<>();

        List<AsgardeoScopeResponse> scopes =  apiResourceScopesClient.listScopes(globalApiResourceId);

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
        //delete old local scopes from Asgardeo (optional: only those prefixed as local)
        for (String oldScope : oldLocalScopeKeys) {
            deleteScope((oldScope));
        }

        List<AsgardeoScopeResponse> fetchedScopes = apiResourceScopesClient.listScopes(globalApiResourceId);

        ArrayList<AsgardeoScopeCreateRequest> scopesToBeUpdated = new ArrayList<>(newLocalScopes.size() + fetchedScopes.size());

        //create or update new scopes
        for (Scope scope : newLocalScopes) {
            AsgardeoScopeCreateRequest req = new AsgardeoScopeCreateRequest();
            req.setName((scope.getKey()));
            req.setDisplayName(scope.getName());
            req.setDescription(scope.getDescription());

            scopesToBeUpdated.add(req);
        }

        for(AsgardeoScopeResponse scope : fetchedScopes) {
            AsgardeoScopeCreateRequest req = new AsgardeoScopeCreateRequest();
            req.setName(scope.getName());
            req.setDisplayName(scope.getDisplayName());
            req.setDescription(scope.getDescription());

            scopesToBeUpdated.add(req);
        }

        apiResourceScopesClient.createScope(globalApiResourceId, scopesToBeUpdated);
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

        apiResourceScopesClient.deleteScope(globalApiResourceId, scopeName);
        scopeNameToIdMap.remove(scopeName);
    }

    @Override
    public void updateScope(Scope scope) throws APIManagementException {
//        String scopeId = scopeNameToIdMap.get(scope.getKey());
//        if (scopeId == null) {
//            // refresh cache
//            getAllScopes();
//            scopeId = scopeNameToIdMap.get(scope.getKey());
//        }
//        if (scopeId == null) {
//            throw new APIManagementException("Scope not found in Asgardeo: " + scope.getKey());
//        }
//
//        AsgardeoScopeUpdateRequest req = new AsgardeoScopeUpdateRequest();
//        req.setDisplayName(scope.getName());
//        req.setDescription(scope.getDescription());
//
//        apiResourceScopesClient.updateScope(globalApiResourceId, scopeId, req);
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

    private String doesAPIResourceExistAndGetId() throws APIManagementException{
        int limit = 100;

        AsgardeoAPIResourceListResponse page = apiResourceClient.listAPIResources(limit, AsgardeoConstants.GLOBAL_API_RESOURCE_NAME);
        if(page.getApiResources() != null)
            for(AsgardeoAPIResourceResponse r : page.getApiResources())
                if(AsgardeoConstants.GLOBAL_API_RESOURCE_NAME.equals(r.getName()))
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
}
