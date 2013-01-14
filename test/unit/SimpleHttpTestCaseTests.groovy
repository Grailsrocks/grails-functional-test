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

import functionaltestplugin.FunctionalTestCase
import functionaltestplugin.TestingUtil
import groovy.mock.interceptor.StubFor

import com.gargoylesoftware.htmlunit.HttpMethod
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.util.NameValuePair

class SimpleHttpTestCaseTests extends GroovyTestCase {

    void testGetParamsAssignment() {

        def mockClient = new StubFor(WebClient)
        mockClient.demand.asBoolean {-> true }
        mockClient.demand.addWebWindowListener { }
        mockClient.demand.setRedirectEnabled { }
        mockClient.demand.setPopupBlockerEnabled { }
        mockClient.demand.setJavaScriptEnabled { }
        mockClient.demand.setPageCreator { }
        mockClient.demand.setThrowExceptionOnFailingStatusCode { }
        mockClient.demand.loadWebResponse { settings ->
            def params = settings.requestParameters
            assertEquals 2, params.size()
            assertEquals HttpMethod.valueOf('GET'), settings.httpMethod
            assertTrue params.indexOf(new NameValuePair('queryId', '5')) >= 0
            assertTrue params.indexOf(new NameValuePair('userName', 'Marc')) >= 0
            return [:]
        }
        mockClient.demand.getCurrentWindow {->
        }
        mockClient.demand.loadWebResponseInto {resp, window ->
        }

        mockClient.use {
            def tester = new FunctionalTestCase()

            tester.setUp()

            tester.get("http://localhost:8080") {
                queryId = 5
                userName = 'Marc'
            }
        }
    }

    void testURLsWithBaseNoContext() {
        doURLTests("http://localhost:8080/")
    }

    void testURLsWithBaseWithContext() {
        doURLTests("http://localhost:8080/yourapplication/")
    }

    void doURLTests(base) {
        def tester = new FunctionalTestCase()

        tester.setUp()
        tester.baseURL = base

        def p = [:]
        assertEquals base + 'index.gsp', tester.makeRequestURL('index.gsp').toString()
        assertEquals base + 'index.gsp', tester.makeRequestURL('/index.gsp').toString()
        assertEquals base + 'controller/action',
                tester.makeRequestURL('/controller/action').toString()
        tester.baseURL = base + 'controller/action'
        assertEquals base + 'controller/something',
                tester.makeRequestURL('something').toString()

    }

    void testGetParamsInvocation() {

        def mockClient = new StubFor(WebClient)
        mockClient.demand.asBoolean {-> true }
        mockClient.demand.addWebWindowListener(1) { }
        mockClient.demand.setRedirectEnabled(1) { }
        mockClient.demand.setPopupBlockerEnabled(1) { }
        mockClient.demand.setJavaScriptEnabled(1) { }
        mockClient.demand.setPageCreator(1) { }
        mockClient.demand.setThrowExceptionOnFailingStatusCode(1) { }
        mockClient.demand.loadWebResponse(1) {settings ->
            def params = settings.requestParameters
            assertEquals 2, params.size()
            assertEquals HttpMethod.valueOf('GET'), settings.httpMethod
            assertTrue params.indexOf(new NameValuePair('queryId', '5')) >= 0
            assertTrue params.indexOf(new NameValuePair('userName', 'Marc')) >= 0
            return [:]
        }
        mockClient.demand.getCurrentWindow(1) {->
        }
        mockClient.demand.loadWebResponseInto(1) {resp, window ->
        }

        def tester = new FunctionalTestCase()

        mockClient.use {
            tester.setUp()

            tester.get("http://localhost:8080") {
                queryId = 5
                userName = 'Marc'
            }
        }
    }

