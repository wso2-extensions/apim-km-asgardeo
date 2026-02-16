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

        // todo add connection parameters that need to connect to the Custom KeymManager here

        return configurationDtoList;
    }

    /*
     *   Provides list of configurations need to create Oauth applications in Oauth server in Devportal
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

        return AsgardeoConstants.CUSTOM_TYPE;
    }

    @Override
    public String getDisplayName() {

        return AsgardeoConstants.DISPLAY_NAME;
    }
}
