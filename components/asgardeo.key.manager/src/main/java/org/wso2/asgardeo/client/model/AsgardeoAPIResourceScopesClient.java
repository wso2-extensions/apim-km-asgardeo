package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface AsgardeoAPIResourceScopesClient {

    @RequestLine("GET /api-resources/{apiResourceId}/scopes")
    @Headers("Content-Type: application/json")
    List<AsgardeoScopeResponse> listScopes(@Param("apiResourceId") String apiResourceId);

    @RequestLine("PUT /api-resources/{apiResourceId}/scopes")
    @Headers("Content-Type: application/json")
    AsgardeoScopeResponse createScope(@Param("apiResourceId") String apiResourceId,
                                      List<AsgardeoScopeCreateRequest> body);

    @RequestLine("PUT /api-resources/{apiResourceId}/scopes/{scopeId}")
    @Headers("Content-Type: application/json")
    AsgardeoScopeResponse updateScope(@Param("apiResourceId") String apiResourceId,
                                      @Param("scopeId") String scopeId,
                                      AsgardeoScopeUpdateRequest body);

    @RequestLine("DELETE /api-resources/{apiResourceId}/scopes/{scopeName}")
    void deleteScope(@Param("apiResourceId") String apiResourceId,
                     @Param("scopeName") String scopeName);
}

