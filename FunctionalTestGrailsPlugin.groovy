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
    def version = "2.0.STAINLESS-SNAPSHOT"

    def grailsVersion = "1.3 > *"

    def loadAfter = ['greenmail', 'fixtures']

    def scopes = [ includes: "functional_test" ]

    def title = "Functional Testing"
    def description = '''Simple 'pure grails' functional testing for your web applications'''

    static pluginExcludes = [
        'file:./grails-app/controllers/com/grailsrocks/functionaltest/controllers/test/**/*.*',
        'file:./web-app/**/*.*'
    ]

    def documentation = "http://grails.org/plugin/functional-test"

    def license = "APACHE 2"

    def organization = [name: "Grailsrocks", url: "http://grailsrocks.com/"]

    def developers = [
            [name: "Marc Palmer", email: "marc@grailsrocks.com"]
    ]

    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPFUNCTIONALTEST" ]

    def scm = [url: "https://github.com/Grailsrocks/grails-functional-test"]
}
