package org.wso2.asgardeo.client.model;

import feign.FeignException;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.wso2.carbon.apimgt.api.APIManagementException;

public interface AsgardeoAPIResourceClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    AsgardeoAPIResourceResponse createAPIResource(AsgardeoAPIResourceCreateRequest body);

    @RequestLine("GET ?limit={limit}&filter=name+eq+{name}")
    AsgardeoAPIResourceListResponse listAPIResources(@Param("limit") int limit, @Param("name") String filterName);

}
