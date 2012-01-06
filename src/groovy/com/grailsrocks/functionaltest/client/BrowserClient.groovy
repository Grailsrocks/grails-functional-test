package com.grailsrocks.functionaltest.client

import com.gargoylesoftware.htmlunit.WebRequestSettings
import com.gargoylesoftware.htmlunit.util.NameValuePair
import com.gargoylesoftware.htmlunit.WebRequestSettings
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebWindowEvent
import com.gargoylesoftware.htmlunit.WebWindowListener
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlInput
import com.gargoylesoftware.htmlunit.html.HtmlForm
import com.gargoylesoftware.htmlunit.html.HtmlSelect
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput
import com.gargoylesoftware.htmlunit.HttpMethod
import com.gargoylesoftware.htmlunit.ElementNotFoundException
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeListener
import com.gargoylesoftware.htmlunit.html.DomChangeListener
import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeEvent
import com.gargoylesoftware.htmlunit.html.DomChangeEvent
import com.gargoylesoftware.htmlunit.WebWindow
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.Page

import com.grailsrocks.functionaltest.dsl.RequestBuilder
import com.grailsrocks.functionaltest.client.htmlunit.*

import com.grailsrocks.functionaltest.util.HTTPUtils

class BrowserClient implements Client, WebWindowListener, HtmlAttributeChangeListener, DomChangeListener {
    WebRequestSettings settings 

    def interceptingPageCreator = new InterceptingPageCreator(this)

    def _client
    def mainWindow
    def browser
    def response
    def redirectUrl
    def _page
    
    ClientAdapter listener

    BrowserClient(ClientAdapter listener) {
        this.listener = listener
        println "Creating new client"
        _client = browser ? new WebClient(BrowserVersion[browser]) : new WebClient()
        println "Created new client ${_client }"
        _client.addWebWindowListener(this)
        _client.redirectEnabled = false // We're going to handle this thanks very much
        _client.popupBlockerEnabled = true 
        _client.javaScriptEnabled = true 
        _client.throwExceptionOnFailingStatusCode = false
        _client.pageCreator = interceptingPageCreator
    }

    boolean getJavaScriptEnabled() {
        _client.javaScriptEnabled
    }
    
    void setJavaScriptEnabled(boolean enabled) {
        _client.javaScriptEnabled = enabled
    }

    void setPopupBlockerEnabled(boolean enabled) {
        _client.popupBlockerEnabled = enabled
    }

    boolean getPopupBlockerEnabled() {
        _client.popupBlockerEnabled
    }

    /**
     * Set up our magic on the HtmlUnit classes
     */
    static initVirtualMethods() {
        HtmlPage.metaClass.getForms = { ->
            new FormsWrapper(delegate)
        }
        HtmlForm.metaClass.getFields = { ->
            new FieldsWrapper(delegate)
        }
        HtmlForm.metaClass.getSelects = { ->
            new SelectsWrapper(delegate)
        }
        HtmlForm.metaClass.getRadioButtons = { ->
            new RadioButtonsWrapper(delegate)
        }
        HtmlInput.metaClass.setValue = { value ->
            System.out.println("Setting value to [$value] on field [name:${delegate.nameAttribute} id:${delegate.id}] of form [${delegate.enclosingForm?.nameAttribute}]") 
            delegate.valueAttribute = value
        }
        HtmlInput.metaClass.getValue = { ->
            return delegate.valueAttribute
        }
        HtmlTextArea.metaClass.setValue = { value ->
            System.out.println("Setting value to [$value] on text area [name:${delegate.nameAttribute} id:${delegate.id}] of form [${delegate.enclosingForm?.nameAttribute}]") 
            delegate.text = value
        }
        HtmlTextArea.metaClass.getValue = { ->
            return delegate.text
        }
        HtmlSelect.metaClass.select = { value ->
            System.out.println("Selecting option [$value] on select field [name:${delegate.nameAttribute} id:${delegate.id}] of form [${delegate.enclosingForm?.nameAttribute}]") 
            delegate.setSelectedAttribute(value?.toString(), true)
        }
        HtmlSelect.metaClass.deselect = { value ->
            delegate.setSelectedAttribute(value?.toString(), false)
        }
        HtmlSelect.metaClass.getSelected = { ->
            return delegate.getSelectedOptions()?.collect { it.valueAttribute }
        }
    }

    void clientChanged() {
        mainWindow = _client?.currentWindow        
    }
    
    int getResponseStatus() {
        response.statusCode
    }
    
    String getRedirectURL() {
        response.redirectUrl
    }
    
    String getResponseContentType() {
        response.contentType
    }
    
    
    void nodeAdded(DomChangeEvent event) {
        System.out.println "Added DOM node [${nodeToString(event.changedNode)}] to parent [${nodeToString(event.parentNode)}]"
    }

    void nodeDeleted(DomChangeEvent event) {
        System.out.println "Removed DOM node [${nodeToString(event.changedNode)}] from parent [${nodeToString(event.parentNode)}]"
    }
    
