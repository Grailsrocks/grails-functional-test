package com.grailsrocks.functionaltest

import com.grailsrocks.functionaltest.client.*

class HybridTestCase extends BrowserTestCase {
    private Class _defaultClientType = BrowserClient
    
    Class getDefaultClientType() {
        _defaultClientType
    }

    void defaultClientType(String clientType) {
        switch (clientType) {
            case 'API': _defaultClientType = APIClient;
                break;
            case 'Browser': _defaultClientType = BrowserClient;
                break;
            default:
                throw new IllegalArgumentException("Only 'API' and 'Browser' are supported values for client type")
        } 
    }

    
    boolean __isDSLMethod(String name) {
        if (super.__isDSLMethod(name)) {
            return true
        }
        
        name == 'withAPI' ||
        name == 'withBrowser'
    }
    

    def withAPI(Closure c) {
        def oldclient = currentClientId
        client('API', APIClient)
        c()
        client(oldclient)
    }
    
    def withAPI(String id, Closure c) {
        def oldclient = currentClientId
        client(id, APIClient)
        c()
        client(oldclient)
    }
    
    def withBrowser(String id, Closure c) {
        def oldclient = currentClientId
        client(id, BrowserClient)
        c()
        client(oldclient)
    }

    def withBrowser(Closure c) {
        def oldclient = currentClientId
        client('Browser', BrowserClient) 
        c()
        client(oldclient)
    }
}