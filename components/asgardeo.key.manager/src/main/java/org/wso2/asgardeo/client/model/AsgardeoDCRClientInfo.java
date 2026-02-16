package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents the DCR client information returned from Asgardeo.
 */
public class AsgardeoDCRClientInfo {

    public AsgardeoDCRClientInfo() {}

    @SerializedName("id")
    private String id;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("client_secret")
    private String clientSecret;

    @SerializedName("client_name")
    private String clientName;

    @SerializedName("grant_types")
    private List<String> grantTypes;

    @SerializedName("redirect_uris")
    private List<String> redirectUris;

    @SerializedName("token_type_extension")
    private String tokenTypeExtension;

    @SerializedName("ext_application_token_lifetime")
    private Long applicationTokenLifetime;

    @SerializedName("ext_user_token_lifetime")
    private Long userTokenLifetime;

    @SerializedName("ext_refresh_token_lifetime")
    private long refreshTokenLifetime;

    @SerializedName("ext_id_token_lifetime")
    private long idTokenLifetime;

    @SerializedName("ext_pkce_mandatory")
    private boolean pkceMandatory;

    @SerializedName("ext_pkce_support_plain")
    private boolean pkcePlainText;

    @SerializedName("ext_public_client")
    private boolean publicClient;

    //set to "JWT" for JWT access token type

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTokenTypeAsJWT() {
        tokenTypeExtension = "JWT";
    }

    public Long getApplicationTokenLifetime() {
        return applicationTokenLifetime;
    }

    public void setApplicationTokenLifetime(Long applicationTokenLifetime) {
        this.applicationTokenLifetime = applicationTokenLifetime;
    }

    public Long getUserTokenLifetime() {
        return userTokenLifetime;
    }

    public void setUserTokenLifetime(Long userTokenLifetime) {
        this.userTokenLifetime = userTokenLifetime;
    }

    public long getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    public void setRefreshTokenLifetime(long refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    public long getIdTokenLifetime() {
        return idTokenLifetime;
    }

    public void setIdTokenLifetime(long idTokenLifetime) {
        this.idTokenLifetime = idTokenLifetime;
    }

    public void setPkceMandatory(boolean pkceMandatory) {
        this.pkceMandatory = pkceMandatory;
    }

    public void setPkceSupportPlain(boolean pkceSupportPlain) {
        this.pkcePlainText = pkceSupportPlain;
    }

    public String getTokenTypeExtension() {
        return tokenTypeExtension;
    }

    public boolean isPkceMandatory() {
        return pkceMandatory;
    }

    public boolean isPkcePlainText() {
        return pkcePlainText;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }
}
