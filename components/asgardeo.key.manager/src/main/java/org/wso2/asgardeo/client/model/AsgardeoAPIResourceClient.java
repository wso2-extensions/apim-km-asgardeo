package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;


/**
 * Represents the Asgardeo API resource API call client.
 */
public interface AsgardeoAPIResourceClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    AsgardeoAPIResourceResponse createAPIResource(AsgardeoAPIResourceCreateRequest body)
            throws feign.FeignException;

    @RequestLine("GET ?limit={limit}&filter=name+eq+{name}")
    AsgardeoAPIResourceListResponse listAPIResources(@Param("limit") int limit, @Param("name") String filterName)
            throws feign.FeignException;

    @RequestLine(("PATCH /{apiResourceId}"))
    @Headers("Content-Type: application/json")
    void addScopes(@Param("apiResourceId") String apiResourceId, AsgardeoScopePatchRequest body)
            throws feign.FeignException;
}
