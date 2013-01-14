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
}