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
import org.wso2.carbon.apimgt.impl.APIConstants;

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
        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.APPLICATION_MANAGEMENT_ENDPOINT,
                "Asgardeo Application Management Endpoint", "input",
                String.format("E.g., %s/api/server/v1/applications",
                        AsgardeoConstants.BASE_URL_FORMAT), "", true, false,
                Collections.emptyList(), false));
        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.RESOURCE_MANAGEMENT_ENDPOINT,
                "Asgardeo API Resource Management Endpoint", "input",
                String.format("E.g., %s/api/server/v1/api-resources",
                        AsgardeoConstants.BASE_URL_FORMAT), "", true, false,
                Collections.emptyList(), false));
        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.ROLES_MANAGEMENT_ENDPOINT,
                "Asgardeo Roles Endpoint", "input",
                String.format("E.g., %s/scim2/v2/Roles",
                        AsgardeoConstants.BASE_URL_FORMAT), "", true, false,
                Collections.emptyList(), false));
        configurationDtoList.add(new ConfigurationDto(AsgardeoConstants.ENABLE_ROLE_CREATION,
                "Create roles in Asgardeo", "checkbox",
                "Create roles in Asgardeo, corresponding to the roles used in WSO2 API Manager.",
                "Enable", false, false, Collections.singletonList("Enable"), false));

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

        List<ConfigurationDto> applicationConfigurationsList = new ArrayList();
        applicationConfigurationsList
                .add(new ConfigurationDto(AsgardeoConstants.APPLICATION_TOKEN_LIFETIME,
                        "Lifetime of the Application Token ", "input", "Type Lifetime of the Application Token " +
                        "in seconds ", APIConstants.KeyManager.NOT_APPLICABLE_VALUE, false, false,
                        Collections.EMPTY_LIST, false));
        applicationConfigurationsList
                .add(new ConfigurationDto(AsgardeoConstants.USER_TOKEN_LIFETIME,
                        "Lifetime of the User Token ", "input", "Type Lifetime of the User Token " +
                        "in seconds ", APIConstants.KeyManager.NOT_APPLICABLE_VALUE, false, false,
                        Collections.EMPTY_LIST, false));
        applicationConfigurationsList
                .add(new ConfigurationDto(AsgardeoConstants.REFRESH_TOKEN_LIFETIME,
                        "Lifetime of the Refresh Token ", "input", "Type Lifetime of the Refresh Token " +
                        "in seconds ", APIConstants.KeyManager.NOT_APPLICABLE_VALUE, false, false,
                        Collections.EMPTY_LIST, false));
        applicationConfigurationsList
                .add(new ConfigurationDto(AsgardeoConstants.ID_TOKEN_LIFETIME,
                        "Lifetime of the ID Token", "input", "Type Lifetime of the ID Token " +
                        "in seconds ", APIConstants.KeyManager.NOT_APPLICABLE_VALUE, false, false,
                        Collections.EMPTY_LIST, false));

        ConfigurationDto configurationDtoPkceMandatory = new ConfigurationDto(AsgardeoConstants.PKCE_MANDATORY,
                "Enable PKCE", "checkbox", "Enable PKCE", String.valueOf(false), false, false,
                Collections.EMPTY_LIST, false);
        applicationConfigurationsList.add(configurationDtoPkceMandatory);

        ConfigurationDto configurationDtoPkcePlainText =
                new ConfigurationDto(AsgardeoConstants.PKCE_SUPPORT_PLAIN,
                        "Support PKCE Plain text", "checkbox", "S256 is recommended, plain text too can be used.",
                        String.valueOf(false), false, false, Collections.EMPTY_LIST, false);
        applicationConfigurationsList.add(configurationDtoPkcePlainText);

        ConfigurationDto configurationDtoBypassClientCredentials =
                new ConfigurationDto(AsgardeoConstants.PUBLIC_CLIENT,
                        "Public client", "checkbox", "Allow authentication without the client secret.",
                        String.valueOf(false), false, false, Collections.EMPTY_LIST, false);
        applicationConfigurationsList.add(configurationDtoBypassClientCredentials);


        return applicationConfigurationsList;
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
