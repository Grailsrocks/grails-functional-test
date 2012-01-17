package com.grailsrocks.functionaltest

import junit.framework.AssertionFailedError
import com.gargoylesoftware.htmlunit.ElementNotFoundException
import com.gargoylesoftware.htmlunit.html.HtmlForm

import com.grailsrocks.functionaltest.client.BrowserClient
import com.grailsrocks.functionaltest.client.htmlunit.*
import com.grailsrocks.functionaltest.client.Client

class BrowserTestCase extends TestCaseBase {
    void setBrowser(String browser) {
        client.clientState.browser = browser
    }

    @Override
    Client getClient() {
        def c = super.getClient()
        if (c instanceof BrowserClient) {
            return c
        } else {
            throw new IllegalArgumentException("Cannot change browser, current client is not a browser")
        }
    }

    def getBrowser() {
        client.clientState.browser
    }
    
    void clearCache() {
        client.cache.clear()
    }
    
    boolean getCookiesEnabled() {
        client.cookieManager.cookiesEnabled
    }
    
    void setCookiesEnabled(boolean enabled) {
        client.cookieManager.cookiesEnabled = enabled
    }
    
    boolean getJavaScriptEnabled() {
        client.javaScriptEnabled
    }
    
    void setJavaScriptEnabled(boolean enabled) {
        client.javaScriptEnabled = enabled
    }

    void setPopupBlockerEnabled(boolean enabled) {
        client.popupBlockerEnabled = enabled
    }

    boolean getPopupBlockerEnabled() {
        client.popupBlockerEnabled
    }

    /**
     * Return the list of cookie objects from HtmlUnit
     */
    def getCookies() {
        client.cookieManager.cookies
    }
    
    void back() {
        if (urlStack.size() < 2) {
            throw new IllegalStateException('You cannot call back() without first generating at least 2 responses')
        }
        urlStack.remove(urlStack[-1])
        def lastPage = urlStack[-1]
        while (urlStack.size() > 1 && isRedirectStatus(lastPage.statusCode)) {
            urlStack.remove(urlStack[-1])
            lastPage = urlStack[-1]
        }
        if (isRedirectStatus(lastPage.statusCode)) {
            throw new IllegalStateException('Unable to find a non-redirect URL in the history')
        }
        def c = client
        c._page = lastPage.page
        c.response = lastPage.response
    }
    
    def getPage() {
        assertNotNull "Page must never be null!", client._page
        return client._page        
    }
    
    def byXPath(expr) {
        try {
            def results = page.getByXPath(expr.toString())
            if (results.size() > 1) {
                return results
            } else {
                return results[0]
            }
        } catch (ElementNotFoundException e) {
            return null
        }
    }
    
    def byId(id) {
        try {
            return page.getElementById(id.toString())
        } catch (ElementNotFoundException e) {
            return null
        }
    }
        
    def byClass(cssClass) {
        try {
            def results = page.getByXPath("//*[@class]").findAll { element ->
                def attribute = element.attributes?.getNamedItem('class')
                
                return attribute?.value?.split().any { it == cssClass }
            }
            if (results.size() > 1) {
                return results
            } else {
                return results[0]
            }
        } catch (ElementNotFoundException e) {
            println "No element found for class $cssClass"
            return null
        }
    }

    def byName(name) {
        def elems = page.getElementsByName(name.toString())
        if (elems.size() > 1) { 
            return elems // return the list
        } else if (!elems) {
            return null
        }
        return elems[0] // return the single element
    }
    
    
    /**
     * Get the first HTML form on the page, if any, and run the closure on it. 
     * Useful when form has no name.
     * @param closure Optional closure containing code to set/get form fields
     * @return the HtmlUnit form object
     */
    def form(Closure closure) {
        def f = page.forms?.getAt(0)
        if (!f) {
            throw new IllegalArgumentException("There are no forms in the current response")
        }
        processForm(f, closure)
    }
    
    /**
     * Get the HTML form by ID or name, with an optional closure to set field values
     * on the form
     * @param closure Optional closure containing code to set/get form fields
     * @return the HtmlUnit form object
     */
    def form(name, Closure closure) {
        def f = byId(name)
        if (!f) {
            f = page.getFormByName(name)
        }
        if (!f) {
            throw new IllegalArgumentException("There is no form with id/name [$name]")
        }
        processForm(f, closure)
    }

