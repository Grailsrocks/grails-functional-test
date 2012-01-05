// Add XERCES & XALAN to classpath for building - we assume you're building with 1.1 or higher now
/*
def xmlJars = new File("${basedir}/lib").listFiles().findAll { it.name.endsWith("._jar") }

grailsSettings.compileDependencies.addAll xmlJars
grailsSettings.runtimeDependencies.addAll xmlJars
grailsSettings.testDependencies.addAll xmlJars
*/

grails.project.dependency.resolution = {
   inherits "global"
   //flatDir name:'gfunclocalJars', dirs:'./lib/'
   
   dependencies {
       test( 'net.sourceforge.htmlunit:htmlunit:2.7')
       test( 'net.sourceforge.htmlunit:htmlunit-core-js:2.7')
       test( 'commons-codec:commons-codec:1.4')
       test( 'commons-httpclient:commons-httpclient:3.1')
       test( 'nekohtml:nekohtml:1.9.14')
       test( 'cssparser:cssparser:0.9.5')
       test( 'sac:sac:1.3')
       test( 'serializer:serializer:2.7.1')
       test( 'xalan:xalan:2.7.1')
       test( 'xercesImpl:xercesImpl:2.9.1')
   }
   
   plugins {
       runtime( ":tomcat:$grailsVersion") {
           export = false
       }
   }
}