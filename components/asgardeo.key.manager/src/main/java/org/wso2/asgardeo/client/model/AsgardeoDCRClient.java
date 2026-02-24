/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
