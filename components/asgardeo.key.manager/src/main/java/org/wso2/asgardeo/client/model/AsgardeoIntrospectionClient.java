package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;

public interface AsgardeoIntrospectionClient {
    @RequestLine("POST")
    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Authorization: Basic {basic}"
    })
    AsgardeoIntrospectionResponse introspect(@Param("token") String token,
                                             @Param("basic") String basic)
            throws KeyManagerClientException, feign.FeignException;
}
