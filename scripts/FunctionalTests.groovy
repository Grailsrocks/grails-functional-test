/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Gant script that runs the functional tests
 *
 * @author Marc Palmer
 *
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import grails.util.GrailsUtil as GU;
import grails.util.GrailsWebUtil as GWU
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.*
import java.lang.reflect.Modifier;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator as GRC;
import org.apache.tools.ant.taskdefs.optional.junit.*
import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.RequestContextHolder;
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.TransactionStatus
import org.apache.commons.logging.LogFactory
import grails.web.container.EmbeddableServerFactory

Ant.property(environment: "env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
result = new TestResult()
compilationFailures = []
testingBaseURL = null
testingInProcessJetty = false

// Change default env to test
scriptEnv = "test"

includeTargets << grailsScript("Init")
includeTargets << grailsScript("Bootstrap")
includeTargets << grailsScript("Run")
includeTargets << grailsScript("War")

generateLog4jFile = true

target('default': "Run a Grails application's functional tests") {
    depends(classpath, checkVersion, configureProxy, parseArguments, clean, cleanTestReports)
    runFunctionalTests()
}

reportsDir = "${basedir}/test/reports"

def processResults = {
    if (result) {
        if (result.errorCount() > 0 || result.failureCount() > 0 || compilationFailures.size > 0) {
            event("StatusFinal", ["Tests failed: ${result.errorCount()} errors, ${result.failureCount()} failures, ${compilationFailures.size} compilation errors. View reports in $reportsDir"])
            exit(1)
        }
        else {
            event("StatusFinal", ["Tests passed. View reports in $reportsDir"])
            exit(0)
        }

    }
    else {
        event("StatusFinal", ["Tests passed. View reports in $reportsDir"])
        exit(0)
    }
}

target(runFunctionalTests: "The functional test implementation target") {
    depends(packageApp)

    // We accept commands of the form:
    // grails functional-tests [URL] [testname1] [testname2] [testnameN]
    // Where all in [...] are optional
    ftArgs = argsMap["params"]
    if (ftArgs && ftArgs[0] =~ "^http(s)?://") {
        testingBaseURL = ftArgs[0]

        // Shift the args
        ftArgs.remove(0)
    } else {
        // Default to internally hosted app
        testingBaseURL = "http://localhost:$serverPort$serverContextPath"
        if (!testingBaseURL.endsWith('/')) testingBaseURL += '/'
        testingInProcessJetty = true
    }

    if (testingInProcessJetty) {
        // Do init required to simulate runWar
        depends(configureProxy)
        if (!argsMap["dev-mode"]) war()
    }

    if (config.grails.testing.reports.destDir) {
        reportsDir = config.grails.testing.reports.destDir
    }

    Ant.mkdir(dir: reportsDir)
    Ant.mkdir(dir: "${reportsDir}/html")
    Ant.mkdir(dir: "${reportsDir}/plain")

    compileTests()
    packageTests()

    /* In Grails 1.1 we will use Groovy mixin
    // Call me evil
    Class testBaseClass = classLoader.loadClass('functionaltestplugin.FunctionalTestCase')
    GroovyTestCase.metaClass.mixin testBaseClass
    */

    def server
    def completed = false
    def previousRunMode

    previousRunMode = System.getProperty('grails.run.mode', '')
    System.setProperty('grails.run.mode', "functional-test")

    try {
        if (testingInProcessJetty) {
            def savedOut = System.out
            def savedErr = System.err
            try {
                new File(reportsDir, "bootstrap-out.txt").withOutputStream {outStream ->
                    System.out = new PrintStream(outStream)
                    new File(reportsDir, "bootstrap-err.txt").withOutputStream {errStream ->
                        System.err = new PrintStream(errStream)

                        if (argsMap["dev-mode"]) {
                            println "Running tests in dev mode"
                            server = runInline(SCHEME_HTTP, serverHost, serverPort, serverPortHttps)
                        }
                        else {
                            server = runWar(SCHEME_HTTP, serverHost, serverPort, serverPortHttps)
                        }
                        // start it
                        server.start()
                    }
                }
            } finally {
                System.out = savedOut
                System.err = savedErr
            }
        }


        System.setProperty('grails.functional.test.baseURL', testingBaseURL)

        System.out.println "Functional tests running with base url: ${testingBaseURL}"
        // @todo Hmmm this doesn't look like the right event to use
        event("AllTestsStart", ["Starting run-functional-tests"])
        doFunctionalTests()
        event("AllTestsEnd", ["Finishing run-functional-tests"])
        produceReports()
        completed = true
    }
    catch (Exception ex) {
        ex.printStackTrace()
        throw ex
    }
    finally {
        if (testingInProcessJetty && server) {
            stopWarServer()
        }
        System.setProperty('grails.run.mode', previousRunMode)
        if (completed) {
            processResults()
        }
    }
}

private runInline(scheme, host, httpPort, httpsPort) {
    EmbeddableServerFactory serverFactory = loadServerFactory()
    grailsServer = serverFactory.createInline("${basedir}/web-app", webXmlFile.absolutePath, serverContextPath, classLoader)
    runServer server: grailsServer, host:host, httpPort: httpPort, httpsPort: httpsPort, scheme:scheme
    startPluginScanner()
}

private runWar(scheme, host, httpPort, httpsPort) {
    EmbeddableServerFactory serverFactory = loadServerFactory()
    grailsServer = serverFactory.createForWAR(warName, serverContextPath)

    grails.util.Metadata.getCurrent().put(grails.util.Metadata.WAR_DEPLOYED, "true")
    runServer server:grailsServer, host:host, httpPort:httpPort, httpsPort: httpsPort, scheme: scheme
}

target(packageTests: "Puts some useful things on the classpath") {
    Ant.copy(todir: testDirPath) {
        fileset(dir: "${basedir}", includes: "application.properties")
    }
    Ant.copy(todir: testDirPath, failonerror: false) {
        fileset(dir: "${basedir}/grails-app/conf", includes: "**", excludes: "*.groovy, log4j*, hibernate, spring")
        fileset(dir: "${basedir}/grails-app/conf/hibernate", includes: "**/**")
        fileset(dir: "${basedir}/src/java") {
            include(name: "**/**")
            exclude(name: "**/*.java")
        }
        fileset(dir: "${basedir}/test/functional") {
            include(name: "**/**")
            exclude(name: "**/*.java")
            exclude(name: "**/*.groovy)")
        }
    }

}
target(compileTests: "Compiles the functional test cases") {
    event("TestCompileStart", ['functional-tests'])

    def destDir = testDirPath
    Ant.mkdir(dir: destDir)
    try {
        //def nonTestCompilerClasspath = compilerClasspath.curry(false)
        Ant.groovyc(destdir: destDir,
                projectName: grailsAppName,
                encoding: "UTF-8",
                classpathref: "grails.test.classpath", {
                    src(path: "${basedir}/test/functional")
                })
    }
    catch (Exception e) {
        event("StatusFinal", ["Compilation Error: ${e.message}"])
        exit(1)
    }

    classLoader = new URLClassLoader([new File(destDir).toURI().toURL()] as URL[],
            classLoader)
    Thread.currentThread().contextClassLoader = classLoader

    event("TestCompileEnd", ['functional-tests'])
}

def populateTestSuite = {suite, testFiles, classLoader, String base ->
    for (r in testFiles) {
        try {
            def fileName = r.URL.toString()
            def endIndex = -8
            if (fileName.endsWith(".java")) {
                endIndex = -6
            }
            def className = fileName[fileName.indexOf(base) + base.size()..endIndex].replace('/' as char, '.' as char)
            def c = classLoader.loadClass(className)
            if (TestCase.isAssignableFrom(c) && !Modifier.isAbstract(c.modifiers)) {
                suite.addTestSuite(c)
            }
            else {
                event("StatusUpdate", ["Functional test ${r.filename} is not a valid test case. It does not implement junit.framework.TestCase or is abstract!"])
            }
        } catch (Exception e) {
            compilationFailures << r.file.name
            event("StatusFinal", ["Error loading functional test: ${e.message}"])
            exit(1)
        }
    }
}
def runTests = {suite, TestResult result, Closure callback ->
    for (TestSuite test in suite.tests()) {
        new File("${reportsDir}/FUNCTEST-${test.name}.xml").withOutputStream {xmlOut ->
            new File("${reportsDir}/plain/FUNCTEST-${test.name}.txt").withOutputStream {plainOut ->

                def savedOut = System.out
                def savedErr = System.err

                try {
                    def outBytes = new ByteArrayOutputStream()
                    def errBytes = new ByteArrayOutputStream()
                    System.out = new PrintStream(outBytes)
                    System.err = new PrintStream(errBytes)
                    def xmlOutput = new XMLJUnitResultFormatter(output: xmlOut)
                    def plainOutput = new PlainJUnitResultFormatter(output: plainOut)
                    def junitTest = new JUnitTest(test.name)
                    try {
                        plainOutput.startTestSuite(junitTest)
                        xmlOutput.startTestSuite(junitTest)
                        savedOut.println "Running functional test ${test.name}..."
                        def start = System.currentTimeMillis()
                        def runCount = 0
                        def failureCount = 0
                        def errorCount = 0

                        for (i in 0..<test.testCount()) {
                            def thisTest = new TestResult()
                            thisTest.addListener(xmlOutput)
                            thisTest.addListener(plainOutput)
                            def t = test.testAt(i)
                            System.out.println "--Output from ${t.name}--"
                            System.err.println "--Output from ${t.name}--"

                            callback(test, {
                                savedOut.print "    ${t.name}... "
                                event("TestStart", [test, t, thisTest])
                                // Let the test know where it can communicate with the user
                                t.consoleOutput = savedOut
                                test.runTest(t, thisTest)
                                event("TestEnd", [test, t, thisTest])
                                thisTest
                            })
                            runCount += thisTest.runCount()
                            failureCount += thisTest.failureCount()
                            errorCount += thisTest.errorCount()

                            if (thisTest.errorCount() > 0 || thisTest.failureCount() > 0) {
                                thisTest.errors().each {result.addError(t, it.thrownException())}
                                thisTest.failures().each {result.addFailure(t, it.thrownException())}
                            }
                            else {savedOut.println " Passed!"}
                        }
                        junitTest.setCounts(runCount, failureCount, errorCount);
                        junitTest.setRunTime(System.currentTimeMillis() - start)
                    } finally {
                        def outString = outBytes.toString()
                        def errString = errBytes.toString()
                        new File("${reportsDir}/FUNCTEST-${test.name}-out.txt").write(outString)
                        new File("${reportsDir}/FUNCTEST-${test.name}-err.txt").write(errString)
                        plainOutput?.setSystemOutput(outString)
                        plainOutput?.setSystemError(errString)
                        plainOutput?.endTestSuite(junitTest)
                        xmlOutput?.setSystemOutput(outString)
                        xmlOutput?.setSystemError(errString)
                        xmlOutput?.endTestSuite(junitTest)
                    }
                } finally {
                    System.out = savedOut
                    System.err = savedErr
                }

            }
        }
    }
}

target(doFunctionalTests: "Run Grails' function tests under the test/functional directory") {
    try {
        def testFiles = resolveTestResources {"file:${basedir}/test/functional/${it}.groovy"}
        testFiles.addAll(resolveTestResources {"file:${basedir}/test/functional/${it}.java"})
        testFiles = testFiles.findAll {it.exists()}
        if (testFiles.size() == 0) {
            event("StatusUpdate", ["No tests found in test/functional to execute"])
            return
        }

        def suite = new TestSuite()
        classLoader.rootLoader.addURL(new File("test/functional").toURI().toURL())
        populateTestSuite(suite, testFiles, classLoader, "test/functional/")
        if (suite.testCount() > 0) {

            event("TestSuiteStart", ["functional"])
            int testCases = suite.countTestCases()
            println "-------------------------------------------------------"
            println "Running ${testCases} Functional Test${testCases > 1 ? 's' : ''}..."

            def start = new Date()
            runTests(suite, result) {test, invocation ->
                invocation()
            }
            def end = new Date()

            event("TestSuiteEnd", ["functional", suite])
            event("StatusUpdate", ["Functional Tests Completed in ${end.time - start.time}ms"])
            println "-------------------------------------------------------"
        }
    }
    catch (Exception e) {
        event("StatusFinal", ["Error running functional tests: ${e.toString()}"])
        e.printStackTrace()
    }
}

def resolveTestResources(patternResolver) {
    def testNames = getTestNames(ftArgs)

    if (!testNames) {
        testNames = config.grails.testing.patterns ?: ['**/*']
    }

    def testResources = []
    testNames.each {
        def testFiles = resolveResources(patternResolver(it))
        testResources.addAll(testFiles.findAll {it.exists()})
    }
    testResources
}

def getTestNames(testNames) {
    // If a list of test class names is provided, split it into ant
    // file patterns.
    def nameSuffix = 'Tests'
    if (config.grails.testing.nameSuffix) {
        nameSuffix = config.grails.testing.nameSuffix
    }

    if (testNames) {
        testNames = testNames.collect {
            // If the test name includes a package, replace it with the
            // corresponding file path.
            if (it.indexOf('.') != -1) {
                it = it.replace('.' as char, '/' as char)
            }
            else {
                // Allow the test class to be in any package.
                it = "**/$it"
            }
            return "${it}${nameSuffix}"
        }
    }

    return testNames
}
