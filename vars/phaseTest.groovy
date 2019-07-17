import org.ods.parser.JUnitParser
import org.ods.phase.PipelinePhases
import org.ods.util.MultiRepoOrchestrationPipelineUtil

def call(Map metadata, List<Set<Map>> repos) {
    // Get a list of automated test scenarios from Jira
    def issues = jiraGetIssuesForJQLQuery(metadata, "project = ${metadata.id} AND labels = AutomatedTest AND issuetype = sub-task")
        .collect {[ id: it.id, key: it.key, summary: it.fields.summary, description: it.fields.description, url: it.self ]}

    def testResultsString = """
<testsuites>
    <testsuite name="Feature X" tests="1" skipped="0" failures="1" errors="0" timestamp="2019-06-25T18:12:36" hostname="surfer-172-29-1-61-hotspot.internet-for-guests.com" time="1.458">
        <properties/>
        <testcase name="Test Scenario A" classname="app.DocGenSpec" time="0.033">
            <failure message="java.io.FileNotFoundException: /Users/metmajer/Dropbox/IT%20Platform/demo/pltf-doc-gen/build/resources/test/templates.zip (No such file or directory)" type="java.io.FileNotFoundException">java.io.FileNotFoundException: /Users/metmajer/Dropbox/IT%20Platform/demo/pltf-doc-gen/build/resources/test/templates.zip (No such file or directory)
            at java.base/java.io.FileInputStream.open(FileInputStream.java:219)
            at java.base/java.io.FileInputStream.&lt;init&gt;(FileInputStream.java:157)
            at app.SpecHelper.mockTemplatesZipArchiveDownload(SpecHelper.groovy:49)
            at app.DocGenSpec.generate(DocGenSpec.groovy:59)
            </failure>
        </testcase>
        <system-out><![CDATA[[2019-06-25 20:12:36,150]-[Test worker] INFO  app.App - [test@netty]: Server started in 710ms

        POST /document    [application/json]     [application/json]    (/anonymous)

        listening on:
        http://localhost:9000/

        [2019-06-25 20:12:37,616]-[Test worker] INFO  app.App - Stopped
        ]]></system-out>
        <system-err><![CDATA[]]></system-err>
    </testsuite>
    <testsuite name="Feature Y" tests="2" skipped="0" failures="0" errors="0" timestamp="2019-06-25T18:12:42" hostname="surfer-172-29-1-61-hotspot.internet-for-guests.com" time="1.458">
        <properties/>
        <testcase name="Test Scenario B" classname="app.DocGenSpec" time="1.311"/>
        <testcase name="Test Scenario C" classname="app.DocGenSpec" time="0.113"/>
        <system-out><![CDATA[[2019-06-25 20:12:36,150]-[Test worker] INFO  app.App - [test@netty]: Server started in 710ms

        POST /document    [application/json]     [application/json]    (/anonymous)

        listening on:
        http://localhost:9000/

        [2019-06-25 20:12:37,616]-[Test worker] INFO  app.App - Stopped
        ]]></system-out>
        <system-err><![CDATA[]]></system-err>
    </testsuite>
</testsuites>
    """

    def testResults = JUnitParser.parseJUnitXML(testResultsString)
    println "TEST_RESULTS: " + testResults

    def testCasesExecuted = testResults.testsuites.each { testsuite ->
        testsuite.testcases.collect { it.name }
    }
    println "TEST_CASES_EXECUTED: " + testCasesExecuted

    // Execute phase for each repository
    def util = new MultiRepoOrchestrationPipelineUtil(this)
    util.prepareExecutePhaseForReposNamedJob(PipelinePhases.TEST_PHASE, repos)
        .each { group ->
            parallel(group)
        }

    // Provide Junit XML reports to Jenkins
    writeFile file: ".tmp/JUnitReport.xml", text: testResultsString
    junit ".tmp/*.xml"   
}

return this
