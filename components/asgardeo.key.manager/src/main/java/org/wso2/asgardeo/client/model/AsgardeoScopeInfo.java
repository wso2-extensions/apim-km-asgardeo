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


import com.google.gson.annotations.SerializedName;
import org.wso2.carbon.apimgt.api.model.Scope;

/**
 * Represents Scopes info payload body sent to Asgardeo.
 */
public class AsgardeoScopeInfo {
    @SerializedName("name")
    private String name;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("description")
    private String description;

    public AsgardeoScopeInfo() {
    }

    public AsgardeoScopeInfo(Scope scope) {
        name = scope.getKey();
        displayName = scope.getName();
        description = scope.getDescription();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
