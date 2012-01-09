/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The original code of this plugin was developed by Historic Futures Ltd.
 * (www.historicfutures.com) and open sourced.
 */
package com.grailsrocks.functionaltest.dsl

class RequestBuilder {
    List reqParameters = []
    Map reqParametersByName = [:]
    Map headers = [:]
    String body
    def clientConfig // This could be HTMLUnit or RestClient etc
    
    RequestBuilder(clientConfig) {
        this.clientConfig = clientConfig
    }
    
    void headers(Closure c) {
        c.delegate = headers
        c.call()
    }

    void setProperty(String name, def value) {
        reqParameters << [name, value]
        def existingByName = reqParametersByName[name]
        // Sorry, I'm being evil, and no I can't even remember what it does currently! :)
        (existingByName != null) ? (existingByName instanceof List ? 
            (existingByName << value) : (reqParametersByName[name] = [existingByName, value])) : 
            (reqParametersByName[name] = value) 
    }

    void body(Closure c) {
        bodyString = c()?.toString() // call the closure and use result
    }
    
    void body(Map args) {
        if (args.file) {
            headers['Content-Type'] = 'image/jpeg' // work out mime type
            
            // Hope that client does close the stream
            body = f.newInputStream() 
        } else {
            if (args.json) {
                headers['Content-Type'] = 'text/json'
                body = args.json
            } else if (args.xml) {
                headers['Content-Type'] = 'text/xml'
                body = args.xml
            }
        }
    }
    
    void xml(def value) {
        body(xml:value)
    }
    
    void file(def value) {
        body(file:value)
    }
    
    void json(def value) {
        body(json:value)
    }
    
    def missingMethod(String name, args) {
        if (args.size() == 1) {
            this[name] = args[1]
        } else {
            throw NoSuchMethodException("No such method $name - you can only invoke methods with a single argument to set request parameters")
        }
    }
    
    def getProperty(String name) {
        switch (name) {
            case 'headers': return headers
            case 'settings': return clientConfig
            default: return reqParametersByName[name]
        }
    }
}
