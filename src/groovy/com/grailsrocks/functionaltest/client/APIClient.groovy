package com.grailsrocks.functionaltest.client

import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException

import org.codehaus.groovy.grails.plugins.codecs.Base64Codec

import static groovyx.net.http.ContentType.*

import com.grailsrocks.functionaltest.dsl.RequestBuilder
import com.grailsrocks.functionaltest.util.TestUtils

class APIClient implements Client {

    def client = new RESTClient()
    def response
    def url
    def clientArgs
    def listener
    def responseString
    String requestMethod
    Map stickyHeaders = [:]
    def currentAuthInfo
    
    APIClient(ClientAdapter listener) {
        this.listener = listener
        client.parserRegistry = new EvilWizardsKilledByFireIncantationParserRegistry()
    }
    
    void clientChanged() {
    
    }

    String setAuth(type, user, credentials) {
        currentAuthInfo = [type:type, user:user, credentials:credentials]
    }

    void clearAuth() {
        currentAuthInfo = null
    }

    String setStickyHeader(String header, String value) {
        stickyHeaders[header] = value
    }

    String getRequestBody() {
        if (clientArgs.body != null) {
            return clientArgs.body
        } else {
            return ''
        }
    }
    
    void request(URL url, String method, Closure paramSetupClosure) {
        this.url = url
        this.requestMethod = method

        this.response = null
        this.responseString = null
        
        clientArgs = [uri: url, headers:[:]]

        // Set the authorization if any
        if (currentAuthInfo) {
            // @todo We could use client.auth.basic here?
            def encoded = Base64Codec.encode("${currentAuthInfo.user}:${currentAuthInfo.credentials}".getBytes('utf-8'))
            clientArgs.headers.Authorization = "Basic "+encoded
        }
        
        def wrapper
        if (paramSetupClosure) {
            def builder = new RequestBuilder(clientArgs)
            wrapper = builder.build(paramSetupClosure)
        }
        
        def headerLists = [stickyHeaders]
        if (wrapper) {
            headerLists << wrapper.headers
        }
        headerLists.each { headers ->
            for (entry in headers) { 
                clientArgs.headers[entry.key] = entry.value.toString()
            }
        }
        
        if (wrapper?.reqParameters) {
            // @todo RESTClient doesn't like if you use query and queryString together it seems
            clientArgs.query = [:]
            wrapper.reqParameters.each { pair ->
                clientArgs.query[pair[0]] = pair[1].toString()
            }
        }

        def requestType = "application/binary"
        def acceptType = "*/*"

        if (clientArgs.headers.'Content-Type') {
            requestType = clientArgs.headers.'Content-Type'
        }
        if (clientArgs.headers.'Accept') {
            acceptType = clientArgs.headers.'Accept'
        }

        // Override request type if uploading binary
        if (wrapper?.bodyIsUpload) {
            // Make the REST client just stream stuff up, we've set content type correctly
            // this tells it to use our content type
            requestType = BINARY
        }
        
        clientArgs.contentType = acceptType
        clientArgs.requestContentType = requestType
        
        
        switch (clientArgs.contentType) {
            case 'application/json':
            case 'text/json':
                if (wrapper?.body != null) {
                    clientArgs.body = wrapper.body
                }
                break;
            case 'text/xml':
                if (wrapper?.body != null) {
                    clientArgs.body = wrapper.body
                }
                break;
            default:
                if (wrapper?.body != null) {
                    clientArgs.body = wrapper.body
                }   
                break;
        }

        TestUtils.dumpRequestInfo(this)

        def event
        try {
            def methodName = method.toLowerCase()
            // @todo add failure handler here / stop failure handler being called
            response = client."${methodName}"(clientArgs)

            if (response.data != null) {

                // @todo need to copy the response data first, then mutate it to string also
                // as RESTClient only lets you read the response once.
                
                switch (response.contentType) {
                    case ~'application/json.*':
                    case ~'text/.*':
                        responseString = response.data.text
                        break;
                    default:
                        println "yresp is: ${response.data.getClass()}"
                        byte[] bytes = new byte[100] 
                        response.data.read(bytes)
                        responseString = "Binary file:\r\n" + new String(bytes, 'utf-8')
                        def n = response.data.available()
                        if (n) {
                            responseString += "\r\n and $n more bytes"
                        }
                        break;
                }
            } else {
                responseString = ''
            }

            event = new ContentChangedEvent(
                    client: this, 
                    url: this.url,
                    method: method,
                    eventSource: 'API client request',
                    statusCode: responseStatus)
        } catch (HttpResponseException e) {
            response = e.response
            event = new ContentChangedEvent(
                    client: this, 
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

    String getResponseStatusMessage() {
        if (response?.statusLine != null) {
            return response.statusLine.reasonPhrase
        } else {
            return ''
        }
    }

    String getResponseAsString() {
        responseString
    }

    String getResponseContentType() {
        response.contentType
    }

    String getResponseHeader(String name) {
        response.headers[name]?.value
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
}