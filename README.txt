h1. Functional Testing Plugin

Author: Marc Palmer [http://www.anyware.co.uk]

These docs cover version 1.1

{note}
This plugin is licensed under the Apache License, Version 2.0.
The original code of this plugin was developed by Historic Futures Ltd. [http://www.historicfutures.com] and open sourced.
{note}

h2. Overview

This plugin provides really easy functional web testing within the existing
framework of JUnit testing under grails.

It is lightweight and leverages the HtmlUnit engine for simulating the client
browser - without requiring any specific browser installed.

This means you can:

* Do easy REST functional testing just by issuing GET/POST etc calls and then inspect the result
* Do stateful functional testing of websites, including DOM inspection
* Do any of these against your application that is automatically run locally from WAR under Jetty, or against any other URL for testing production sites

h2. Installation
{code}
Run: grails install-plugin functional-test
{code}
Done!

h2. Usage

1) Run: grails create-functional-test HelloWorld

2) Edit the generated <project>/test/functional/HelloWorldTests.groovy file

3) Add code to the test methods (see reference below)

Here's an example script:
{code}
class TwitterTests extends functionaltestplugin.FunctionalTestCase {
  void testSearch() {
    get('http://www.twitter.com')
		
    click "Search"
		
    assertStatus 200
    assertContentContains "search"
		
    form('searchForm') {
      q = "#grails"
      click "Search"
    }

    assertStatus 200
    assertContentContains "#grails"		
  }
}
{code}

4) Run: grails functional-tests
(Optionally specifying a single test name e.g. to run YourTests add Your)

5) View the output in test/reports


h2. Functional Testing Reference

The test class has dynamic methods added by the plugin to make it easy to request content, post content, and interact with forms and page elements.

URLs are resolved in the following ways:

# Absolute urls are treated "as is"
# URLs that do not begin with a / are relative to the last page retrieved, or if it is the first page retrieved, relative to the value of the system property "grails.functional.test.baseURL"
# URLs that begin with / are relative to the application, or the value of grails.functional.test.baseURL system property if it is set.

h3. property: cookiesEnabled

Controls whether or not cookies are allowed. Default is true. You can change this through the course of your test code.

h3. property: cookies

Returns all current cookies in a set. Properties on the cookies includ "domain", "name", "value", "secure", "version", "path" and "comment"

h3. property: javaScriptEnabled

Controls whether or not Javascript code will be executed. Default is true. You can change this through the course of your test code.

h3. property: redirectEnabled

Controls whether or not redirects are automatically followed. Default is true. You can change this through the course of your test code.

{note}
If you want to be able to call assertRedirectUrlXXXX methods and prove that a redirect takes place, you must set this to false
{note}

h3. property: page

This property is available at all times, once a page has been retrieved. It exposes the underlying HtmlUnit page object for more advanced manipulation.

You can also get direct access to any forms of the page:
{code}
void testSomething() {
    get('/mycontroller','myAction')

    assertNotNull page.forms['userDetails'] // Make sure form is there
}
{code}

h3. method: get(uri)

This will issue a GET request to the URI which can be relative to the last page retrieved in the test method, or absolute within the application, or a full remote URL starting with http:// or https://

An optional closure can be supplied that will enable you to attach parameters to the request:
{code}
void testSomething() {
  get('/mycontroller') {
    headers['X-something-special'] = 'A-value-here'

    // NOTE: you can use this "method call" approach or assignment x = y
    userName "marc"
    email "marc@somewhere.com"
  }

  assertContentContains "it worked"
}
{code}

h3. method: post(uri)

As get(uri) but using the HTTP POST method. Note that you for POST and PUT you will usually also want to specify a body:

{code}
void testSomething() {
  post('/mycontroller/save') {
	body {
		"""
<cart><item id="3"><title>Xenosapien</title><artist>Cephalic Carnage</artist></item></cart>
"""
	}
  }

  assertStatus 200
}
{code}

The body closure expects the closure result to be a string.

h3. method: put(uri)

As get(uri) but using the HTTP PUT method

h3. method: delete(uri)

As get(uri) but using the HTTP DELETE method

