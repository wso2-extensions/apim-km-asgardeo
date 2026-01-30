package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AsgardeoScopeListResponse {

    @SerializedName("scopes")
    private List<AsgardeoScopeResponse> scopes;

    public List<AsgardeoScopeResponse> getScopes() {
        return scopes;
    }

    public void setScopes(List<AsgardeoScopeResponse> scopes) {
        this.scopes = scopes;
    }
}
