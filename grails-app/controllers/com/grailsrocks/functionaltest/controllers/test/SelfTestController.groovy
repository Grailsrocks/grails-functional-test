package com.grailsrocks.functionaltest.controllers.test

import javax.servlet.http.Cookie

class SelfTestController {

    def paramecho = {
        println "Self Test param echo request: ${params}"
        render text:params, contentType:'text/plain'
    }

    def minimalHtml = {
        render text: "<html><body>this is a minimal html body</body></html>"
    }

    def sampleHtml = {
        render """<html><head></head>
            <body>
              <div id="main-div">
                <div id="nav-div">
                  <ul name="menu">
                    <li>home
                    <li><a href="minimalHtml">show min</a>
                  </ul>
                </div>
                <div id="content-div">Sorry, no content...
                </div>
              </div>
            </body></html>"""
    }

    def info = {
        header('about', 'functional test plugin')
        render text: """<html><head>
            <title>info page</title>
            <meta charset="UTF-8">
            <meta name="keywords" content="HTML,CSS,JavaScript">
            </head><body>(empty)</body></html>"""
    }

    def withForm = {
        if (params.cmd == 'Clear') {
            params.input1 = null
            params.input2 = null
        }
        def result = ""
        if (params.input1 && params.input2) {
            result = params.int('input1') + params.int('input2')
        }
        render """<html><head></head>
            <body>
              <form id="form1">
                input 1: <input type="text" name="input1" value="${params.input1?:''}"><br>
                input 2: <input type="text" name="input2" value="${params.input2?:''}"><br>
                output:  <input type="text" name="output" value="${result}"><br>
                <input type="submit" name="cmd" value="Submit">
                <input type="submit" name="cmd" value="Clear">
              </form>
            </body></html>"""
    }

    def formWithPostOnly = {
        if (request.method == 'GET') {
            render """<html><body>
                 <form id="form1" method="POST">
                   <input type="text" name="input1">
                   <input type="text" name="input2">
                   <input type="submit" name="cmd" value="Submit">
                 </form></body></html>"""
        }
        else if (request.method == 'POST') {
            render "<html><body>${params.input1?:''}+${params.input2?:''}</body></html>"
        }
        else {
            render status: 400
        }
    }

    def echoHeader = {
        def customHeader = request.getHeader('X-custom')
        render text: "custom header: ${customHeader}"
    }

    def sendRedirect = {
        redirect(action: 'minimalHtml')
    }

    def returnHttpMethodUsed = {
        render "<html><body>request method used: ${request.method}</body></html>"
    }

    def returnRequestBody = {
        render """<html><body><pre id="post-data">${request.reader.text}</pre></body></html>"""
    }

    def setCookie = {
        Cookie cookie = new Cookie(params.cookieName, params.cookieValue)
        cookie.maxAge = params.int('cookieAge')
        response.addCookie(cookie)
        render status: 200
    }

    def echoCookies = {
        def cookieInfo = request.cookies.collect { "${it.name}=${it.value} on ${it.domain} for ${it.maxAge}" }.join("\n")
        render text: cookieInfo
    }
}
