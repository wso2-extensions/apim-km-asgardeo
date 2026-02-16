package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.RequestLine;

public interface AsgardeoDCRClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    AsgardeoDCRClientInfo create(AsgardeoDCRClientInfo body);
}
