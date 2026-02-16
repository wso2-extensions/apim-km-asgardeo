package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Represents the Asgardeo Dynamic Client Registration API call client.
 */
public interface AsgardeoDCRClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo create(AsgardeoDCRClientInfo body) throws feign.FeignException;

    @RequestLine("GET /{clientId}")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo get(@Param("clientId") String clientId) throws feign.FeignException;

    @RequestLine("PUT /{clientId}")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo update(@Param("clientId") String clientId, AsgardeoDCRClientInfo body)
            throws feign.FeignException;

    @RequestLine("DELETE /{clientId}")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo delete(@Param("clientId") String clientId) throws feign.FeignException;
}
