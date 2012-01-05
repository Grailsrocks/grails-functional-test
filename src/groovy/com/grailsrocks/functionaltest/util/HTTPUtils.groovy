package com.grailsrocks.functionaltest.util

class HTTPUtils {
    static boolean isRedirectStatus(code) {
        [300, 301, 302, 303, 307].contains(code)
    }
}