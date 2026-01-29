package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AsgardeoScopeListResponse {
    @SerializedName("totalResults")
    private int totalResults;

    @SerializedName("scopes")
    private List<AsgardeoScopeResponse> scopes;

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public List<AsgardeoScopeResponse> getScopes() {
        return scopes;
    }

    public void setScopes(List<AsgardeoScopeResponse> scopes) {
        this.scopes = scopes;
    }
}