h3. method: click(idOrLinkText)

This will click a link in the currently retrieved page, locating the link by an id attribute value, or if not found, by the text of the link.

h3. method: followRedirect()

Follows the redirect URL specified in the last response. For use after calling assertRedirectUrl with redirectEnabled set to false.

h3. method: form(name)

Obtains a reference to a form with name attribute matching the name passed to the method. You can then set or query values of fields in the form:
{code}
void testSomething() {
  get('/mycontroller') {
    userName "marc"
    // NOTE: you can use this "method call" approach or assignment x = y
    email "marc@somewhere.com"
  }

  form("userDetails") {
    name = "Marc"
    email = "secret@hades.com"
    click "submit"
  }
	
  assertContentContains "form submitted"
}
{code}

The form object return lets you locate elements by the value of their name attribute, by invoking methods or setting properties. Depending on their type you can interact with them in different ways:

* Simple text fields such as inputs with type text, hidden, password etc can just be set or accessed as the value
* Checkable items - radio buttons and checkboxes - can just be set to true/false
* Selectable items - select boxes - can have their single selection get/set 

So for example, normal input fields can have their value attribute get/set when you access them:
{code}
form("userDetails") {
	name = "Marc"
	email = "secret@hades.com"
	screenName "the_unknown_guest"
	fields['convoluted.field.name'].value = "have to use setValue here"
}
{code}
Select fields can have the selected item(s) changes by calling select:
{code}
form("userDetails") {
	name = "Marc"
	country = "uk" // this is a select box!
	selects['currency.id'].select "GBP" // This is a select box retrieved explicitly
}
{code}
Checkboxes and radios are just set to true/false to change their checked status:
{code}
form("userDetails") {
	name = "Marc"
	agreedTsAndCs true
	click "submit"
}
{code}
You can also access groups of radiobuttons by the field name attribute, and set the selected radio button in one easy call
{code}
form("userDetails") {
	name = "Marc"
	radioButtons.typeOfService = "POWERUSER"
	click "submit"
}
{code}
The above will find the radioButton of name typeOfService and value "POWERUSER" and set it to checked.

To click a button or image input in a form, there is a synthetic method "click" method, as well as a click method on clickable elements:
{code}
form("userDetails") { 
	name = "Marc" 
	click "send" 
}
{code}
The above will find the clickable element in the form with name "send", or failing that with the value "send" and click it. If still nothing suitable is found, it will look for a button-type element with the *value* of the name specified. Alternatively you can do:
{code}
form("userDetails") {
	name = "Marc"
	send.click()
}
{code}
Note that you can have nested closures in the "form" closure, to denote nested field names with dot notation:
{code}
form("userDetails") {
	name "Marc"
	address {
		street "668 Rue des Mortes"
		country "The U.S. of A"
	}
	send.click()
}
{code}
The above would try to set the fields with names "address.street" and "address.country"

{note}
You can directly access fields of certain types using the "fields", "radiobuttons" and "selects" array properties.

Properties and methods that you can access on specific types of field:
# Text fields: "value" can be get or set
# Radio buttons: "value" can be get or set - the value of the currently checked item in the group
# Selects: "select(value)" and "deselect(value)" to change the selection. Property "selected" to get the list of selected values.
{note}

h3. method: byId(elementID)

Retrieves an element from the current page by its id attribute. Returns null if there is no such element.

h3. method: byName(elementName)

Retrieves an element from the current page by its name attribute. Returns null if there is no such element. Throws and exception if there are multiple elements in the page with the same name

h3. method: byXPath(xpathQuery)

Retrieves the first element from the current page matching the XPath query. Returns null if there is no such element.

h3. method: clearCache()

Call this if you want to force the clearing of the JS and CSS cache, which may be useful if for example you are dynamically generating CSS or JS code in your test.

h3. method: assertStatus <value>

Called to assert the numerical status code of the last response:
{code}
void testSomething() {
    get('/mycontroller','myAction') {
		userName "marc"
		// NOTE: you can use this "method call" approach or assignment x = y
		email "marc@somewhere.com"
    }
	
	assertStatus 403   // we're not logged in!
}
{code}

