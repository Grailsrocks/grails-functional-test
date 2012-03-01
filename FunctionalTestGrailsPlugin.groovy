/* Copyright 2004-2007 the original author or authors.
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
 
 class FunctionalTestGrailsPlugin {
    def version = "2.0.M2"
    
    def grailsVersion = "1.3 > *"

    def dependsOn = [:]

    def loadAfter = ['greenmail', 'fixtures']

    def scopes = [ includes: "functional_test" ]

    def author = "Marc Palmer"
    def authorEmail = "marc@grailsrocks.com"
    def title = "Functional Testing"
    def description = '''\
Simple 'pure grails' functional testing for your web applications
'''

    static pluginExcludes = [
        'file:./grails-app/conf/TestFilters.groovy',
        'file:./grails-app/controllers/com/grailsrocks/functionaltest/controllers/test/**/*.*',
        'file:./web-app/**/*.*'
    ]
    
    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/functional-test"

    def license = "APACHE 2"

    def organization = [name: "Grailsrocks", url: "http://grailsrocks.com/"]

    def developers = [
            [name: "Marc Palmer", email: "marc@grailsrocks.com"]
    ]

    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPFUNCTIONALTEST" ]

    def scm = [url: "https://github.com/Grailsrocks/grails-functional-test"]

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }
   
    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)		
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }
	                                      
    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }
	
    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
