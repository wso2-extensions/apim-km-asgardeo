package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;


/**
 * Represents an individual scope update payload Asgardeo.
 */
public class AsgardeoScopeUpdateRequest {
    @SerializedName("displayName")
    private String displayName;

    @SerializedName("description")
    private String description;

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
}
