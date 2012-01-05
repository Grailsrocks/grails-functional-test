package com.grailsrocks.functionaltest.client

import com.gargoylesoftware.htmlunit.DefaultPageCreator
import com.gargoylesoftware.htmlunit.WebWindow
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.Page

class InterceptingPageCreator extends DefaultPageCreator {
    def client
    
    InterceptingPageCreator(BrowserClient client) {
        this.client = client
    }
    
    Page createPage(WebResponse webResponse, WebWindow webWindow)  {
        def p = super.createPage(webResponse,webWindow)
        if (p instanceof HtmlPage) {
            p.addDomChangeListener(client)
            p.addHtmlAttributeChangeListener(client)
        }
        return p
    }
}