    void testPostParams() {

        def mockClient = new StubFor(WebClient)
        mockClient.demand.asBoolean {-> true }
        mockClient.demand.addWebWindowListener(1) { }
        mockClient.demand.setRedirectEnabled(1) { }
        mockClient.demand.setPopupBlockerEnabled(1) { }
        mockClient.demand.setJavaScriptEnabled(1) { }
        mockClient.demand.setPageCreator(1) { }
        mockClient.demand.setThrowExceptionOnFailingStatusCode(1) { }
        mockClient.demand.loadWebResponse(1) {settings ->
            def params = settings.requestParameters
            assertEquals 2, params.size()
            assertEquals HttpMethod.valueOf('POST'), settings.httpMethod
            assertTrue params.indexOf(new NameValuePair('queryId', '8')) >= 0
            assertTrue params.indexOf(new NameValuePair('userName', 'Simon')) >= 0
            return [:]
        }
        mockClient.demand.getCurrentWindow(1) {->
        }
        mockClient.demand.loadWebResponseInto(1) {resp, window ->
        }

        def tester = new FunctionalTestCase()

        mockClient.use {
            tester.setUp()

            tester.post("http://localhost:8080") {
                queryId = 8
                userName = 'Simon'
            }
        }
    }

    void testPostBody() {

        def mockClient = new StubFor(WebClient)
        mockClient.demand.asBoolean {-> true }
        mockClient.demand.addWebWindowListener(1) { }
        mockClient.demand.setRedirectEnabled(1) { }
        mockClient.demand.setPopupBlockerEnabled(1) { }
        mockClient.demand.setJavaScriptEnabled(1) { }
        mockClient.demand.setPageCreator(1) { }
        mockClient.demand.setThrowExceptionOnFailingStatusCode(1) { }
        mockClient.demand.loadWebResponse(1) {settings ->
            assertEquals HttpMethod.valueOf('POST'), settings.httpMethod
            assertEquals "q=grails", settings.requestBody
            return [:]
        }
        mockClient.demand.getCurrentWindow(1) {->
        }
        mockClient.demand.loadWebResponseInto(1) {resp, window ->
        }

        def tester = new FunctionalTestCase()

        mockClient.use {
            tester.setUp()

            tester.post("http://localhost:8080") {
                body { "q=grails" }
            }
        }
    }


/* Commented out because it seems nekohtml fails to parse out name=f without quotes
    void testGoogleSearchNamedSubmitField() {
        def code = {
            redirectEnabled = true
            javaScriptEnabled = false
            get 'http://google.com'

            assertStatus 200

            form('f') {
                q "grails"
                btnG.click()
            }

            assertStatus 200
        }
        TestingUtil.runTestScript(code)
    }
*/

    void testJQuery() {
        def code = {
            get 'file:test/resources/jquerytest.html'

            assertStatus 200
            assertContentContains "Hello"

            Thread.sleep(3000)

            assertElementTextContains 'message', "World"
        }
        TestingUtil.runTestScript(code)
    }


    void testById() {
        def code = {
            get 'file:test/resources/domtests.html'

            assertStatus 200

            assertEquals "idnodetext", byId('idnode').textContent
        }
        TestingUtil.runTestScript(code)
    }

    void testFormsAccess() {
        def code = {
            get 'file:test/resources/formtests.html'

            assertStatus 200

            def f = form('formName1') { }
            assertNotNull f
            assertEquals "formName1", f.nameAttribute

            f = form('formId1') { }
            assertNotNull f
            assertEquals "formId1", f.id

            f = form() { }
            assertNotNull f
            assertEquals "formName1", f.nameAttribute
        }
        TestingUtil.runTestScript(code)
    }

    void testByName() {
        def code = {
            get 'file:test/resources/domtests.html'

            assertStatus 200

            assertEquals "namenodetext", byName('namenode').textContent

            def nodes = byName('multinamenode')
            assertEquals 2, nodes.size()
            assertNotNull nodes.find { it.textContent == "multinamenodetext1" }
            assertNotNull nodes.find { it.textContent == "multinamenodetext2" }

            assertNull byName("nosuchname")
        }
        TestingUtil.runTestScript(code)
    }

    void testByClass() {
        def code = {
            get 'file:test/resources/domtests.html'

            assertStatus 200

            assertEquals "classnodetext", byClass('classnode').textContent
            def nodes = byClass('multiclassnode')
            assertEquals 2, nodes.size()
            assertNotNull nodes.find { it.textContent == "multiclassnodetext1" }
            assertNotNull nodes.find { it.textContent == "multiclassnodetext2" }

            assertNull byClass("nosuchclass")
        }
        TestingUtil.runTestScript(code)
    }

