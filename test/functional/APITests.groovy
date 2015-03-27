import com.grailsrocks.functionaltest.APITestCase

class APITests extends APITestCase {
    String base = 'https://twitter.com/statuses/'

    void testHead() {
        head(base + 'public_timeline.json')

        assertStatus 200
    }

    void testGet() {
        get(base + 'public_timeline.json')

        assertStatus 200
    }
    
    /**
     * Test the JSON response received from a remote web service that consumes JSON data in request
    */
    void testPost() {
        post("http://httpbin.org/post", {
            param1 = "value1"
            param2 = "value2"
        })
        
        assertStatus 200
        
        def model = this.response.contentAsString
        def map = JSON.parse(model)
        
        assertNotNull(map.attributes.id)
    }
    
}
