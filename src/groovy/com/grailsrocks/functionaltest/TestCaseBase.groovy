
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
package com.grailsrocks.functionaltest

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.StackTraceUtils
import groovy.util.slurpersupport.GPathResult

import java.net.URLEncoder

import grails.util.GrailsUtil
import grails.converters.JSON
import grails.converters.XML
import grails.util.Environment

import com.grailsrocks.functionaltest.util.HTTPUtils
import com.grailsrocks.functionaltest.util.TestUtils

import junit.framework.AssertionFailedError

import com.grailsrocks.functionaltest.client.*

class TestCaseBase extends GroovyTestCase implements GroovyInterceptable, ClientAdapter {

    static MONKEYING_DONE
    
    static BORING_STACK_ITEMS = [
        'FunctionalTests', 
        'functionaltestplugin.', 
        'gant.', 
        'com.grailsrocks', 
        'com.gargoylesoftware', 'org.apache']
    
    static {
        StackTraceUtils.addClassTest { className ->
            if (BORING_STACK_ITEMS.find { item ->
                return className.startsWith(item)
            }) {
                return false
            } else {
                return null
            }
        }
    }
    
    def baseURL // populated via test script
    def urlStack = new ArrayList()
    boolean autoFollowRedirects = true
    def consoleOutput
    protected stashedClients = [:]
    String currentClientId
    Client currentClient
    def redirectUrl
    
    def contentTypeForJSON = 'application/json'
    def contentTypeForXML = 'text/xml'
    
    protected void setUp() {
        super.setUp()
                
        baseURL = System.getProperty('grails.functional.test.baseURL')
        
        if (!MONKEYING_DONE) {
            BrowserClient.initVirtualMethods()
            MONKEYING_DONE = true
        }
    }

    Class getDefaultClientType() {
        BrowserClient
    }
    
    void switchClient(Class<Client> type = getDefaultClientType()) {
        currentClient = type.newInstance(this)
    }
    
    Client getClient() {
        if (!currentClient) {
            switchClient()
            clientChanged()
        }
        return currentClient
    }

    protected void clientChanged() {
        currentClient?.clientChanged()
    }

    boolean isRedirectEnabled() {
        autoFollowRedirects
    }
    
    void setRedirectEnabled(boolean enabled) {
        autoFollowRedirects = enabled
    }

    /**
     * Call to switch between multiple client browsers, simulating different users
     */
    void client(String id) {
        //System.out.println "Stashed clients: ${stashedClients.dump()}"
        if (id != currentClientId) {
            // If we were currently unnamed but have some state, save our state with name ""
            stashClient(currentClientId ?: '')
            // restore client if it is known, else 
            unstashClient(id)
            currentClientId = id
        }
    }

    protected void stashClient(id) {
        stashedClients[id] = client
        currentClient = null
    }

    protected void unstashClient(id) {
        // Clear them in case this is a new unknown client name
        def c = stashedClients[id]
        if (c) {
            currentClient = c
        }
        
        clientChanged()
    }
    
    protected void tearDown() {
        currentClient = null
        consoleOutput.println('') // force newline
        super.tearDown()
    }
    
    def invokeMethod(String name, args) {
        def t = this
        // Let's not mess with internal calls, or it is a nightmare to debug
        if (name.startsWith('__')) {
            return InvokerHelper.getMetaClass(this).invokeMethod(this,name,args)
        } else if ((name.startsWith('assert') || 
                name.startsWith('shouldFail') || 
                name.startsWith('fail')) ) {
            try {
                return InvokerHelper.getMetaClass(this).invokeMethod(this,name,args)
            } catch (Throwable e) {
                // Protect against nested func test exceptions when one assertX calls another
                if (!(e instanceof FunctionalTestException)) {
                    __reportFailure(__sanitize(e))
                    throw __sanitize(new FunctionalTestException(this, e))
                } else throw e
            }
        } else {
            try {
                //System.out.println "Invoking: ${name} - $args"
                return InvokerHelper.getMetaClass(this).invokeMethod(this,name,args)
            } catch (Throwable e) {
                if (!(e instanceof FunctionalTestException)) {
                    __reportFailure(__sanitize(e))
                    throw __sanitize(new FunctionalTestException(this, e))
                } else throw e
            }
        }
    }
    
