package com.grailsrocks.functionaltest.client

import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException

import com.grailsrocks.functionaltest.dsl.RequestBuilder

class APIClient implements Client {

    def client = new RESTClient()
    def response
    def url
    def clientArgs
    def listener

    APIClient(ClientAdapter listener) {
        this.listener = listener
    }
    
    void clientChanged() {
    
    }

    void request(URL url, String method, Closure paramSetupClosure) {
        this.url = url
    
        clientArgs = [uri: url, headers:[:], query:[:]]
        if (paramSetupClosure) {
            def wrapper = new RequestBuilder(clientArgs)
            paramSetupClosure.delegate = wrapper
            paramSetupClosure.call()
            
            wrapper.@headers.each { entry ->
                clientArgs.headers[entry.key] = entry.value.toString()
            }

            if (wrapper.@reqParameters) {
                def params = []
                wrapper.@reqParameters.each { pair ->
                    clientArgs.query[pair[0]] = pair[1].toString()
                }
            }
            
            switch (clientArgs.headers.'Content-Type') {
                case 'application/json':
                case 'text/json':
                    // If we're JSON and a string we need to force the converter to not try to do anything
                    /*if (wrapper.@body instanceof String) {
                        clientArgs.requestContentType = 'text/plain'
                    } 
                    if (!(wrapper.@body instanceof Map) || !(wrapper.@body instanceof Closure)) {
                        throw new IllegalArgumentException('Cannot work out what you are trying to submit in this JSON request, your body is a [${wrapper.@body?.getClass()}]')
                    } */
                    clientArgs.body = wrapper.@body
                    break;
                case 'text/xml':
                    clientArgs.body = wrapper.@body
                    break;
                default:
                    clientArgs.body = wrapper.@body
                    break;
            }
        }

        // twitter auth omitted
        def event
        try {
            response = client."${method.toLowerCase()}"(clientArgs)
            event = new ContentChangedEvent(
                    url: this.url,
                    method: method,
                    eventSource: 'API client request',
                    statusCode: responseStatus)
        } catch (HttpResponseException e) {
            response = e.response
            event = new ContentChangedEvent(
                    url: this.url,
                    method: method,
                    eventSource: 'API client request failure',
                    statusCode: responseStatus)
        }

        listener.contentChanged( event)
    }
    
    Map getRequestHeaders() {
        clientArgs.headers
    }

    Map getRequestParameters() {
        clientArgs.query
    }

    int getResponseStatus() {
        response.status
    }

    String getResponseAsString() {
        response.data.text
    }

    def getResponseDOM() {
        throw new RuntimeException('Not supported')
    }

    String getResponseContentType() {
        response.contentType
    }

    String getResponseHeader(String name) {
        response.headers[name].value
    }

    Map getResponseHeaders() {
        def r = [:]
        for (h in response.headers) {
            r[h.name] = h.value
        }
        return r
    }

    String getCurrentURL() {
        url.toString()
    }

    String getRedirectURL() {
        null
    }

    String followRedirect()  {
        null
    }
}