package com.grailsrocks.functionaltest.client

interface Client {
    
    void clientChanged()
    void request(URL url, String method, Closure setupDSL)
    
    String setStickyHeader(String header, String value)
    String setAuth(type, user, credentials)
    void clearAuth()
    
    String getRequestMethod()
    String getRequestBody()
    Map getRequestHeaders()
    Map getRequestParameters()
    
    int getResponseStatus()
    String getResponseStatusMessage()
    
    String getResponseAsString()
    String getResponseContentType()
    String getResponseHeader(String name)
    Map getResponseHeaders()

    String getCurrentURL()    
}