import grails.util.Environment

class FunctionalTestUrlMappings {
    static mappings = {
        if (Environment.current != Environment.PRODUCTION) {
            "/functionaltesting/$action?"{
                controller = "functionalTestDataAccess"
            }
        }
	}
}
