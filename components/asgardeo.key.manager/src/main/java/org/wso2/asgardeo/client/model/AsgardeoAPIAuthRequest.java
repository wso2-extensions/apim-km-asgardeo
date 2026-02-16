package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the payload sent to authorize global api resource to OAuth app on Asgardeo.
 */
public class AsgardeoAPIAuthRequest {

    @SerializedName("id")
    private String id;

    public AsgardeoAPIAuthRequest(String id) {
        this.id = id;
    }

    public AsgardeoAPIAuthRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
