class TestFilters {
    def filters = {
        test(uri:'/**') {
            before = {
                println "Filter says params are: $params"
            }
        }
    }
}