    protected __sanitize(Throwable t) {
        StackTraceUtils.deepSanitize(t)
    }

    protected void __reportFailure(e) {
        // Write out to user console
        def msg
        if (!e.message) {
            msg = "[no message available]"
        } else {
            msg = e.message
        }
        def out = consoleOutput ?: System.out
        out.println "\nFailed: ${msg}"
        e.printStackTrace(out) 
        if (e.cause) {
            out.println "\nFailed: ${msg}"
            e.cause.printStackTrace(out) 
        }
        // Write to output capture file
        //System.out.println "\nFailed: ${msg}"
        if (urlStack) {
            out.println "URL: ${urlStack[-1].url}"
        }
        out.println ""
    }
    
    void followRedirect() {
	    if (redirectEnabled) {
	        throw new IllegalStateException("Trying to followRedirect() but you have not disabled automatic redirects so I can't! Do redirectEnabled = false first, then call followRedirect() after asserting.")
	    }
        doFollowRedirect()
    }
    
    protected void doFollowRedirect() {
        def u = redirectUrl
        if (u) {
            get(u) // @todo should be same HTTP method as previous request?
            System.out.println("Followed redirect to $u")
        } else {
            throw new IllegalStateException('The last response was not a redirect, so cannot followRedirect')
        }
    }

    def forceTrailingSlash(url) {
        if (!url.endsWith('/')) {
           url += '/'
        }
        return url
    }
    
    URL makeRequestURL(url) {
        def reqURL
        url = url.toString()
        if ((url.indexOf('://') >= 0) || url.startsWith('file:')) {
            reqURL = url.toURL()
        } else {
            def base
            if (url.startsWith('/')) {
                base = forceTrailingSlash(baseURL)
                url -= '/'
            } else {
                base = client.currentURL ? client.currentURL : baseURL                 
            }
            reqURL = new URL(new URL(base), url.toString())
        }        
        return reqURL
    }

    protected handleRedirects() {
        if (HTTPUtils.isRedirectStatus(client.responseStatus)) {
            if (autoFollowRedirects) {
                this.doFollowRedirect()
            }
        }
    }

    def doRequest(String url, String method, Closure paramSetup = null) {
        // @todo build URL like we used to, relative to the app:
        URL u = makeRequestURL(url)
        
        redirectUrl = null
	    client.request(u, method, paramSetup)
    }
    
	def get(url, Closure paramSetup = null) {
	    doRequest(url, 'GET', paramSetup)
	}

	def post(url, Closure paramSetup = null) {
	    doRequest(url, 'POST', paramSetup)
	}
	
	def delete(url, Closure paramSetup = null) {
	    doRequest(url, 'DELETE', paramSetup)
	}
	
	def put(url, Closure paramSetup = null) {
	    doRequest(url, 'PUT', paramSetup)
	}
	
	void assertContentDoesNotContain(String expected) {
	    assertFalse "Expected content to not loosely contain [$expected] but it did".toString(), stripWS(client.responseAsString?.toLowerCase()).contains(stripWS(expected?.toLowerCase()))
	}

	void assertContentContains(String expected) {
	    assertTrue "Expected content to loosely contain [$expected] but it didn't".toString(), stripWS(client.responseAsString?.toLowerCase()).contains(stripWS(expected?.toLowerCase()))
	}

	void assertContentContainsStrict(String expected) {
	    assertTrue "Expected content to strictly contain [$expected] but it didn't".toString(), client.responseAsString?.contains(expected)
	}

