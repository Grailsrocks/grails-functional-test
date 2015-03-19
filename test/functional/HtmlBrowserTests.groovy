import com.grailsrocks.functionaltest.BrowserTestCase;

public class HtmlBrowserTests extends BrowserTestCase {

    void testGetHtml() {
        get('/selfTest/minimalHtml')

        assertStatus(200)
    }

    void testGetNonExistingPageReturns404() {
        get('/selfTest/thisUriDoesNotExist')

        assertStatus(404)

        shouldFail(junit.framework.AssertionFailedError) {
            assertStatus(200)
        }
    }

    void testContentAssertions() {
        get('/selfTest/minimalHtml')

        assertContent("<html><body>this is a minimal html body</body></html>")
        assertContentContains("ThisIsA")  // assertContentContains does a loosely compare (case insensitive and all whitespace ignored)
        assertContentContainsStrict("this is a minimal html body")

        shouldFail(junit.framework.ComparisonFailure) {
            assertContent("this is a minimal html body")  // assertContent compares _all_ content
        }
    }

    void testGetWithParametersClosure() {
        get('/selfTest/paramecho') {
            userName "marc"
            email "marc@somewhere.com"
        }

        assertContentContains 'username:marc'
        assertContentContains 'email:marc@somewhere.com'
    }

    void testGetWithParameters() {
        get('/selfTest/paramecho') {
            userName = "marc"
            email = "marc@somewhere.com"
        }

        assertContentContains 'username:marc'
        assertContentContains 'email:marc@somewhere.com'
    }

    void testGetWithHeaders() {
        get('/selfTest/echoHeader') {
            headers['X-custom'] = 'foobar'
        }

        assertContent('custom header: foobar')
    }

    void testResponseHeaders() {
        get('/selfTest/info')

        // Note that response headers are currently not printed in the test output!
        assertHeader('about', 'functionaltest plugin')
        assertHeaderStrict('Transfer-Encoding', 'chunked')
        assertHeaderStrict('Server', 'Apache-Coyote/1.1')
        assertHeaderStrict('Content-Type', 'text/html;charset=utf-8')
        assertHeaderContains('Date', new Date().format("EEE, d MMM yyyy"))
        assertHeaderContainsStrict('Date', new Date().format("EEE, d MMM yyyy"))  // GPFUNCTIONALTEST-128
    }

    void testContentType() {
        get('/selfTest/minimalHtml')

        assertHeaderStrict('Content-Type', 'text/html;charset=utf-8')
        assertContentType('text/html')
        assertContentTypeStrict('text/html')
        shouldFail(junit.framework.AssertionFailedError) {
            assertContentTypeStrict('text/html;charset=utf-8')  // assertContentType only checks the type and subtype, not the charset parameter.
        }
    }

    void testAssertTitle() {
        get('/selfTest/info')

        assertTitle("info page")
        assertTitleContains('info')
        // there is no assertTitleStrict...
    }

    void testAssertMetaWithNameAndContent() {
        get('/selfTest/info')

        assertMeta('keywords')  // not documented...
        assertMetaContains('keywords', 'JavaScript')
        // assertMeta('keywords', 'HTML,CSS,JavaScript') => is documented, but not implemented
    }

    void testPost() {
        post('/selfTest/returnHttpMethodUsed')

        assertContentContains('method used: POST')
    }

    void testPostWithBody() {
        post('/selfTest/returnRequestBody') {
            body {
                headers['Content-Type'] = 'application/octet-stream'    // needed to overwrite content-type application/x-www-form-urlencoded
                """<cart><item id="3"><title>Xenosapien</title><artist>Cephalic Carnage</artist></item></cart>"""
            }
        }

        assertContentContains('<pre id="post-data"><cart><item id="3"><title>Xenosapien</title><artist>Cephalic Carnage</artist></item></cart></pre>')
    }

    void testPut() {
        put('/selfTest/returnHttpMethodUsed')

        assertContentContains('method used: PUT')
    }

    void testPutWithBody() {
        put('/selfTest/returnRequestBody') {
            body {
                headers['Content-Type'] = 'application/octet-stream'    // needed to overwrite content-type application/x-www-form-urlencoded
                """<cart><item id="3"><title>Xenosapien</title><artist>Cephalic Carnage</artist></item></cart>"""
            }
        }

        assertContentContains('<pre id="post-data"><cart><item id="3"><title>Xenosapien</title><artist>Cephalic Carnage</artist></item></cart></pre>')
    }

