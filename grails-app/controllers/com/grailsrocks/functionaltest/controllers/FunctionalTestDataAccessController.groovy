package com.grailsrocks.functionaltest.controllers

import grails.util.Environment
import grails.converters.JSON
import grails.util.GrailsNameUtils

class FunctionalTestDataAccessController {
    
    def fixtureLoader
    
    def objectExists = {
        assert Environment.current != Environment.PRODUCTION
        def clsName = params.className
        def findField = GrailsNameUtils.getClassNameRepresentation(params.findField)
        def findValue = params.findValue
        def domclass = grailsApplication.getDomainClass(clsName).clazz
        def obj = domclass."findBy${findField}"(findValue)
        def res = [:]
        if (!obj) {
            res.error = 'Not found'
        }
        render(text: res as JSON, status:obj ? 200 : 404)
    }

    def findObject = {
        assert Environment.current != Environment.PRODUCTION
        def clsName = params.className
        def findField = GrailsNameUtils.getClassNameRepresentation(params.findField)
        def findValue = params.findValue
        def domclass = grailsApplication.getDomainClass(clsName).clazz
        def obj = domclass."findBy${findField}"(findValue)
        if (obj) {
            render obj as JSON
        } else {
            render(text: [error:'Not found'] as JSON, status: 404)
        }
    }
    
    def fixture = {
        assert Environment.current != Environment.PRODUCTION

        def f = fixtureLoader.load(params.name)
        def res = [:]
        if (!f) { 
            res.error = 'No such fixture: [${params.name}]'
        }
        render(text: res as JSON, status: res.error ? 500 : 200)
    }
}