	void assertContent(String expected) {
	    assertEquals stripWS(expected?.toLowerCase()), stripWS(client.responseAsString?.toLowerCase())
	}

	void assertContentStrict(String expected) {
	    assertEquals expected, client.responseAsString
	}

    void expect(Map args) {
        if (args.status) {
            assertStatus(args.status)
        }
        if (args.contentType) {
            assertContentType(args.contentType)
        }
        if (args.contentTypeStrict) {
            assertContentTypeString(args.contentTypeStrict)
        }
        if (args.redirectUrl) {
            assertRedirectUrl(args.redirectUrl)
        }
        if (args.redirectUrlContains) {
            assertRedirectUrlContains(args.redirectUrlContains)
        }
        if (args.content) {
            assertContent(args.content)
        }
        if (args.contentStrict) {
            assertContentStrict(args.contentStrict)
        }
        if (args.contentContainsStrict) {
            assertContentContainsStrict(args.contentContainsStrict)
        }
    }
    
	void assertStatus(int status) {
	    def msg = "Expected HTTP status [$status] but was [${client.responseStatus}]"
	    if (HTTPUtils.isRedirectStatus(client.responseStatus)) msg += " (received a redirect to ${redirectUrl})"
	    assertTrue msg.toString(), status == client.responseStatus
	}
    
	void assertRedirectUrl(String expected) {
	    if (redirectEnabled) {
	        throw new IllegalStateException("Asserting redirect, but you have not disabled redirects. Do redirectEnabled = false first, then call followRedirect() after asserting.")
	    }
	    if (!HTTPUtils.isRedirectStatus(client.responseStatusCode)) {
	        throw new AssertionFailedError("Asserting redirect, but response was not a valid redirect status code")
	    }
	    assertEquals expected, redirectUrl
	}

	void assertRedirectUrlContains(String expected) {
	    if (redirectEnabled) {
	        throw new IllegalStateException("Asserting redirect, but you have not disabled redirects. Do redirectEnabled = false first, then call followRedirect() after asserting.")
	    }
	    if (!HTTPUtils.isRedirectStatus(client.responseStatus)) {
	        throw new AssertionFailedError("Asserting redirect, but response was not a valid redirect status code")
	    }
	    if (!edirectUrl?.contains(expected)) {
            throw new AssertionFailedError("Asserting redirect contains [$expected], but it didn't. Was: [${redirectUrl}]")
        }
	}

	void assertContentTypeStrict(String expected) {
	    assertEquals expected, client.responseContentType
	}

	void assertContentType(String expected) {
	    assertTrue "Expected content type to match [${expected}]", stripWS(client.responseContentType.toLowerCase()).startsWith(stripWS(expected?.toLowerCase()))
	}

	void assertHeader(String header, String expected) {
	    assertEquals stripWS(expected.toLowerCase()), stripWS(client.getResponseHeader(header)?.toLowerCase())
	}

	void assertHeaderStrict(String header, String expected) {
	    assertEquals expected, client.getResponseHeader(header)
	}

	void assertHeaderContains(String header, String expected) {
	    assertTrue "Expected header [$header] to match [${expected}]", stripWS(client.getResponseHeader(header)?.toLowerCase()).contains(stripWS(expected?.toLowerCase()))
	}

	void assertHeaderContainsStrict(String header, String expected) {
	    assertTrue "Expected header [$header] to strictly match [${expected}]", client.getResponseHeader(header)?.contains(expected)
	}

    /** 
     * Make sure a domain object exists in the target system
     * Relies on access to our testing controller
     * @todo don't use get for this! it screws up url stack
     */
/*
    void assertDomainObjectExists( Map args) {
        get('/functionaltesting/objectExists') {
            className = args.className
            findField = args.findBy
            findValue = args.value
        }
        
        assertStatus 200
    }
*/

