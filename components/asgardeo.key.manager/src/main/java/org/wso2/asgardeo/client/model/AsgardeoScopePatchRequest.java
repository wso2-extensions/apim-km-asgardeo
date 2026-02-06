package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;
import org.wso2.carbon.apimgt.api.model.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AsgardeoScopePatchRequest {
    @SerializedName("addedScopes")
    private List<AsgardeoScopeInfo> addedScopes;

    public AsgardeoScopePatchRequest(int size){
        addedScopes = new ArrayList<>(size);
    }

    public AsgardeoScopePatchRequest(List<AsgardeoScopeInfo> scopesToAdd){
        addedScopes = scopesToAdd;
    }

    public AsgardeoScopePatchRequest(Scope scopeToAdd){
        addedScopes = new ArrayList<>(1);
        addedScopes.add(new AsgardeoScopeInfo(scopeToAdd));
    }

    public AsgardeoScopePatchRequest(Set<Scope> scopesToAdd){
        addedScopes = new ArrayList<>(scopesToAdd.size());

        for(Scope scope: scopesToAdd){
            addedScopes.add(new AsgardeoScopeInfo(scope));
        }
    }

    public AsgardeoScopePatchRequest(){
        addedScopes = new ArrayList<>();
    }
}
