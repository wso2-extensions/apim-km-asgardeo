package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AsgardeoAPIResourceClient {
    @RequestLine("POST /api-resources")
    @Headers("Content-Type: application/json")
    AsgardeoAPIResourceResponse createAPIResource(AsgardeoAPIResourceCreateRequest body);

    @RequestLine("GET /api-resources?limit={limit}&filter=name+eq+{name}")
    AsgardeoAPIResourceListResponse listAPIResources(@Param("limit") int limit, @Param("name") String filterName);
}
