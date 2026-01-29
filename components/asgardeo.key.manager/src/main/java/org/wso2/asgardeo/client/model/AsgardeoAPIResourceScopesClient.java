package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AsgardeoAPIResourceScopesClient {

    @RequestLine("GET /api-resources/{apiResourceId}/scopes?limit={limit}&offset={offset}")
    AsgardeoScopeListResponse listScopes(@Param("apiResourceId") String apiResourceId,
                                         @Param("limit") int limit,
                                         @Param("offset") int offset);

    @RequestLine("POST /api-resources/{apiResourceId}/scopes")
    @Headers("Content-Type: application/json")
    AsgardeoScopeResponse createScope(@Param("apiResourceId") String apiResourceId,
                                      AsgardeoScopeCreateRequest body);

    @RequestLine("PUT /api-resources/{apiResourceId}/scopes/{scopeId}")
    @Headers("Content-Type: application/json")
    AsgardeoScopeResponse updateScope(@Param("apiResourceId") String apiResourceId,
                                      @Param("scopeId") String scopeId,
                                      AsgardeoScopeUpdateRequest body);

    @RequestLine("DELETE /api-resources/{apiResourceId}/scopes/{scopeId}")
    void deleteScope(@Param("apiResourceId") String apiResourceId,
                     @Param("scopeId") String scopeId);
}

