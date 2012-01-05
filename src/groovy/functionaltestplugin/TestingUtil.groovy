package functionaltestplugin

class TestingUtil {
    static void runTestScript(Closure c) {
        def test = new FunctionalTestCase()
        
        c.delegate = test
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c() 
    }
}