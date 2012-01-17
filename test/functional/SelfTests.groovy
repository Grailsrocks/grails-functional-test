import com.grailsrocks.functionaltest.APITestCase

class SelfTests extends APITestCase {
    void testParamsPassedToHTTPGet() {
        get('/selfTest/paramecho?x=999&y=111')
        
        assertContentContains '"x":"999"'
        assertContentContains '"y":"111"'
    }

    void testParamsPassedToHTTPPost() {
        post('/selfTest/paramecho?x=999&y=111')

        assertContentContains '"x":"999"'
        assertContentContains '"y":"111"'
    }

    void testParamsPassedToHTTPPostWithFile() {
        post('/selfTest/paramecho?x=999&y=111') {
            settings.requestContentType = 'text/plain'
            file 'web-app/images/grails_logo.jpg'
        }
        
        assertContentContains '"x":"999"'
        assertContentContains '"y":"111"'
    }
}