    void attributeAdded(HtmlAttributeChangeEvent event) {
        def tag = event.htmlElement.tagName
        def name = event.htmlElement.attributes.getNamedItem('name')
        def id = event.htmlElement.attributes.getNamedItem('id')
        System.out.println "Added attribute ${event.name} with value ${event.value} to tag [${tag}] (id: $id / name: $name)"
    }

    void attributeRemoved(HtmlAttributeChangeEvent event) {
        def tag = event.htmlElement.tagName
        def name = event.htmlElement.attributes.getNamedItem('name')
        def id = event.htmlElement.attributes.getNamedItem('id')
        System.out.println "Removed attribute ${event.name} from tag [${tag}] (id: $id / name: $name)"
    }

    void attributeReplaced(HtmlAttributeChangeEvent event)  {
        def tag = event.htmlElement.tagName
        def name = event.htmlElement.attributes.getNamedItem('name')
        def id = event.htmlElement.attributes.getNamedItem('id')
        System.out.println "Changed attribute ${event.name} to ${event.value} on tag [${tag}] (id: $id / name: $name)"
    }
                        
    void webWindowClosed(WebWindowEvent event) {
        
    }
    
    void webWindowContentChanged(WebWindowEvent event) {
        System.out.println "Content of web window [${event.webWindow}] changed"
        if (event.webWindow == mainWindow) {
            _page = event.newPage
            def response = _page.webResponse
            newResponseReceived(response)
            listener.contentChanged( new ContentChangedEvent(
                    url: response.requestSettings.url,
                    method: response.requestSettings.httpMethod,
                    eventSource: 'webWindowContentChange event',
                    statusCode: response.statusCode) )
        } else {
            System.out.println "New content of web window [${event.webWindow}] was not for main window, ignoring"
        }
    }
    
    void webWindowOpened(WebWindowEvent event) {
        // @todo we need to think how to handle multiple windows
    }

    protected newResponseReceived(response) {
        System.out.println("${'<'*20} Received response from ${response.requestMethod} ${response.requestUrl} ${'<'*20}")
        if (HTTPUtils.isRedirectStatus(response.statusCode)) {
            redirectUrl = response.getResponseHeaderValue('Location')
            System.out.println("Response was a redirect to ${redirectUrl} ${'<'*20}")
        } else {
            redirectUrl = null
        }
        dumpResponseHeaders(response)
        System.out.println("Content")
        System.out.println('='*40)
        System.out.println(response.contentAsString)
        System.out.println('='*40)
        System.out.println('')
        response = response
    }

    String getCurrentURL() {
        response?.requestSettings?.url        
    }
    
    String followRedirect() {
        if (redirectUrl) {
            get(u)
            return u
        } else {
            return null
        }
    }
    
    void request(URL url, String method, Closure paramSetupClosure) {
        settings = new WebRequestSettings(url)
        settings.httpMethod = HttpMethod.valueOf(method)
        
        if (paramSetupClosure) {
            def wrapper = new RequestBuilder(settings)
            paramSetupClosure.delegate = wrapper
            paramSetupClosure.call()
            
            wrapper.@headers.each { entry ->
                settings.addAdditionalHeader(entry.key, entry.value.toString())
            }

            if (wrapper.@reqParameters) {
                def params = []
                wrapper.@reqParameters.each { pair ->
                    params << new NameValuePair(pair[0], pair[1].toString()) 
                }
                settings.requestParameters = params
            }
        }
        
        dumpRequestInfo(settings)

        response = _client.loadWebResponse(settings)
        _page = _client.loadWebResponseInto(response, mainWindow)
        
        // By this time the events will have been triggered
    } 

    Map getRequestHeaders() {
        reqSettings?.additionalHeaders
    }

    Map getRequestParameters() {
        reqSettings?.requestParameters   
    }

    String getResponseAsString() {
        response.contentAsString
    }
    
    def getResponseDOM() {
        
    }
    
    String getResponseHeader(String name) {
        response.getResponseHeaderValue(name)
    }
    
    Map getResponseHeaders() {
        response.responseHeaders
    }    

    protected dumpRequestInfo(reqSettings) {
        System.out.println("Request parameters:")
        System.out.println('='*40)
        reqSettings?.requestParameters?.each {
            System.out.println( "${it.name}: ${it.value}")
        }
        System.out.println('='*40)
        System.out.println("Request headers:")
        System.out.println('='*40)
        reqSettings?.additionalHeaders?.each {Map.Entry it ->
            System.out.println("${it.key}: ${it.value}")
        }
        System.out.println('='*40)
    }
    
    protected dumpResponseHeaders(response) {
        System.out.println("Response was ${response.statusCode} '${response.statusMessage}', headers:")
        System.out.println('='*40)
        response?.responseHeaders?.each {
            System.out.println( "${it.name}: ${it.value}")
        }
        System.out.println('='*40)
    }
    
    String nodeToString(def n) {
        "[${n?.nodeName}] with value [${n?.nodeValue}] and "+
            "id [${n?.attributes?.getNamedItem('id')?.nodeValue}], name [${n?.attributes?.getNamedItem('name')?.nodeValue}]"
    }
    

}