package org.wso2.asgardeo.client.model;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Represents the Asgardeo Token call client.
 */
public interface AsgardeoTokenClient {
    @RequestLine("POST")
    @Headers({"Content-Type: application/x-www-form-urlencoded", "Authorization: Basic {base64encodedString}"})
    AsgardeoAccessTokenResponse getAccessToken(@Param("grant_type") String grantType,
                                                      @Param("scope") String scope,
                                                      @Param("base64encodedString") String base64encodedString);
}
