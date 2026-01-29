package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AsgardeoScopeUpdateRequest {
    @SerializedName("displayName")
    private String displayName;

    @SerializedName("description")
    private String description;

    @SerializedName("bindings")
    private List<String> bindings;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getBindings() {
        return bindings;
    }

    public void setBindings(List<String> bindings) {
        this.bindings = bindings;
    }
}
