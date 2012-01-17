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
 
class TestingTests extends functionaltestplugin.FunctionalTestCase {
    void testTrivialGet() {
        get 'http://json.org'
        assertStatus 200
        assertContentContains "JSON"
    }

/*    
    void testTwitterSearch() {
        get 'http://www.twitter.com'

        click "Search"

        assertStatus 200
        assertContentContains "search"

        form('searchForm') {
            q = "#grails"
            click "Search"
        }

        assertStatus 200
        
        assertTitleContains "#grails"
        assertTitleContains "twitter search"
        assertContentContains "#grails"
    }
*/

    void testGrailsPluginSearch() {
        get("http://grails.org/")
        click "Plugins"
        
        expect status:200, contentType:'text/html'
        
        form('pluginSearch') {
            q = 'functional-test'
            Search.click()
        }
        
        expect status:200, contentType:'text/html'

        assertContentContains "functional test"
    }
}

