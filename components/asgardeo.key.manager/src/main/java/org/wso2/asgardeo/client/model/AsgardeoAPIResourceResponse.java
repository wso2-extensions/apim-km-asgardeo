package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents body of API Resource response from Asgardeo.
 */
public class AsgardeoAPIResourceResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("identifier")
    private String identifier;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
