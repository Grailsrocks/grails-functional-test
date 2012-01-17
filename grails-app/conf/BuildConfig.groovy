// Add XERCES & XALAN to classpath for building - we assume you're building with 1.1 or higher now
/*
def xmlJars = new File("${basedir}/lib").listFiles().findAll { it.name.endsWith("._jar") }

grailsSettings.compileDependencies.addAll xmlJars
grailsSettings.runtimeDependencies.addAll xmlJars
grailsSettings.testDependencies.addAll xmlJars
*/

grails.project.dependency.resolution = {
   inherits "global"
   
	repositories {        
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenCentral()
        mavenRepo "http://repository.codehaus.org"
        mavenRepo "http://repository.jboss.org/maven2" // For SAC
    }

    dependencies {
        compile( 'org.codehaus.groovy.modules.http-builder:http-builder:0.5.2') {
            excludes 'groovy', 'xml-apis', 'xerces'
        }
        compile( 'net.sourceforge.htmlunit:htmlunit:2.9') {
            excludes 'xml-apis', 'xerces'
        }
        compile( 'net.sourceforge.htmlunit:htmlunit-core-js:2.9') {
            excludes 'xml-apis', 'xerces'
        }
        compile( 'org.apache.httpcomponents:httpclient:4.1.2') {
            excludes 'xml-apis', 'xerces'
        }
        
        test( 'org.w3c.css:sac:1.3') {
            excludes 'xml-apis', 'xerces'
        }
        test( 'commons-codec:commons-codec:1.6') {
            excludes 'xml-apis', 'xerces'
        }
        test( 'net.sourceforge.nekohtml:nekohtml:1.9.15') {
            excludes 'xml-apis', 'xerces'
        }
        test( 'net.sourceforge.cssparser:cssparser:0.9.6') {
            excludes 'xml-apis', 'xerces'
        }
        test( 'xalan:serializer:2.7.1') {
            excludes 'xml-apis', 'xerces'
        }
        test( 'xalan:xalan:2.7.1') {
            excludes 'xml-apis', 'xerces'
        }
/*
        test( 'xercesImpl:xercesImpl:2.9.1') {
            excludes 'xml-apis'
        }
*/
    }

    plugins {
        build( ":tomcat:$grailsVersion", ':release:1.0.1') {
            export = false
        }
    }
}