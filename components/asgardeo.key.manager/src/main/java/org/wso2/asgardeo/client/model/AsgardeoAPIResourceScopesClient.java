package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

/**
 * Represents the Asgardeo API Resource Scopes API call client.
 */
public interface AsgardeoAPIResourceScopesClient {

    @RequestLine("GET /{apiResourceId}/scopes")
    @Headers("Content-Type: application/json")
    List<AsgardeoScopeResponse> listScopes(@Param("apiResourceId") String apiResourceId) throws feign.FeignException;

    @RequestLine("PATCH /{apiResourceId}/scopes/{scopeName}")
    @Headers("Content-Type: application/json")
    void updateScope(@Param("apiResourceId") String apiResourceId,
                                      @Param("scopeName") String scopeName,
                                      AsgardeoScopeUpdateRequest body) throws feign.FeignException;

    @RequestLine("DELETE /{apiResourceId}/scopes/{scopeName}")
    void deleteScope(@Param("apiResourceId") String apiResourceId,
                     @Param("scopeName") String scopeName) throws feign.FeignException;
}

