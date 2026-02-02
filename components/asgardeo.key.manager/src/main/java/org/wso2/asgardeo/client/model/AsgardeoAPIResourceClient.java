package org.wso2.asgardeo.client.model;

import feign.FeignException;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.wso2.carbon.apimgt.api.APIManagementException;

public interface AsgardeoAPIResourceClient {
    @RequestLine("POST /api-resources")
    @Headers("Content-Type: application/json")
    AsgardeoAPIResourceResponse createAPIResource(AsgardeoAPIResourceCreateRequest body);

    @RequestLine("GET /api-resources?limit={limit}&filter=name+eq+{name}")
    AsgardeoAPIResourceListResponse listAPIResources(@Param("limit") int limit, @Param("name") String filterName);

    @RequestLine("POST /applications/{applicationId}/authorized-apis")
    @Headers("Content-Type: application/json")
    void authorizeAPItoApp(@Param("applicationId") String applicationId, AsgardeoAPIAuthRequest body) throws FeignException;
}
