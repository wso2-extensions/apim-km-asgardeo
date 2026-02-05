package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class AsgardeoRoleInfo {
    //taken from WSO2IS7RoleInfo
    @SerializedName("id")
    private String id;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("schemas")
    private List<String> schemas;

    @SerializedName("permissions")
    private List<Map<String, String>> permissions;

    @SerializedName("audience")
    private Map<String, String> audience;

    @SerializedName("meta")
    private Map<String, String> meta;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    public List<Map<String, String>> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Map<String, String>> permissions) {
        this.permissions = permissions;
    }

    public Map<String, String> getAudience() {
        return audience;
    }

    public void setAudience(Map<String, String> audience) {
        this.audience = audience;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }
}