    void testDelete() {
        delete('/selfTest/returnHttpMethodUsed')

        assertContentContains('method used: DELETE')
    }

    void testElementText() {
        get('/selfTest/sampleHtml')

        assertElementTextContains('content-div', 'sorry')
        shouldFail(junit.framework.AssertionFailedError) {
            assertElementTextContainsStrict('content-div', 'sorry')
        }
        shouldFail(junit.framework.AssertionFailedError) {
            assertElementTextContains('content-div', 'div')
        }
    }

    void testGetElementById() {
        get('/selfTest/sampleHtml')

        def contentDiv = byId("content-div")

        assertNotNull(contentDiv)
        assertTrue(contentDiv.isBlock())

        assertNull(byId("no-such-named-element"))
    }

    void testGetElementByName() {
        get('/selfTest/sampleHtml')

        def menuElement = byName("menu")

        assertNotNull(menuElement)
        assertEquals("ul", menuElement.getNodeName())

        assertNull(byName("no such named element"))
    }

    void testClickOnLink() {
        get('/selfTest/sampleHtml')

        click('show min')  // click on link that refers to 'minimalHtml' page

        assertContent("<html><body>this is a minimal html body</body></html>")
    }

    void testFormSubmit() {
        get('/selfTest/withForm')

        form() {
            input1 = "5"
            input2 = "8"
            click "Submit"
        }

        def output = byName('output')
        assertEquals("13", output.text)
    }

    void testFormSecondSubmit() {
        get('/selfTest/withForm')

        form('form1') {
            input1 = "5"
            input2 = "8"
            Clear.click()
        }

        assertEquals("", byName('input1').text)
        assertEquals("", byName('input2').text)
        assertEquals("", byName('output').text)
    }

    void testFormWithPostOnly() {
        get('/selfTest/formWithPostOnly')  // Use get to retrieve the form itself...

        form() {
            input1 = "5"
            input2 = "8"
            click "Submit"                 // but submit with a POST (form action method is POST)
        }
        assertContentContains("5+8")
    }

    void testFollowRedirect() {
        get('/selfTest/sendRedirect')  // Redirects to minimalHtml action

        assertContent("<html><body>this is a minimal html body</body></html>")
    }

    void testRedirect() {
        redirectEnabled = false
        get('/selfTest/sendRedirect')

        assertStatus(302)
        assertRedirectUrl('http://localhost:8080/FunctionalTest/selfTest/minimalHtml')
        assertRedirectUrlContains('/selfTest/minimalHtml')
    }

    void testCookies() {
        get('/selfTest/setCookie?cookieName=myCookie&cookieValue=Cookie%20Monster&cookieAge=200')

        assertCookieExists('myCookie')
        assertCookieContains('myCookie', 'cookiemonster')
        shouldFail(junit.framework.AssertionFailedError) {
            assertCookieContainsStrict('myCookie', 'cookiemonster')
        }
        assertCookieContainsStrict('myCookie', 'Cookie Monster')
        shouldFail(junit.framework.AssertionFailedError) {
            assertCookieExists('foo')
        }
    }

    void testCookiesDisabled() {
        cookiesEnabled = false
        get('/selfTest/setCookies?cookieName=myCookie&cookieValue=Cookie%20Monster&cookieAge=200')

        shouldFail(junit.framework.AssertionFailedError) {
            assertCookieExists('myCookie')
        }
    }

    void testKeepCookies() {
        get('/selfTest/setCookie?cookieName=myCookie&cookieValue=Cookie%20Monster&cookieAge=200')
        get('/selfTest/echoCookies')
        assertContentContains('myCookie=Cookie Monster')
    }

    void testCookieExpires() {
        get('/selfTest/setCookie?cookieName=myCookie&cookieValue=Cookie%20Monster&cookieAge=1')
        get('/selfTest/echoCookies')
        assertContentContains('myCookie=Cookie Monster')
        sleep(1000)
        get('/selfTest/echoCookies')

        shouldFail(junit.framework.AssertionFailedError) {
            assertContentContains('myCookie=Cookie Monster')
        }
    }

    void testGetCookies() {
        get('/selfTest/setCookie?cookieName=myCookie&cookieValue=Cookie%20Monster&cookieAge=200')
        get('/selfTest/setCookie?cookieName=2ndCookie&cookieValue=wafel&cookieAge=200')

        assertEquals(2, cookies.size())
        assertEquals('"Cookie Monster"', cookies.find { it.name == 'myCookie' }.value)
        assertEquals('wafel', cookies.find { it.name == '2ndCookie' }.value)
    }
}
