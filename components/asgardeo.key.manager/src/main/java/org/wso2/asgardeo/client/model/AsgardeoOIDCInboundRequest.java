package org.wso2.asgardeo.client.model;

import java.util.List;

public class AsgardeoOIDCInboundRequest {
    private String clientId;
    private String clientSecret;
    private java.util.List<String> grantTypes;
    private java.util.List<String> callbackURLs;
    private java.util.List<String> allowedOrigins;
    private AccessToken accessToken;

    public static class AccessToken {
        private String type;
        public AccessToken(String type) { this.type = type; }
        public AccessToken() {}

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
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

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public List<String> getCallbackURLs() {
        return callbackURLs;
    }

    public void setCallbackURLs(List<String> callbackURLs) {
        this.callbackURLs = callbackURLs;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }
}
