package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AsgardeoScopeResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name; // scope key

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("description")
    private String description;

    @SerializedName("bindings")
    private List<String> bindings; //roles

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
