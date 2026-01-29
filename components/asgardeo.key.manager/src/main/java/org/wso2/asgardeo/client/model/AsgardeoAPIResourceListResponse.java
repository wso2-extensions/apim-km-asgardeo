package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AsgardeoAPIResourceListResponse {
    @SerializedName("totalResults")
    private int totalResults;

    @SerializedName("apiResources")
    private List<AsgardeoAPIResourceResponse> apiResources;

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public List<AsgardeoAPIResourceResponse> getApiResources() {
        return apiResources;
    }

    public void setApiResources(List<AsgardeoAPIResourceResponse> apiResources) {
        this.apiResources = apiResources;
    }
}