h3. method: assertContentContains <value>

Asserts that the content of the last response contains the text supplied, case insensitive and all whitespace ignored.
{code}
assertContentContains "user profile"
{code}

h3. method: assertContentContainsStrict <value>

Asserts that the content of the last response contains the text supplied, case sensitive, whitespace matching.
{code}
assertContentContainsStrict "User Profile"
{code}

h3. method: assertContent <value>

Asserts that the content of the last response equals the text supplied, case insensitive and all whitespace ignored.
{code}
assertContent "<response>ok</response>"
{code}

h3. method: assertContentStrict <value>

Asserts that the content of the last response equals the text supplied, case sensitive, whitespace matching.
{code}
assertContentStrict "<response>\nOK\n</response>\n"
{code}

h3. method: assertContentType <value>

Asserts that the content type of the last response starts with the supplied string, eg assertContentType "text/html" will pass even if there is an
encoding at the end.
{code}
assertContentType "text/html"
assertContentType "text/html; charset=utf-8"
{code}

h3. method: assertContentTypeStrict <value>

Asserts that the content type of the last response matches the supplied string, case and whitespace matching exactly
{code}
assertContentTypeStrict "text/html; charset=UTF-8"
{code}

h3. method: assertHeader <headername>, <value>

Asserts that a response header equals the expected content, case and whitespace ignored eg:
{code}
assertHeader "Cache-Control", "private, max-age="
{code}

h3. method: assertHeaderStrict <headername>, <value>

Asserts that a response header equals exactly the expected content eg:
{code}
assertHeaderStrict "Pragma", "no-cache"
{code}

h3. method: assertHeaderContains <headername>, <value>

Asserts that a response header contains the expected content, case and whitespace ignored eg:
{code}
assertHeader "Set-Cookie", "domain=.google.co.uk"
{code}

h3. method: assertHeaderContainsStrict <headername>, <value>

Asserts that a response header contains exactly the expected content eg:
{code}
assertHeaderContainsStrict "Set-Cookie", "domain=.google.co.uk"
{code}

h3. method: assertRedirectUrl <value>

Asserts that the response included a redirect to the specified URL
{code}
assertRedirectUrl "/auth/login"
{code}

h3. method: assertRedirectUrlContains <value>

Asserts that the response included a redirect that contains the specified string
{code}
assertRedirectUrlContains "?id=74"
{code}

h3. method: assertTitle <value>

Asserts that the title of the current page equals the value supplied, ignoring case and whitespace

h3. method: assertTitleContains <value>

Asserts that the title of the current page contains the value supplied, ignoring case and whitespace

h3. method: assertMeta <name>, <value>

Asserts that the meta tag of the current page with the specified name equals the value supplied, ignoring case and whitespace

h3. method: assertMetaContains <name>, <value>

Asserts that the meta tag of the current page with the specified name contains the value supplied, ignoring case and whitespace

h3. method: assertCookieExists <name>

Asserts that a cookie with that name exists in the browser of the currently executing test

h3. method: assertCookieExistsInDomain <name>, <domain>

Asserts that a cookie with that name exists in specified domain in the browser of the currently executing test

h3. method: assertCookieContains <name>, <content>

Asserts that a cookie with that name exists in the browser of the currently executing test, and contains the content expected (loosely - case and whitespace insensitive) 

h3. method: assertCookieContainsStrict <name>, <content>

Asserts that a cookie with that name exists in the browser of the currently executing test, and contains the content expected case and whitespace sensitive 


h2. Roadmap - future stuff

* Make assert variants dump out the actual value compared to in the case of failure eg "Value: 'xxxxx' did not contain 'y'"
* Add JSON and XML response parsing
* Add JSON and XML request payloads
* Monkey patch functional tests so no need to extend test class
* Support setting post BODY (not just params)
* Support asserting that an alert window pops up
* Fix HTML results and XSLT template says Unit Tests
* Custom test reports - with URL request stack (and all req params)
* Add support for assertElememtWithId and assertElememtWithClass
* Add assert variants that take message as first param
* Analyze stacktraces to find line of test that failed and highlight in reports

