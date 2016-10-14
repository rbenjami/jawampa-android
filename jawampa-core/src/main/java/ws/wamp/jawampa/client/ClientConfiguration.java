/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package ws.wamp.jawampa.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ws.wamp.jawampa.WampRoles;
import ws.wamp.jawampa.auth.client.ClientSideAuthentication;
import ws.wamp.jawampa.connection.IWampConnector;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.internal.Version;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores various configuration data for WAMP clients
 */
public class ClientConfiguration {
    private final boolean closeClientOnErrors;
    
    private final String authId;
    private final List<ClientSideAuthentication> authMethods;
    
    private final Gson gson;

    private final URI routerUri;
    private final String realm;
    private final boolean useStrictUriValidation;
    
    private final WampRoles[] clientRoles;
    
    private final int totalNrReconnects;
    private final int reconnectInterval;
    
    /** The provider that should be used to obtain a connector */
    private final IWampConnectorProvider connectorProvider;
    /** The connector which is used to create new connections to the remote peer */
    private final IWampConnector connector;

    private final JsonObject helloDetails;
    
    public ClientConfiguration(
        boolean closeClientOnErrors,
        String authId,
        List<ClientSideAuthentication> authMethods,
        URI routerUri,
        String realm,
        boolean useStrictUriValidation,
        WampRoles[] clientRoles,
        int totalNrReconnects,
        int reconnectInterval,
        IWampConnectorProvider connectorProvider,
        IWampConnector connector, Gson gson )
    {
        this.closeClientOnErrors = closeClientOnErrors;
        
        this.authId = authId;
        this.authMethods = authMethods;
        
        this.routerUri = routerUri;
        this.realm = realm;
        
        this.useStrictUriValidation = useStrictUriValidation;
        
        this.clientRoles = clientRoles;
        
        this.totalNrReconnects = totalNrReconnects;
        this.reconnectInterval = reconnectInterval;
        
        this.connectorProvider = connectorProvider;
        this.connector = connector;
        this.gson = gson;

        // Put the requested roles in the Hello message
        helloDetails = new JsonObject();
        helloDetails.addProperty( "agent", Version.getVersion());

        JsonObject rolesNode = new JsonObject();
        for (WampRoles role : clientRoles) {
            JsonObject featuresNode = new JsonObject();
            if (role == WampRoles.Publisher )
                featuresNode.addProperty("publisher_exclusion", true);
            else if (role == WampRoles.Subscriber)
                featuresNode.addProperty("pattern_based_subscription", true);
            else if (role == WampRoles.Caller)
                featuresNode.addProperty("caller_identification", true);
            rolesNode.add( role.toString(), featuresNode );
        }
        helloDetails.add( "roles", rolesNode );

        // Insert authentication data
        if(authId != null) {
            helloDetails.addProperty("authid", authId);
        }
        if (authMethods != null && authMethods.size() != 0) {
            JsonArray authMethodsNode = new JsonArray();
            for(ClientSideAuthentication authMethod : authMethods) {
                authMethodsNode.add(authMethod.getAuthMethod());
            }
            helloDetails.add( "authmethods", authMethodsNode );
        }
    }
    
    public boolean closeClientOnErrors() {
        return closeClientOnErrors;
    }
    
    public Gson gson() {
        return gson;
    }
    
    public URI routerUri() {
        return routerUri;
    }
    
    public String realm() {
        return realm;
    }
    
    public boolean useStrictUriValidation() {
        return useStrictUriValidation;
    }
    
    public int totalNrReconnects() {
        return totalNrReconnects;
    }
    
    public int reconnectInterval() {
        return reconnectInterval;
    }
    
    /** The connector which is used to create new connections to the remote peer */
    public IWampConnector connector() {
        return connector;
    }

    public WampRoles[] clientRoles() {
        return clientRoles.clone();
    }
    
    public String authId() {
        return authId;
    }
    
    public List<ClientSideAuthentication> authMethods() {
        return new ArrayList<ClientSideAuthentication>(authMethods);
    }

    public IWampConnectorProvider connectorProvider() {
        return connectorProvider;
    }

    JsonObject helloDetails(){
        return helloDetails;
    }
}
