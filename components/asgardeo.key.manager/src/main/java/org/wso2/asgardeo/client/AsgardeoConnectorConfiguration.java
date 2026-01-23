/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.asgardeo.client;

import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.model.ConfigurationDto;
import org.wso2.carbon.apimgt.api.model.KeyManagerConnectorConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component(
        name = "custom.configuration.component",
        immediate = true,
        service = KeyManagerConnectorConfiguration.class
)
public class AsgardeoConnectorConfiguration implements KeyManagerConnectorConfiguration {

    @Override
    public String getImplementation() {

        return AsgardeoOAuthClient.class.getName();
    }

    @Override
    public String getJWTValidator() {

        // If you need to implement a custom JWT validation logic you need to implement
        // org.wso2.carbon.apimgt.impl.jwt.JWTValidator interface and instantiate it in here.
        return null;
    }

    /*
     *  Provides list of Configurations that need to show in Admin portal in order to connect with KeyManager
     *
     * */
    @Override
    public List<ConfigurationDto> getConnectionConfigurations() {

        List<ConfigurationDto> configurationDtoList = new ArrayList<ConfigurationDto>();

        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.ORG_NAME, "Organization Name", "input",
                "Name of the Organization with ", "", true, false,
                Collections.emptyList(), false));


        //COME BACK to change tooltip
        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.MGMT_CLIENT_ID, "Client ID", "input",
                "Client ID of Application with Approved API", "", true, false,
                Collections.emptyList(), false));

        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.MGMT_CLIENT_SECRET, "Client Secret", "input",
                "Client Secret of Application with Approved API", "", true, true,
                Collections.emptyList(), false));

        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.ACCESS_TOKEN_TYPE, "Prefer JWT Access Tokens", "checkbox",
                "Choose to use JWT instead of Opaque as the Access Token Type", "Enable", false, false, Collections.singletonList("Enable"), false));

        return configurationDtoList;
    }

    /*
     *   Provides list of configurations need to create Oauth applications in Oauth server in Dev portal
     *
     * */
    @Override
    public List<ConfigurationDto> getApplicationConfigurations() {

        List<ConfigurationDto> configurationDtoList = new ArrayList<ConfigurationDto>();

       // todo add application configuration parameters that need create an OAuth application in the OAuth Server

        return configurationDtoList;
    }

    @Override
    public String getType() {

        return AsgardeoConstants.ASGARDEO_TYPE;
    }

    @Override
    public String getDisplayName() {

        return AsgardeoConstants.DISPLAY_NAME;
    }

    public String getDefaultScopesClaim() {
        return AsgardeoConstants.DEFAULT_SCOPES_CLAIM;
    }

    public String getDefaultConsumerKeyClaim() {
        return AsgardeoConstants.DEFAULT_CONSUMER_KEY_CLAIM;
    }
}
