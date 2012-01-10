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

import com.grailsrocks.functionaltest.util.HTTPUtils

class RequestBuilder {
    def ___data
    def ___clientConfig
    
    RequestBuilder(clientConfig) {
        this.@___clientConfig = clientConfig
    }
    
    def build(Closure paramSetupClosure) {
        this.@___data = [
            reqParameters:[],
            reqParametersByName:[:],
            headers:[:],
            body:null,
            bodyIsUpload:false
        ]
        paramSetupClosure.delegate = this
        paramSetupClosure.call()
        return this.@___data
    }

    void headers(Closure c) {
        c.delegate = this.@___data.headers
        c.call()
    }

    void setProperty(String name, def value) {
        this.@___data.reqParameters << [name, value]
        def existingByName = this.@___data.reqParametersByName[name]
        // Sorry, I'm being evil, and no I can't even remember what it does currently! :)
        (existingByName != null) ? (existingByName instanceof List ? 
            (existingByName << value) : (this.@___data.reqParametersByName[name] = [existingByName, value])) : 
            (this.@___data.reqParametersByName[name] = value) 
    }

    void body(Closure c) {
        this.@___data.body = c()?.toString() // call the closure and use result
    }

    void ___setContentTypeIfNotAlreadySet(String value) {
        if (!this.@___data.headers['Content-Type']) {
            this.@___data.headers['Content-Type'] = value
        }
    }
    
    void body(Map args) {
        if (args.file) {
            this.@___data.bodyIsUpload = true
            switch (args.file) {
                case File: 
                    // @todo look up mime type
                    ___setContentTypeIfNotAlreadySet(HTTPUtils.getMimeTypeOfFile(args.file.toString()))
                    this.@___data.body = args.file.newInputStream()
                    break;
                case InputStream:
                    this.@___data.body = args.file
                    break;
                default:
                    def fn = args.file.toString()
                    ___setContentTypeIfNotAlreadySet(HTTPUtils.getMimeTypeOfFile(fn))
                    this.@___data.body = new File(fn).newInputStream()
                    break;
            }
            ___setContentTypeIfNotAlreadySet('application/binary')
        } else {
            if (args.json) {
                ___setContentTypeIfNotAlreadySet('text/json')
                this.@___data.body = args.json
            } else if (args.xml) {
                this.@___data.headers['Content-Type'] = 'text/xml'
                this.@___data.body = args.xml
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
    
    void contentType(String type) {
        setContentType(type)
    }
    
    void setContentType(String type) {
        this.@___data.headers['Content-Type'] = type
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
            case 'headers': return this.@___data.headers
            case 'settings': return this.@___clientConfig
            default: return this.@___clientConfig.reqParametersByName[name]
        }
    }
}
