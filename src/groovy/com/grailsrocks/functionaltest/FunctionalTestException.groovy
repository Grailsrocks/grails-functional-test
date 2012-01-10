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

package com.grailsrocks.functionaltest

import grails.util.GrailsUtil

class FunctionalTestException extends junit.framework.AssertionFailedError {
    def urlStack
    def hackedCause
    
    FunctionalTestException(TestCaseBase test, Throwable cause) {
        super(cause.message ?: cause.toString())
        this.hackedCause = GrailsUtil.sanitize(cause)
        this.urlStack = test.urlStack
    }
    
    void dumpURLStack(PrintWriter pw = null) {
        if (!pw) pw = System.out
        pw.println "URL Stack that resulted in ${hackedCause ?: 'failure'}"
        pw.println "---------------"
        urlStack?.reverseEach {
            pw.println "${it.url} (${it.source})"
        }
        pw.println "---------------"
        
        if (hackedCause) {
            hackedCause.printStackTrace(pw)
        } else {
            super.printStackTrace(pw)
        }
    }
    
    void printStackTrace() {
        dumpURLStack()
    }

    void printStackTrace(PrintStream s) {
        dumpURLStack(new PrintWriter(s))
    }

    void printStackTrace(PrintWriter s) {
        dumpURLStack(s)
    }
}