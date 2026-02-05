package org.wso2.asgardeo.client.model;

import com.google.gson.JsonObject;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;

public interface AsgardeoSCIMRolesClient {
    @RequestLine("GET /{roleId}")
    @Headers("Content-Type: application/json")
    AsgardeoRoleInfo getRole(@Param("roleId") String roleId) throws KeyManagerClientException;

    @RequestLine("POST ")
    @Headers("Content-Type: application/json")
    AsgardeoRoleInfo createRole(AsgardeoRoleInfo role) throws KeyManagerClientException;

    @RequestLine("POST /.search")
    @Headers("Content-Type: application/json")
    JsonObject searchRoles(JsonObject payload) throws KeyManagerClientException;

    @RequestLine("PATCH /{roleId}")
    @Headers("Content-Type: application/json")
    void patchRole(@Param("roleId") String roleId, AsgardeoPatchRoleOperationInfo patchRoleOperationInfo)
            throws KeyManagerClientException;
}
