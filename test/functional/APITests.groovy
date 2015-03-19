import com.grailsrocks.functionaltest.APITestCase

class APITests extends APITestCase {
    String base = 'http://coinabul.com/api.php'

    void testHead() {
        head(base)

        assertStatus 200
    }

    void testGet() {
        get(base)

        assertStatus 200
    }
}