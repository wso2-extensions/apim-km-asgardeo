package org.wso2.asgardeo.client.model;

import java.util.List;

public class AsgardeoOIDCInboundRequest {
    private String clientId;
    private java.util.List<String> grantTypes;
    private java.util.List<String> allowedOrigins;
    private AccessToken accessToken;

    public static class AccessToken {
        private String type;
        private int userAccessTokenExpiryInSeconds;
        private int applicationAccessTokenExpiryInSeconds;

        public AccessToken(String type, int userAccessTokenExpiryInSeconds, int applicationAccessTokenExpiryInSeconds) {
            this.type = type;
            this.userAccessTokenExpiryInSeconds = userAccessTokenExpiryInSeconds;
            this.applicationAccessTokenExpiryInSeconds = applicationAccessTokenExpiryInSeconds;
        }

        public AccessToken() {}


        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getUserAccessTokenExpiryInSeconds() {
            return userAccessTokenExpiryInSeconds;
        }

        public void setUserAccessTokenExpiryInSeconds(int userAccessTokenExpiryInSeconds) {
            this.userAccessTokenExpiryInSeconds = userAccessTokenExpiryInSeconds;
        }

        public int getApplicationAccessTokenExpiryInSeconds() {
            return applicationAccessTokenExpiryInSeconds;
        }

        public void setApplicationAccessTokenExpiryInSeconds(int applicationAccessTokenExpiryInSeconds) {
            this.applicationAccessTokenExpiryInSeconds = applicationAccessTokenExpiryInSeconds;
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

//    public String getClientSecret() {
//        return clientSecret;
//    }
//
//    public void setClientSecret(String clientSecret) {
//        this.clientSecret = clientSecret;
//    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
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
