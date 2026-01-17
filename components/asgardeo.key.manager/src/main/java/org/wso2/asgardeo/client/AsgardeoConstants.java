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

    public static final String DEFAULT_SCOPES_CLAIM = "scope";
    public static final String DEFAULT_CONSUMER_KEY_CLAIM = "azp";

    public static final String ERROR_ENCODING_METHOD_NOT_SUPPORTED = "Encoding method is not supported";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    AsgardeoConstants() {
    }
}
