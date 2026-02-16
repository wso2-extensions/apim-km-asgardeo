package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents create API resource payload that creates the global API resource on Asgardeo.
 */
public class AsgardeoAPIResourceCreateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("identifier")
    private String identifier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
