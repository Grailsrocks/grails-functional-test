package com.grailsrocks.functionaltest.client

import org.codehaus.groovy.grails.plugins.codecs.Base64Codec

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.HttpMethod
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebRequest
import com.gargoylesoftware.htmlunit.WebWindowEvent
import com.gargoylesoftware.htmlunit.WebWindowListener
import com.gargoylesoftware.htmlunit.html.DomChangeEvent
import com.gargoylesoftware.htmlunit.html.DomChangeListener
import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeEvent
import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeListener
import com.gargoylesoftware.htmlunit.html.HtmlForm
import com.gargoylesoftware.htmlunit.html.HtmlInput
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlSelect
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import com.gargoylesoftware.htmlunit.util.NameValuePair
import com.grailsrocks.functionaltest.client.htmlunit.*
import com.grailsrocks.functionaltest.dsl.RequestBuilder

class BrowserClient implements Client, WebWindowListener, HtmlAttributeChangeListener, DomChangeListener {
    WebRequest settings

    def interceptingPageCreator = new InterceptingPageCreator(this)

    def _client
    def mainWindow
    def browser
    def response
    def _page
    Map stickyHeaders = [:]
    def currentAuthInfo
    def currentURL

    ClientAdapter listener

    BrowserClient(ClientAdapter listener) {
        this.listener = listener
        _client = browser ? new WebClient(BrowserVersion[browser]) : new WebClient()
        _client.addWebWindowListener(this)
        _client.redirectEnabled = false // We're going to handle this thanks very much
        _client.popupBlockerEnabled = true
        _client.javaScriptEnabled = true
        _client.throwExceptionOnFailingStatusCode = false
        _client.pageCreator = interceptingPageCreator
    }

    String setAuth(type, user, credentials) {
        currentAuthInfo = [type:type, user:user, credentials:credentials]
    }

    void clearAuth() {
        currentAuthInfo = null
    }

    void clearStickyHeader(String header) {
        stickyHeaders.remove(header)
    }

    String setStickyHeader(String header, String value) {
        stickyHeaders[header] = value
    }

    boolean getJavaScriptEnabled() {
        _client.javaScriptEnabled
    }

    void setJavaScriptEnabled(boolean enabled) {
        _client.javaScriptEnabled = enabled
    }

    boolean getScriptErrorsEnabled() {
        _client.throwExceptionOnScriptError
    }

    void setScriptErrorsEnabled(boolean enabled) {
        _client.throwExceptionOnScriptError = enabled
    }

    void setPopupBlockerEnabled(boolean enabled) {
        _client.popupBlockerEnabled = enabled
    }

    boolean getPopupBlockerEnabled() {
        _client.popupBlockerEnabled
    }

    String getRequestBody() {
        if (settings?.requestBody != null) {
            return settings.requestBody
        } else {
            return ''
        }
    }

    /**
     * Set up our magic on the HtmlUnit classes
     */
    static initVirtualMethods(ClientAdapter listener) {
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
            listener.printlnToTestReport "Setting value to [$value] on field [name:${delegate.nameAttribute} id:${delegate.id}] of form [${delegate.enclosingForm?.nameAttribute}]"
            delegate.valueAttribute = value
        }
        HtmlInput.metaClass.getValue = { ->
            return delegate.valueAttribute
        }
        HtmlTextArea.metaClass.setValue = { value ->
            listener.printlnToTestReport "Setting value to [$value] on text area [name:${delegate.nameAttribute} id:${delegate.id}] of form [${delegate.enclosingForm?.nameAttribute}]"
            delegate.text = value
        }
        HtmlTextArea.metaClass.getValue = { ->
            return delegate.text
        }
        HtmlSelect.metaClass.select = { value ->
            listener.printlnToTestReport "Selecting option [$value] on select field [name:${delegate.nameAttribute} id:${delegate.id}] of form [${delegate.enclosingForm?.nameAttribute}]"
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
    }

    int getResponseStatus() {
        response?.statusCode
    }

    String getResponseStatusMessage() {
        response?.statusMessage
    }

    String getResponseContentType() {
        response?.contentType
    }

    void nodeAdded(DomChangeEvent event) {
        listener.printlnToTestReport "DOM: Added node [${nodeToString(event.changedNode)}] to parent [${nodeToString(event.parentNode)}]"
    }

    void nodeDeleted(DomChangeEvent event) {
        listener.printlnToTestReport "DOM: Removed node [${nodeToString(event.changedNode)}] from parent [${nodeToString(event.parentNode)}]"
    }

