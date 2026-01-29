/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.asgardeo.client;

/**
 * This class will hold constants related to Asgardeo key manager implementation.
 */
public class AsgardeoConstants {
    public static final String UTF_8 = "UTF-8";
    public static final String ASGARDEO_TYPE = "Asgardeo";
    public static final String DISPLAY_NAME = "Asgardeo";

    public static final String ORG_NAME = "org_name";
    public static final String MGMT_CLIENT_ID = "mgmt_client_id";
    public static final String MGMT_CLIENT_SECRET = "mgmt_client_secret";
    public static final String ACCESS_TOKEN_TYPE = "access_token_type"; //for JWT or opaque (default) token selection
    public static final String JWT = "JWT";

    public static final String DEFAULT_SCOPES_CLAIM = "scope";
    public static final String DEFAULT_CONSUMER_KEY_CLAIM = "azp";

    public static final String ERROR_ENCODING_METHOD_NOT_SUPPORTED = "Encoding method is not supported";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    public static final String DCR_SCOPES = "internal_dcr_create internal_dcr_view internal_dcr_update internal_dcr_delete " +
            "internal_application_mgt_view internal_application_mgt_update";
    public static final String TOKEN_APPLICATION = "APPLICATION";
    public static final String TOKEN_STATE_INTROSPECTION_FAILED = "INTROSPECTION_FAILED";
    public static final String TOKEN_STATE_ACTIVE = "ACTIVE";
    public static final String TOKEN_STATE_INACTIVE = "INACTIVE";

    public static final String GLOBAL_API_RESOURCE_NAME = "APIM_GLOBAL_SCOPES";
    public static final String GLOBAL_API_RESOURCE_DISPLAY_NAME = "APIM Global Scopes";
    public static final String GLOBAL_API_RESOURCE_IDENTIFIER = "apim-global-scopes";

    AsgardeoConstants() {
    }
}
