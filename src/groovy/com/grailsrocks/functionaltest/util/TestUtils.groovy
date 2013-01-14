package com.grailsrocks.functionaltest.util

import com.grailsrocks.functionaltest.client.Client

class TestUtils {

    static int maxW = 80

    static final void dumpHeading(String title) {
        def padL = '== '
        def padR = '=' * Math.max( (int)2, (int)(maxW - (padL.length() + 1 + title.length())) )

        println(padL + title + ' ' + padR)
    }

    static final void dumpSeparator() {
        println('='*maxW)
    }

    static final void dumpRequestInfo(Client client) {
        println('')
        dumpHeading("Making request ${client.requestMethod} ${client.currentURL} parameters:")
        client?.requestParameters?.each {
            println( "${it.key}: ${it.value}")
        }
        dumpHeading("Request headers:")
        client?.requestHeaders?.each {Map.Entry it ->
            println("${it.key}: ${it.value}")
        }
        dumpHeading("Content")
        println(client.requestBody)
        dumpSeparator()
    }

    static final void dumpResponseHeaders(Client client) {
        dumpHeading("Response was ${client.responseStatus} (${client.responseStatusMessage ?: 'no message'}) headers:")
        client?.responseHeaders?.each {
            println( "${it.key}: ${it.value}")
        }
        dumpSeparator()
    }

    static final void dumpContent(Client client) {
        dumpHeading("Content")
        println(client.responseAsString)
        dumpSeparator()
    }
}