    void testByXPath() {
        def code = {
            get 'file:test/resources/domtests.html'

            assertStatus 200

            assertEquals "classnodetext", byXPath("//*[@class='classnode']").textContent
            def nodes = byXPath("//*[@class='multiclassnode']")
            assertEquals 2, nodes.size()
            assertNotNull nodes.find { it.textContent == "multiclassnodetext1" }
            assertNotNull nodes.find { it.textContent == "multiclassnodetext2" }

            assertNull byXPath("//*[@class='nosuchclass']")
        }
        TestingUtil.runTestScript(code)
    }

    void testTwoNamedClients() {
        def code = {
            client "A"

            get 'file:test/resources/a.html'

            assertStatus 200
            assertContentContains "File A"

            client "B"

            get 'file:test/resources/b.html'

            assertStatus 200
            assertContentContains "File B"

            client "A"

            assertContentContains "File A"
        }
        TestingUtil.runTestScript(code)
    }

/*
    void testWaitForJS() {
        def code = {
            get 'file:test/resources/bgjs.html'

            assertContentDoesNotContain 'COMPLETE'
            waitForScripts(5000)
            assertElementTextContains 'result', 'COMPLETE'
        }
        TestingUtil.runTestScript(code)
    }
*/
    void testBackWithOnlyRedirect() {
/*        def tester = new FunctionalTestCase()
        tester.setUp()
        tester.get('http://www.twitter.com')
        try {
            tester.back()
        } catch (FunctionalTestException re) {
            assertEquals('Unable to find a non-redirect URL in the history', re.message)
        }
*/    }

    void testCustomHeaders() {
/*        def tester = new FunctionalTestCase()
        tester.setUp()

        tester.get('http://twitter.com') {
            headers['some-custom-header'] = 'blah'
        }
*/    }

    void testMeta(){
/*        def tester = new FunctionalTestCase()
        tester.setUp()
        tester.get('http://twitter.com')
        tester.assertMeta('description')
        tester.assertMetaContains('description','Twitter')
*/    }

    void testXmlPages() {
/*        def tester = new FunctionalTestCase()
        tester.setUp()

        tester.get('http://twitter.com/statuses/public_timeline.xml')
        tester.assertContentContains '<statuses'
        tester.get('user_timeline/leebutts.xml')
        tester.assertContentContains '<statuses'
*/
    }

    void testRegexExtract() {
        def tester = new FunctionalTestCase()
        tester.setUp()

        tester.get('file:test/resources/a.html')
        assertEquals "View B", tester.extract(/<a.*>(.+)<\/a>/)
    }

    void testBack() {
/*        def tester = new FunctionalTestCase()
        tester.setUp()

        try {
            tester.back()
        } catch (FunctionalTestException re) {
            assertEquals('You cannot call back() without first generating at least 2 responses', re.message)
        }

        tester.get('http://twitter.com')
        try {
            tester.back()
        } catch (FunctionalTestException re) {
            assertEquals('You cannot call back() without first generating at least 2 responses', re.message)
        }
        tester.assertTitle 'Twitter: What are you doing?'

        tester.click "Search"

        tester.assertTitle "Twitter Search"
        tester.back()

        tester.assertTitle 'Twitter: What are you doing?'
        tester.click "Search"

        tester.assertTitle "Twitter Search"
        tester.click 'About Twitter Search'
        tester.assertTitle 'About Twitter Search'

        tester.back()
        tester.back()
        tester.assertTitle 'Twitter: What are you doing?'

*/    }

    /*
         Hmm how to mock HtmlPage which has no default ctor?
     void testFindingForms() {
         def mockPage = new StubFor(HtmlPage)
         mockPage.demand.getElementById(1) { id ->
             throw new ElementNotFoundException("I'm pretending I can't find $id")
         }
         mockPage.demand.getFormByName(1) { name ->
             [:]
         }

         def mockClient = new StubFor(WebClient)
         mockClient.demand.addWebWindowListener(1) { }
         mockClient.demand.setRedirectEnabled(1) { }
         mockClient.demand.setThrowExceptionOnFailingStatusCode(1) { }
         mockClient.demand.loadWebResponse(1) { settings -> new HtmlPage() }

         def tester = new FunctionalTestCase()

         mockClient.use {
             mockPage.use {
                 tester.setUp()

                 tester.get("http://search.twitter.com")

                 assertNotNull tester.form("Search")
             }
         }
     }
     */
}
