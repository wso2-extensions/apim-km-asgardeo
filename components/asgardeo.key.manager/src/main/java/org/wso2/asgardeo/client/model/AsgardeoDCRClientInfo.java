package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AsgardeoDCRClientInfo {

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

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public List<String> getGrantTypes() { return grantTypes; }
    public void setGrantTypes(List<String> grantTypes) { this.grantTypes = grantTypes; }

    public List<String> getRedirectUris() { return redirectUris; }
    public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
}
