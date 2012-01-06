package com.grailsrocks.functionaltest.client

interface Client {
    
    void clientChanged()
    void request(URL url, String method, Closure setupDSL)
    
    Map getRequestHeaders()
    Map getRequestParameters()
    int getResponseStatus()
    String getResponseAsString()
    def getResponseDOM()
    String getResponseContentType()
    String getResponseHeader(String name)
    Map getResponseHeaders()
    String getCurrentURL()
    
    String getRedirectURL()
    String followRedirect()
}