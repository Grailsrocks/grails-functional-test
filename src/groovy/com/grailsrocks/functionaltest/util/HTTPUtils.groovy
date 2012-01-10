package com.grailsrocks.functionaltest.util

class HTTPUtils {
    static MIME_MAPPINGS = [
        jpg: 'image/jpeg',
        png: 'image/png',
        gif: 'image/gif',
        txt: 'text/plain',
        xml: 'text/xml',
        json: 'text/json'
    ]
    
    static boolean isRedirectStatus(code) {
        [300, 301, 302, 303, 307].contains(code)
    }
    
    static String getMimeTypeOfFile(String fileName) {
        def dotpos = fileName.lastIndexOf('.')
        def mt 
        if (dotpos >= 0) {
            def ext = fileName[dotpos+1..-1]
            mt = MIME_MAPPINGS[ext]
        }
        if (!mt) {
            mt = 'application/octect-stream'        
        }
        return mt
    }
    
    
}