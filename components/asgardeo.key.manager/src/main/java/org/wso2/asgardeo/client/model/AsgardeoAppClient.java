package org.wso2.asgardeo.client.model;

import feign.FeignException;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AsgardeoAppClient {
    @RequestLine("GET limit={limit}&attributes={attributes}&offset={offset}")
    @Headers("Accept: application/json")
    AsgardeoApplicationsResponse list(
            @Param("limit") int limit,
            @Param("attributes") String attributes,
            @Param("offset") int offset
    );

    @RequestLine("POST /{applicationId}/authorized-apis")
    @Headers("Content-Type: application/json")
    void authorizeAPItoApp(@Param("applicationId") String applicationId, AsgardeoAPIAuthRequest body) throws FeignException;
}
