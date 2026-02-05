package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;

import java.util.List;

public interface AsgardeoAPIResourceScopesClient {

    @RequestLine("GET /{apiResourceId}/scopes")
    @Headers("Content-Type: application/json")
    List<AsgardeoScopeResponse> listScopes(@Param("apiResourceId") String apiResourceId);

    @RequestLine("PUT /{apiResourceId}/scopes")
    @Headers("Content-Type: application/json")
    AsgardeoScopeResponse createScope(@Param("apiResourceId") String apiResourceId,
                                      List<AsgardeoScopeCreateRequest> body);

    @RequestLine("PATCH /{apiResourceId}/scopes/{scopeName}")
    @Headers("Content-Type: application/json")
    AsgardeoScopeResponse updateScope(@Param("apiResourceId") String apiResourceId,
                                      @Param("scopeName") String scopeName,
                                      AsgardeoScopeUpdateRequest body) throws KeyManagerClientException;

    @RequestLine("DELETE /{apiResourceId}/scopes/{scopeName}")
    void deleteScope(@Param("apiResourceId") String apiResourceId,
                     @Param("scopeName") String scopeName) throws KeyManagerClientException;
}

