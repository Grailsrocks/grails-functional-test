import grails.util.Environment
class TestFilters {
    def filters = {
        test(uri:'/**') {
            before = {
                //disable filter in production environments
                if(Environment.current != Environment.PRODUCTION){
                    println "Filter says params are: $params"    
                }                
            }
        }
    }
}