    /** 
     * Make sure a domain object exists in the target system
     * Relies on access to our testing controller
     * @todo don't use get for this! it screws up url stack
     */
/*
    grails.converters.JSON getDomainObject( Map args) {
        get('/functionaltesting/findObject') {
            className = args.className
            findField = args.findBy
            findValue = args.value
        }
        
        assertStatus 200
        
        return response.contentAsString.decodeJSON()
    }
*/    
    GPathResult getJSON() {
        assertContentType contentTypeForJSON
        grails.converters.JSON.parse(client.responseAsString)
    }

    GPathResult getXML() {
        assertContentType contentTypeForXML
        grails.converters.XML.parse(client.responseAsString)
    }
    
    /**
     * Set the Authorization header
     */
    void authBasic(String user, String pass) {
        println "Authentication set to: Basic $user:$pass"
        client.setAuth('Basic', user, pass)
    }
    
    /**
     * Set the Authorization header
     */
    void clearAuth() {
        client.clearAuth()
    }
    
    /**
     * Load a fixture into the app using the fixtures plugin
     */
    void fixture(String name) {
        def result = testDataRequest('fixture', [name:name])
        if (result.error) {
            throw new UnsupportedOperationException("Cannot load fixture [$name], the application replied with: [{}$result.error}]")
        }
    }
    
    def URLEncode(x) {
        URLEncoder.encode(x.toString(), 'utf-8')
    }
    
    /**
     * Send a request to the test data controller that this plugin injects into non-production apps
     * @param action The name of the controller action to execute eg findObject
     * @param params The query args
     * @return The JSON response object
     */
    def testDataRequest(action, params ) {
        def args = (params.collect { k, v -> k+'='+URLEncode(v) }).join('&')
        grails.converters.JSON.parse(makeRequestURL("/functionaltesting/$action?$args").text)
    }
    
    /**
     * Assert that the mock mail system has a mail matching the specified args
     */
    void assertEmailSent( Map args) {
        try {
            def result = makeRequestURL('/greenmail/list').text
            result = result?.toLowerCase()
            if (result.indexOf(args.to.toLowerCase()) < 0 || result.indexOf(args.subject?.toLowerCase()) < 0) {
    	        throw new AssertionFailedError("There was no email to an address containing [$args.to] with subject containing [$args.subject] found - greenmail had the following: ${result}")
            }
        } catch (FileNotFoundException fnfe) {
            throw new UnsupportedOperationException("Cannot interact with mocked mails, the application does not have the 'greenmail' plugin installed or url mapping for /greenmail/\$action? is missing")
        }
    }

    /** 
     * Clear the greenmail email queue.
     * @todo should do this after every test run from the test runner
     */
    void clearEmails() {
        def result = makeRequestURL('/greenmail/clear').text
    }

    /** 
     * Extract the first match from the contentAsString using the supplied regex
     */
    String extract(regexPattern) {
        def m = client.responseAsString =~ regexPattern
        return m ? m[0][1] : null
    }
    
/*
	void assertXML(String xpathExpr, expectedValue) {
		
	}
*/
    String stripWS(String s) {
        def r = new StringBuffer()
        s?.each { c ->
            if (!Character.isWhitespace(c.toCharacter())) r << c
        }
        r.toString()
    }
    
    protected newResponseReceived(Client client) {
        if (HTTPUtils.isRedirectStatus(client.responseStatus)) {
            redirectUrl = client.getResponseHeader('Location')
            System.out.println("Response was a redirect to ${redirectUrl} ${'<'*20}")
        } else {
            redirectUrl = null
        }
        TestUtils.dumpResponseHeaders(client)
        TestUtils.dumpContent(client)

        // Now let's see if it was a redirect
        handleRedirects()
    }

    void contentChanged(ContentChangedEvent event) {
        newResponseReceived(event.client)
        
        // params.method ? params.method.toString()+' ' : 
        consoleOutput.print '#'
        while(urlStack.size() >= 50){ // only keep a window of the last 50 urls
            urlStack.remove(0)
        }
        urlStack << event
    }    
}


