package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

public class AsgardeoAPIAuthRequest {
    @SerializedName("id")
    private String id;

    public AsgardeoAPIAuthRequest(String id){
        this.id = id;
    }

    public AsgardeoAPIAuthRequest(){}

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

}
