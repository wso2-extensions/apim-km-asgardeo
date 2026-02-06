package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface AsgardeoAPIResourceClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    AsgardeoAPIResourceResponse createAPIResource(AsgardeoAPIResourceCreateRequest body);

    @RequestLine("GET ?limit={limit}&filter=name+eq+{name}")
    AsgardeoAPIResourceListResponse listAPIResources(@Param("limit") int limit, @Param("name") String filterName);

    @RequestLine(("PATCH /{apiResourceId}"))
    @Headers("Content-Type: application/json")
    void addScopes(@Param("apiResourceId") String apiResourceId, AsgardeoScopePatchRequest body);
}
