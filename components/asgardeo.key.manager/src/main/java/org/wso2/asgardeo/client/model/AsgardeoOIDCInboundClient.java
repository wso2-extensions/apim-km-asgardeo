package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.RequestLine;

public interface AsgardeoOIDCInboundClient {
    @RequestLine("PUT /applications/{id}/inbound-protocols/oidc")
    @Headers("Content-Type: application/json")
    void updateOidcInbound(@Param("id") String appId, AsgardeoOIDCInboundRequest body);
}
