package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the introspection response returned from Asgardeo.
 */
public class AsgardeoIntrospectionResponse {

    @SerializedName("active")
    private boolean active;

    @SerializedName("scope")
    private String scope;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("aut")
    private String aut; // APPLICATION

    @SerializedName("exp")
    private Long exp;

    @SerializedName("iat")
    private Long iat;

    @SerializedName("nbf")
    private Long nbf;

    @SerializedName("iss")
    private String iss;

    @SerializedName("sub")
    private String sub;

    @SerializedName("aud")
    private String aud;

    @SerializedName("jti")
    private String jti;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAut() {
        return aut;
    }

    public void setAut(String aut) {
        this.aut = aut;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public Long getNbf() {
        return nbf;
    }

    public void setNbf(Long nbf) {
        this.nbf = nbf;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }
}