    /**
     * Check the form is valid, and then if necessary run the closure delegating to the form
     * wrapper to implement all our magic
     * @return the HtmlUnit form object
     */
    protected processForm( form, Closure closure = null) {
        if (!(form instanceof HtmlForm)) {
            throw new IllegalArgumentException("Element of id/name $name is not an HTML form")
        }
        if (closure) {
            closure.delegate = new FormWrapper(this, form)
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.call()
        }
        return form
    }

    def getResponse() {
        client.response
    }
    
    protected makeRequest(url, method, paramSetupClosure) {
        System.out.println("\n\n${'>'*20} Making request to $url using method $method ${'>'*20}")
        
        def reqURL = makeRequestURL(url)
            
        System.out.println("Initializing web request settings for $reqURL")
        client.request(reqURL, method, paramSetupClosure)

        // Now let's see if it was a redirect
        handleRedirects()
    }

    /**
     * Experimental code in HtmlUnit is called here. May change in future, YMMV
     */
    void waitForScripts(timeout) {
        consoleOutput.println "Waiting for JavaScripts, timeout $timeout"
        def n = client.waitForBackgroundJavaScript(timeout) 
        consoleOutput.println "Finished waiting for JavaScripts, pending tasks: $n"
    }
    
	/**
	 * Clicks an element, finding the link/button by first the id attribute, or failing that the clickable text of the link.
	 */
	def click(anchor) {
	    def a = byId(anchor)
	    try {
	        if (!a) a = page.getAnchorByText(anchor)
        } catch (ElementNotFoundException e) {
        }
        if (!a) {
	        throw new IllegalArgumentException("No such element for id or anchor text [${anchor}]")
        }
        System.out.println "Clicked [$anchor] which resolved to a [${a.class}]"
        a.click()
        // page loaded, events are triggered if necessary

        // Now let's see if it was a redirect
        handleRedirects()
	}
	
	void assertTitleContains(String expected) {
        boolean con = stripWS(page.titleText.toLowerCase()).contains(stripWS(expected?.toLowerCase()))
	    assertTrue "Expected title of response to loosely contain [${expected}] but was [${page.titleText}]".toString(), con
	}

	void assertTitle(String expected) {
	    assertTrue "Expected title of response to loosely match [${expected}] but was [${page.titleText}]".toString(),
	        stripWS(expected?.toLowerCase()) == stripWS(page.titleText.toLowerCase())
	}

	void assertMetaContains(String name, String expected) {
	    def node = page.getElementsByTagName('meta')?.iterator().find { it.attributes?.getNamedItem('name')?.nodeValue == name }
	    if (!node) throw new AssertionFailedError("No meta tag found with name $name")
        def nodeValue = node.attributes.getNamedItem('content').nodeValue
	    assertTrue stripWS(nodeValue.toLowerCase()).contains(stripWS(expected?.toLowerCase()))
	}

	void assertMeta(String name) {
	    def node = page.getElementsByTagName('meta')?.iterator().find { it.attributes?.getNamedItem('name')?.nodeValue == name }
	    if (!node) throw new AssertionFailedError("No meta tag found with name $name")
	}

    void assertCookieExists(String cookieName) {
        if (!client.cookieManager.getCookie(cookieName)) {
            def cookieList = (client.cookieManager.cookies.collect { it.name }).join(',')
	        throw new AssertionFailedError("There is no cookie with name $cookieName, the cookies that exist are: $cookieList")
        }
    }

    void assertCookieExistsInDomain(String cookieName, String domain) {
        def domainCookies = client.cookieManager.getCookies(cookieName)
        if (!domainCookies) {
            def cookieList = (client.cookieManager.cookies.collect { it.name }).join(',')
	        throw new AssertionFailedError("There are no cookies for domain $domain")
        }
        assertTrue domainCookies.find { it.name == cookieName }
    }

    void assertCookieContains(String cookieName, String value) {
        assertCookieExists(cookieName)
        def v = client.cookieManager.getCookie(cookieName)
        assertTrue stripWS(v.toLowerCase()).contains(stripWS(value?.toLowerCase()))
    }

    void assertCookieContainsStrict(String cookieName, String value) {
        assertCookieExists(cookieName)
        def v = client.cookieManager.getCookie(cookieName)
        assertTrue v.contains(value)
    }

	void assertElementTextContains(String id, String expected) {
	    def node = byId(id)
	    if (!node) throw new IllegalArgumentException("No element found with id $id")
	    assertTrue stripWS(node.textContent.toLowerCase()).contains(stripWS(expected?.toLowerCase()))
	}

	void assertElementTextContainsStrict(String id, String expected) {
	    def node = byId(id)
	    if (!node) throw new IllegalArgumentException("No element found with id $id")
	    assertTrue node.textContent.contains(expected)
	}

    
}