package org.wso2.asgardeo.client.model;


import com.google.gson.annotations.SerializedName;
import org.wso2.carbon.apimgt.api.model.Scope;

/**
 * Represents Scopes info payload body sent to Asgardeo.
 */
public class AsgardeoScopeInfo {
    @SerializedName("name")
    private String name;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("description")
    private String description;

    public AsgardeoScopeInfo() {
    }

    public AsgardeoScopeInfo(Scope scope) {
        name = scope.getKey();
        displayName = scope.getName();
        description = scope.getDescription();
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
}
