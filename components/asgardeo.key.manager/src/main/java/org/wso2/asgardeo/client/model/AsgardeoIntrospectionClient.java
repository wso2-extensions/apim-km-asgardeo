package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Represents the Asgardeo Introspection Client.
 */
public interface AsgardeoIntrospectionClient {
    @RequestLine("POST")
    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Authorization: Basic {basic}"
    })
    AsgardeoIntrospectionResponse introspect(@Param("token") String token,
                                             @Param("basic") String basic)
            throws feign.FeignException;
}