    void attributeAdded(HtmlAttributeChangeEvent event) {
        def tag = event.htmlElement.tagName
        def name = event.htmlElement.attributes.getNamedItem('name')
        def id = event.htmlElement.attributes.getNamedItem('id')
        listener.printlnToTestReport "DOM: Added attribute ${event.name} with value ${event.value} to tag [${tag}] (id: $id / name: $name)"
    }

    void attributeRemoved(HtmlAttributeChangeEvent event) {
        def tag = event.htmlElement.tagName
        def name = event.htmlElement.attributes.getNamedItem('name')
        def id = event.htmlElement.attributes.getNamedItem('id')
        listener.printlnToTestReport "DOM: Removed attribute ${event.name} from tag [${tag}] (id: $id / name: $name)"
    }

    void attributeReplaced(HtmlAttributeChangeEvent event)  {
        def tag = event.htmlElement.tagName
        def name = event.htmlElement.attributes.getNamedItem('name')
        def id = event.htmlElement.attributes.getNamedItem('id')
        listener.printlnToTestReport "DOM: Changed attribute ${event.name} to ${event.value} on tag [${tag}] (id: $id / name: $name)"
    }

    void webWindowClosed(WebWindowEvent event) {
        listener.printlnToTestReport "Web window [${event?.webWindow}] closed"
    }

    void webWindowContentChanged(WebWindowEvent event) {
        listener.printlnToTestReport "Content of web window [${event?.webWindow}] changed"
        if (event?.webWindow == mainWindow) {
            _page = event.newPage
            response = _page.webResponse

            listener.contentChanged( new ContentChangedEvent(
                    client: this,
                    url: response.webRequest.url,
                    method: response.webRequest.httpMethod,
                    eventSource: 'Browser content change',
                    statusCode: response.statusCode) )
        } else {
            listener.printlnToTestReport "New content of web window [${event?.webWindow}] was not for main window, ignoring"
        }
    }

    void webWindowOpened(WebWindowEvent event) {
        listener.printlnToTestReport "Web window [${event?.webWindow}] opened"
    }

    String getRequestMethod() {
        settings.httpMethod
    }

    String getCurrentURL() {
        currentURL
    }

    void request(URL url, String method, Closure paramSetupClosure) {
        settings = new WebRequest(url)
        settings.httpMethod = HttpMethod.valueOf(method)
        response = null
        currentURL = url

        if (currentAuthInfo) {
            // @todo We could use htmlunit auth stuff here?
            def encoded = Base64Codec.encode("${currentAuthInfo.user}:${currentAuthInfo.credentials}".getBytes('utf-8'))
            settings.setAdditionalHeader('Authorization', "Basic "+encoded)
        }

        def wrapper
        if (paramSetupClosure) {
            def builder = new RequestBuilder(settings)
            wrapper = builder.build(paramSetupClosure)
        }

        def headerLists = [stickyHeaders]
        if (wrapper) {
            headerLists << wrapper.headers
        }
        headerLists.each { headers ->
            for (entry in headers) {
                settings.setAdditionalHeader(entry.key, entry.value.toString())
            }
        }

        if (wrapper?.reqParameters) {
            def params = []
            wrapper.reqParameters.each { pair ->
                params << new NameValuePair(pair[0], pair[1].toString())
            }
            settings.requestParameters = params
        }

        if (wrapper?.body) {
            settings.requestBody = wrapper.body
        }

        listener.requestSent(this)

        mainWindow = _client?.currentWindow
        response = _client.loadWebResponse(settings)
        _client.loadWebResponseInto(response, mainWindow)

        // By this time the events will have been triggered
    }

    Map getRequestHeaders() {
        settings?.additionalHeaders
    }

    Map getRequestParameters() {
        def r = [:]
        for (p in settings?.requestParameters) {
            r[p.name] = p.value
        }
    }

    String getResponseAsString() {
        response.contentAsString != null ? response.contentAsString : ''
    }

    String getResponseHeader(String name) {
        response.getResponseHeaderValue(name)
    }

    Map getResponseHeaders() {
        def r = [:]
        for (p in response?.responseHeaders) {
            r[p.name] = p.value
        }
    }

    String nodeToString(def n) {
        "[${n?.nodeName}] with value [${n?.nodeValue}] and "+
            "id [${n?.attributes?.getNamedItem('id')?.nodeValue}], name [${n?.attributes?.getNamedItem('name')?.nodeValue}]"
    }
}
