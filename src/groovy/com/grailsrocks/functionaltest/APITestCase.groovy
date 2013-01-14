package com.grailsrocks.functionaltest

import com.grailsrocks.functionaltest.client.APIClient

/**
 * Test client that uses RESTClient
 */
class APITestCase extends TestCaseBase {
    Class getDefaultClientType() {
        APIClient
    }

	def head(url, Closure paramSetup = null) {
	    doRequest(url, 'HEAD', paramSetup)
	}
}