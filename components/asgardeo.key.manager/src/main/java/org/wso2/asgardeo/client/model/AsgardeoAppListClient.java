package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AsgardeoAppListClient {
    @RequestLine("GET /applications?limit={limit}&attributes={attributes}&offset={offset}")
    @Headers("Accept: application/json")
    AsgardeoApplicationsResponse list(
            @Param("limit") int limit,
            @Param("attributes") String attributes,
            @Param("offset") int offset
    );
}
