# Asgardeo Key Manager for WSO2 APIM 4.6
A Key Manager Implementation to Integrate **Asgardeo** with **WSO2 API Manager 4.6**  
  
> <span style="color:#ee3333">⚠ Under active development ⚠</span>  

> <span style="color:#ee3333">⚠ Redirect URLs are hardcoded for now when Generating Keys. Do set them up manually on Asgardeo if you intend to use them with Authorization Code ⚠</span>


[//]: # (___)

[//]: # (## Table of Contents)

[//]: # (- [Overview] &#40;#overview&#41;)

___
## Overview
This project provides a **Key Manager** for **WSO2 APIM 4.6** that delegates Key Manager responsibilities to **[Asgardeo](https://console.asgardeo.io)**  
  
This Key Manager integrates with Asgardeo Management APIs to support:
- Dynamic Client Registration
- OAuth2 token generation
- JWKS based token validation
- Scope based access control

___
## Setting Up  
> ⚠ This project was tested using Apache Maven 3.6.x.  
> Using newer versions may cause dependency resolution issues.
### A. Installing the Key Manager in WSO2 APIM 4.6.
1. Create a local copy of this repository.
2. Run ``mvn clean install`` or `mvn clean install -DskipTests=true` in the root directory to build the Key Manager using Maven.
3. Copy the generated JAR from `<REPOSITORY_ROOT>/components/asgardeo.key.manager/target` to `<APIM_HOME>/repository/components/dropins`
4. Restart APIM 4.6

### B. Creating the Asgardeo Management Application
1. Log into the [Asgardeo Console](https://console.asgardeo.io) and select the **organization** you want to use with APIM.
2. Navigate to **Applications → `+ New Application`**
3. Create a **Standard-Based Application** 

   | Name               | Protocol                   |
   |--------------------|----------------------------|
   | API-Management-App | OAuth 2.0 + OpenID Connect |

4. Navigate to the **API Authorization** tab in the created application.
5. Click **`+ Authorize API resource`** 
6. In the popped up window, select the Management API `OAuth DCR API` as the **API Resource**, `Select All` for **Authorized Scopes**, and click on **Finish**.
    > ⚠ If you don't see the required API Resource, wait a minute or two for the API Resources list to finish loading. Reload the page if the issue doesn't resolve.  

    > ⚠ Ensure you have authorized the **Management API** version of the API Resource. It is a common mistake to authorize the **Organization API** version instead. 
7. Repeat steps 5 and 6 for Management APIs `Application Management API` and `API Resource Management API`. 
8. Note the 
   - **Organization Name**
   - **`Client ID`** and **`Client Secret`**  (found in the **Protocol** tab).

### C. Configuring the Asgardeo Key Manager in WSO2 APIM

1. Log in to the **WSO2 API Manager Admin Portal**: `https://<APIM_HOST>/admin`

2. Navigate to **Key Managers** and click **Add Key Manager**.

3. Under **General Details**, provide the following:
- **Name**: A unique name for the Key Manager (e.g., `AsgardeoKM`)
- **Display Name**: A user-friendly name (e.g., `Asgardeo`)
4. Set the **Key Manager Type** to `Asgardeo`

5. Under **Key Manager Endpoints**, configure the **Well-Known URL**: `
https://api.asgardeo.io/t/<YOUR_ORGANIZATION>/oauth2/token/.well-known/openid-configuration`

6. Click **Import**.
- All endpoint fields will be populated automatically **except** the Scope Management Endpoint.

7. Manually set **Scope Management Endpoint**: `none` (it isn't required to be explicitly set right now)

8. Under **Grant Types**, review the auto-populated list and remove any grant types you do not wish to support.

9. Under **Connector Configurations**, enter the following values noted in section **B**:
   - **Organization Name**
   - **Client ID**
   - **Client Secret**

    >    ⚠ If you want **Asgardeo to issue JWT access tokens** instead of the default Opaque tokens:
    >    - Enable **Prefer JWT Access Tokens**
10. Under **Advanced Configurations**:
- If **Prefer JWT Access Tokens** is **not enabled**, set:
    - **Token Validation Method**: `Use introspect`

11. Click **Add** to save the Key Manager.

---

### D. Verifying the Configuration

1. Log in to the **Developer Portal**: `https://<APIM_HOST>/devportal`
2. Create a new Application.
3. Select **Asgardeo** as the Key Manager.
4. Generate OAuth keys.
5. Generate an Access Token

> Successful generation confirms that the Asgardeo Key Manager is configured correctly.
___
## Requesting and Using Scopes with Asgardeo

This section explains how **OAuth scopes** are created, authorized, and finally requested when using **Asgardeo as the Key Manager** in WSO2 API Manager.

The process involves **three roles**:
- API Publisher (creates scopes)
- Asgardeo Administrator (authorizes scopes)
- Application Developer (requests tokens with scopes)

### Step 1: Create and Assign Scopes in the Publisher Portal
The scopes required in the API must be mirrored in Asgardeo. The following steps must be completed by the **API Publisher**.

1. Log in to the **Publisher Portal**: `https://<APIM_HOST>/publisher`

2. Open the API that requires scope-based access control.

3. Create a new **Scope** (or skip this step and select an existing one)

4. Assign the scope to the required API resource:
- Navigate to the **Resources** section of the API
- Add the scope to the required resource.
- Save the API

> Saving the API triggers APIM to propagate the scope to Asgardeo.  

> The **Resources** section of an **API** is equivalent to the **Tools** section of an **MCP Server**. The steps to create and assign scopes to a tool of an MCP Server are similar to the steps mentioned above.

> ⚠ Saving may take a few moments. This is expected behavior due to the latency of scope management operations in Asgardeo.

> ⚠ Ensure you give the scope a unique name. Asgardeo APIs have their own set of scopes and similar names will cause errors. 

#### What Happens Internally

- APIM creates or updates the scope in **Asgardeo**
- The scope is added under a **common API resource** in Asgardeo
- At this stage, the scope **exists** in Asgardeo but is **not yet usable** by applications


### Step 2: Authorize Scopes in Asgardeo

Scopes must be explicitly authorized by an Asgardeo Admin for the OAuth application that will request them. The following steps must be completed by an **Asgardeo Administrator**. If you do not have admin access, request an admin to do so.

1. Log in to the **[Asgardeo Console](https://console.asgardeo.io)** as an admin.

2. Navigate to **Applications** and open the OAuth application that will request scopes created from the **Developer Portal**.

3. Go to the **API Authorization** tab.

4. Locate the **common API resource** named `APIM_GLOBAL_SCOPES`
- This resource is automatically authorized when the application is created. It is the scopes inside this API that need explicit authorization.

5. Under the authorized API resource:
- Select (allow) the specific scopes that the application should be permitted to request

6. Save the changes.

> ⚠ Only scopes explicitly allowed here can be requested in access token requests.

---

### Step 3: Request Access Tokens with Scopes

Once scopes are authorized, the application developer can request access tokens. These steps can be completed by the **Application Developer**

1. Use the OAuth application credentials to request a token. This can be done from APIM itself or using a cURL command as shown in APIM.
2. Include the required scopes in the request. 
> If the scope is authorized, Asgardeo issues an access token containing the requested scope




