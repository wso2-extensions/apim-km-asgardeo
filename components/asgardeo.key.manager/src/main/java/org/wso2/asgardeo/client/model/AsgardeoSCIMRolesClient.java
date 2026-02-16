package org.wso2.asgardeo.client.model;

import com.google.gson.JsonObject;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Represents the Asgardeo SCIM Roles API call client.
 */
public interface AsgardeoSCIMRolesClient {
    @RequestLine("GET /{roleId}")
    @Headers("Content-Type: application/json")
    AsgardeoRoleInfo getRole(@Param("roleId") String roleId) throws feign.FeignException;

    @RequestLine("POST ")
    @Headers("Content-Type: application/json")
    AsgardeoRoleInfo createRole(AsgardeoRoleInfo role) throws feign.FeignException;

    @RequestLine("POST /.search")
    @Headers("Content-Type: application/json")
    JsonObject searchRoles(JsonObject payload) throws feign.FeignException;

    @RequestLine("PATCH /{roleId}")
    @Headers("Content-Type: application/json")
    void patchRole(@Param("roleId") String roleId, AsgardeoPatchRoleOperationInfo patchRoleOperationInfo)
            throws feign.FeignException;
}
