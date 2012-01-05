//
// This script is executed by Grails during application upgrade ('grails upgrade' command).
// This script is a Gant script so you can use all special variables
// provided by Gant (such as 'baseDir' which points on project base dir).
// You can use 'Ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
// Ant.mkdir(dir:"/Users/marc/Projects/SimpleHttpTest/grails-app/jobs")
//

includeTargets << grailsScript("_GrailsInit")

// Activate XERCES & XALAN if this is a version of Grails beyond 1.0.x
if (!grailsVersion.startsWith('1.0')) {

    def clashingJars = ant.fileScanner {
        fileset(dir: "${functionalTestPluginDir}/lib") {
            include(name: "*._jar")
        }
    }.each {File jar ->
        moveLib(jar.absolutePath, (jar.absolutePath - '._jar') + '.jar')
    }
}

def moveLib(from, to) {
    //done as a copy and delete due to strange locking problem on windows
    ant.copy(overwrite: true, verbose: true, file: from,
            toFile: to)
    ant.delete(verbose: true, file: from)
}
