/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.asgardeo.client.model;

import com.google.gson.JsonObject;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.apache.http.HttpHeaders;

/**
 * Represents the Asgardeo SCIM Roles API call client.
 */
public interface AsgardeoSCIMRolesClient {

    @RequestLine("GET /{roleId}")
    @Headers(HttpHeaders.CONTENT_TYPE + ": application/json")
    AsgardeoRoleInfo getRole(@Param("roleId") String roleId) throws feign.FeignException;

    @RequestLine("POST ")
    @Headers(HttpHeaders.CONTENT_TYPE + ": application/json")
    AsgardeoRoleInfo createRole(AsgardeoRoleInfo role) throws feign.FeignException;

    @RequestLine("POST /.search")
    @Headers(HttpHeaders.CONTENT_TYPE + ": application/json")
    JsonObject searchRoles(JsonObject payload) throws feign.FeignException;

    @RequestLine("PATCH /{roleId}")
    @Headers(HttpHeaders.CONTENT_TYPE + ": application/json")
    void patchRole(@Param("roleId") String roleId, AsgardeoPatchRoleOperationInfo patchRoleOperationInfo)
            throws feign.FeignException;
}
