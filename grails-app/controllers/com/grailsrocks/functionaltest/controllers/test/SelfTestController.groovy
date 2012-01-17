package com.grailsrocks.functionaltest.controllers.test


class SelfTestController {
    def paramecho = { 
        println "Self Test param echo request: ${params}"
        render text:params, contentType:'text/plain'
    }
}