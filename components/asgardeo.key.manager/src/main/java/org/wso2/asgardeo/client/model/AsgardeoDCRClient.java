package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;

public interface AsgardeoDCRClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo create(AsgardeoDCRClientInfo body) throws KeyManagerClientException;

    @RequestLine("GET /{clientId}")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo get(@Param("clientId") String clientId) throws KeyManagerClientException;

    @RequestLine("PUT /{clientId}")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo update(@Param("clientId") String clientId) throws KeyManagerClientException;

    @RequestLine("DELETE /{clientId}")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo delete(@Param("clientId") String clientId) throws KeyManagerClientException;
}
