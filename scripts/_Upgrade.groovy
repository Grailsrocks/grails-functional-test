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
