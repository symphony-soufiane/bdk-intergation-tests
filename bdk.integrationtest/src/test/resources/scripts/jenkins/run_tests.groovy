package scripts.jenkins

import java.text.SimpleDateFormat
@Library("sym-pipeline") _

env.TARGET_POD_NAME = env.TARGET_POD_NAME ?: "localhost"
env.TARGET_POD_HOST = env.TARGET_POD_HOST ?: "localhost.symphony.com"
env.CREATE_EPODS = env.CREATE_EPODS ?: true
env.EPOD1_SBE_ORG = env.EPOD1_SBE_ORG ?: "SymphonyOSF"
env.EPOD1_SBE_BRANCH = env.EPOD1_SBE_BRANCH ?: "dev"
env.EPOD2_SBE_ORG = env.EPOD1_SBE_ORG ?: "SymphonyOSF"
env.EPOD2_SBE_BRANCH = env.EPOD1_SBE_BRANCH ?: "dev"
env.AGENT_GIT = env.AGENT_GIT ?: "SymphonyOSF:master"
env.EPODS_TIME_TO_LIVE = env.EPODS_TIME_TO_LIVE ?: 0
env.TARGET_POD_ADMIN_USERNAME = env.TARGET_POD_ADMIN_USERNAME ?: "admin@symphony.com"
env.TARGET_POD_ADMIN_PASSWORD = env.TARGET_POD_ADMIN_PASSWORD ?: "password"
env.INTEGRATION_TESTS_BOT_USERNAME = env.INTEGRATION_TESTS_BOT_USERNAME ?: "bdk-integration-tests-service-user-TEST1"
env.WORKER_BOT_USERNAME = env.WORKER_BOT_USERNAME ?: "bdk-integration-tests-worker-bot-TEST1"
env.RUN_JAVA_BOT = env.RUN_JAVA_BOT ?: true
env.RUN_PYTHON_BOT = env.RUN_PYTHON_BOT ?: true
env.BDK_INTEGRATION_TESTS_BRANCH = env.BDK_INTEGRATION_TESTS_BRANCH ?: "main"
env.BDK_INTEGRATION_TESTS_ORG = env.BDK_INTEGRATION_TESTS_ORG ?: "SymphonyOSF"
env.WEBHOOK_URL = "https://corporate.symphony.com/integration/v1/whi/simpleWebHookIntegration/5810d144e4b0f884b709cc90/62cf0c1a286be962ac8bc61a"

IS_BOT_FORCED_TO_TERMINATE = false
JBOT_FAILED_TEST_CASES = ""
JBOT_TOTAL_TEST_CASES = 0
JBOT_TOTAL_FAILED_TEST_CASES = 0

def privateKeyContent = "default-value"
def publicKeyContent = "default-value"
def botPid = "default-value"
def buildStages = []

/* BEGIN OF EXECUTION FLOW */
node() {
    parallelsAlwaysFailFast()
    runWithFrozenHashes([]) {
        try {
            stage("Install required packages") {
                if (env.RUN_PYTHON_BOT.toBoolean()==true) {
                    sh "apt-get install -y build-essential zlib1g-dev libncurses5-dev libgdbm-dev libnss3-dev \
                        libssl-dev libsqlite3-dev libreadline-dev libffi-dev curl libbz2-dev \
                        && wget https://www.python.org/ftp/python/3.10.3/Python-3.10.3.tgz \
                       && tar -zxvf Python-3.10.3.tgz \
                       && cd Python-3.10.3 \
                       && ./configure --enable-optimizations && make && make install \
                       && update-alternatives --install /usr/bin/python python /usr/local/bin/python3.10 1 \
                       && update-alternatives --install /usr/bin/pip pip /usr/local/bin/pip3.10 1 \
                       && apt-get remove -y build-essential zlib1g-dev libncurses5-dev libgdbm-dev libnss3-dev \
                       libssl-dev libsqlite3-dev libreadline-dev libffi-dev libbz2-dev \
                       && apt-get autoremove -y && apt-get autoclean -y"

                       sh "python3 --version"
                       sh "pip3 --version"
                }

                sh 'wget -nc -q https://github.com/mikefarah/yq/releases/download/v4.18.1/yq_linux_amd64'
                sh 'mv yq_linux_amd64 yq && chmod +x yq'
                sh './yq --version'

                sh "apt-get install -y libxml2-utils"
            }

             stage("Retrieve secrets") {
                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: 'sym-aws-dev',
                    accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                    secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                ]]) {
                    privateKeyContent = sh(script:"aws ssm get-parameter --name /devsol/test/bdk_private_key --region us-east-1 --with-decryption --output text --query Parameter.Value", returnStdout: true)
                    publicKeyContent = sh(script:"aws ssm get-parameter --name /devsol/test/bdk_public_key --region us-east-1 --with-decryption --output text --query Parameter.Value", returnStdout: true)
                }
            }

            stage("Checkout and configure bdk-integration-tests repository") {
                withCredentials([
                        [$class: 'StringBinding', credentialsId: 'symphonyjenkinsauto-token', variable: 'TOKEN']]) {

                    sh "mkdir bdk-intergation-tests"
                    checkoutBdkIntergationTestsBranch(env.BDK_INTEGRATION_TESTS_ORG, env.BDK_INTEGRATION_TESTS_BRANCH)

                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/main/resources/rsa", "${env.INTEGRATION_TESTS_BOT_USERNAME}-private.pem", privateKeyContent)
                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/main/resources/rsa", "${env.INTEGRATION_TESTS_BOT_USERNAME}-public.pem", publicKeyContent)

                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/main/resources/rsa", "${env.WORKER_BOT_USERNAME}-private.pem", privateKeyContent)
                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/main/resources/rsa", "${env.WORKER_BOT_USERNAME}-public.pem", publicKeyContent)

                    updateBdkIntergationTestsConfig(env.TARGET_POD_NAME, env.TARGET_POD_HOST, env.TARGET_POD_ADMIN_USERNAME, env.TARGET_POD_ADMIN_PASSWORD)
                }
            }

            stage("Create service accounts") {
                configureMavenSettings()
                sh "cd bdk-intergation-tests/bdk.integrationtest \
                        && mvn clean install -DskipTests=true -B -Pci \
                        && java -Dfile.encoding='UTF-8' -DepodDeploymentName='deploymentnametochange' -DpodsEnvironment='${env.TARGET_POD_NAME}' -DusingPods='${env.TARGET_POD_NAME}' -DintegrationTestsBotUsername='${env.INTEGRATION_TESTS_BOT_USERNAME}' -DintegrationTestsWorkerUsername='${env.WORKER_BOT_USERNAME}' -jar target/bdk.integrationtest-0.0.1-SNAPSHOT.jar"
            }

            stage("Checkout and configure JBot repository") {
                if (env.RUN_JAVA_BOT.toBoolean() == true) {
                    withCredentials([
                            [$class: 'StringBinding', credentialsId: 'symphonyjenkinsauto-token', variable: 'TOKEN']]) {
                            sh "mkdir jbot"
                            checkoutJbotBranch("symphony-soufiane", "main")
                            copyContentToFile("jbot/rsa", "privatekey.pem", privateKeyContent)
                            updateJbotConfig(env.TARGET_POD_HOST.replace("https://", ""))
                    }
                }
            }

            stage("Checkout and configure PBot repository") {
                if (env.RUN_PYTHON_BOT.toBoolean() == true) {
                    withCredentials([
                            [$class: 'StringBinding', credentialsId: 'symphonyjenkinsauto-token', variable: 'TOKEN']]) {
                            sh "mkdir pbot"
                            checkoutPbotBranch("symphony-soufiane", "main")
                            copyContentToFile("pbot/rsa", "privatekey.pem", privateKeyContent)
                            updatePbotConfig(env.TARGET_POD_HOST.replace("https://", ""))
                    }
                }
            }

            try {
                stage("Parallel run: Tests x JBot") {
                    if (env.RUN_JAVA_BOT.toBoolean() == true) {
                        buildStagesMap = [:]
                        buildStagesMap.put("BDK_INTEGRATION_TESTS", integrationTestsStage(env.TARGET_POD_NAME))
                        buildStagesMap.put("JBOT", jbotStage())
                        buildStages.add(buildStagesMap)
                    }
                }

                for (buildStage in buildStages) {
                    buildStage.failFast = true
                    parallel(buildStage)
                }
            } catch(error) {
                if ("${IS_BOT_FORCED_TO_TERMINATE}".toBoolean() == true) {
                    echo "Expected error caught: JBot has been forced to terminate as integration tests are done."
                    echo "actual error: ${error}"
                    IS_BOT_FORCED_TO_TERMINATE = false
                } else {
                    echo "Unexpected error caught."
                    throw error
                }
            }

            try {
                // reinitialise buildStages for next parallel runs
                buildStages = []
                stage("Parallel run: Tests x PBot") {
                    if (env.RUN_PYTHON_BOT.toBoolean() == true) {
                        buildStages = []
                        buildStagesMap = [:]
                        buildStagesMap.put("BDK_INTEGRATION_TESTS", integrationTestsStage(env.TARGET_POD_NAME))
                        buildStagesMap.put("PBOT", pbotStage())
                        buildStages.add(buildStagesMap)
                    }
                }

                for (buildStage in buildStages) {
                    buildStage.failFast = true
                    parallel(buildStage)
                }
            } catch(error) {
                if ("${IS_BOT_FORCED_TO_TERMINATE}".toBoolean() == true) {
                    echo "Expected error caught: PBot has been forced to terminate as integration tests are done."
                    IS_BOT_FORCED_TO_TERMINATE = false
                } else {
                    echo "Unexpected error caught."
                    throw error
                }
            }

            stage("Report results") {
                sbeBranch = env.EPOD1_SBE_ORG + "/" + env.EPOD1_SBE_BRANCH
                notificationMessage = getMessageMLNotificationTemplate(sbeBranch, env.AGENT_GIT, env.RUN_JAVA_BOT.toBoolean(), JBOT_TOTAL_TEST_CASES, JBOT_TOTAL_FAILED_TEST_CASES, JBOT_FAILED_TEST_CASES)

                println notificationMessage
                //adds the string content to a file in the workspace. It allows to not have to escape characters for the command
                writeFile file: 'message-ml-content.xml', text: notificationMessage, encoding: 'UTF-8'
                sh "curl -L -X POST -H 'Content-Type: multipart/form-data' -F 'message=@message-ml-content.xml' ${env.WEBHOOK_URL}"
            }
        } catch (error) {
            echo "Error while running the pipeline: ${error}"
            currentBuild.setResult("FAILURE")
            return
        }
    }
}
/* END OF EXECUTION FLOW */

def checkoutBdkIntergationTestsBranch(org, branch) {
    echo "Git checkout of bdk-intergation-tests branch/PR"

    sh "git clone http://${env.TOKEN}:x-oauth-basic@github.com/${org}/bdk-intergation-tests.git ./bdk-intergation-tests"
    sh "cd bdk-intergation-tests \
            && git config --add remote.origin.fetch +refs/pull/*:refs/remotes/origin/pr/* \
            && git fetch origin \
            && git checkout ${branch}"
}

def checkoutJbotBranch(org, branch) {
    echo "Git checkout of JBot branch/PR"

    sh "git clone http://${env.TOKEN}:x-oauth-basic@github.com/${org}/JBot.git ./jbot"
    sh "cd jbot \
            && git config --add remote.origin.fetch +refs/pull/*:refs/remotes/origin/pr/* \
            && git fetch origin \
            && git checkout ${branch}"
}

def checkoutPbotBranch(org, branch) {
    echo "Git checkout of PBot branch/PR"

    sh "git clone http://${env.TOKEN}:x-oauth-basic@github.com/${org}/PBot.git ./pbot"
    sh "cd pbot \
            && git config --add remote.origin.fetch +refs/pull/*:refs/remotes/origin/pr/* \
            && git fetch origin \
            && git checkout ${branch}"
}

def copyContentToFile(folder, filename, content) {
    echo "Copying content to ${folder}/${filename}"

    sh "cd ${folder} \
            && touch ${filename} \
            && echo \"${content}\" > ${filename}"
}

def updateBdkIntergationTestsConfig(podName, podHost, adminUsername, adminPassword) {
    sh "cd bdk-intergation-tests \
            && cd bdk.integrationtest/src/main/resources/pod_configs \
            && cat ${podName}.yaml"
    sh "./yq -i '.pods.${podName}.url = \"${podHost}\"' bdk-intergation-tests/bdk.integrationtest/src/main/resources/pod_configs/${podName}.yaml"
    sh "./yq -i '.pods.${podName}.adminUsername = \"${adminUsername}\"' bdk-intergation-tests/bdk.integrationtest/src/main/resources/pod_configs/${podName}.yaml"
    sh "./yq -i '.pods.${podName}.adminPassword = \"${adminPassword}\"' bdk-intergation-tests/bdk.integrationtest/src/main/resources/pod_configs/${podName}.yaml"
    sh "cat bdk-intergation-tests/bdk.integrationtest/src/main/resources/pod_configs/${podName}.yaml"
}

def updateJbotConfig(podHost) {
   sh "./yq -i '.bdk.host = \"${podHost}\"' jbot/src/main/resources/application.yaml"
}

def updatePbotConfig(podHost) {
   sh "./yq -i '.host = \"${podHost}\"' pbot/resources/config.yaml"
}

def executeBdkIntegrationTests(podName) {
    echo "Executing BDK Integration Tests"
    sh  "cd bdk-intergation-tests/bdk.integrationtest && mvn verify -B -Pci -Dfile.encoding='UTF-8' -DepodDeploymentName='deploymentnametochange' -DpodsEnvironment='${podName}' -DusingPods='${podName}' -DintegrationTestsBotUsername='${env.INTEGRATION_TESTS_BOT_USERNAME}' -DintegrationTestsWorkerUsername='${env.WORKER_BOT_USERNAME}'"
}

def configureMavenSettings() {
    echo "Configuring Maven Settings to be able to get latest version of used Artifacts"
    sh "mkdir /root/.m2 && cp /data/maven/settings.xml /root/.m2/settings.xml"
}

def jbotStage() {
    return {
        stage("Running JBot") {
            // JBot will timeout after 2m to release the pipeline progress
            sh "cd jbot && ./gradlew bootRun"
        }
    }
}

def pbotStage() {
    return {
        stage("Running PBot") {
            // PBot will timeout after 2m to release the pipeline progress
            sh "cd pbot && pip install -r requirements.txt"
            sh "cd pbot && python -m src"
        }
    }
}

def integrationTestsStage(targetPodName) {
    return {
        stage("Running BDK Integration tests") {
            sh 'sleep 60' // Sleep 60s to give more time to JBot/Pbot to be up

            try {
                executeBdkIntegrationTests(targetPodName)
            } catch (error) {
                // ignore the error
            }

            IS_BOT_FORCED_TO_TERMINATE = true
            archiveArtifacts(artifacts: '**/target/failsafe-reports/TEST-com.symphony.bdk.integrationtest.*.xml', allowEmptyArchive: true)
            prepareTestsReport()

            // throw exception to force parallel stages to terminate
            throw new Exception("Bot is forced to terminate as integration tests are done.")
        }
    }
}

def prepareTestsReport() {

    /* Write output in file for these reasons:
        The command exit status is non zero when the xmllint result is empty which makes the step failing. Returning the status avoid that
        xmllint logs are only printed in the output and only the results go to the file
    */
    failedTestCases = sh(script:"xmllint --xpath '//failure/../@name' bdk-intergation-tests/bdk.integrationtest/target/failsafe-reports/TEST-*.xml > xmllintOutput.txt", returnStatus: true)
    failedTestCases = readFile "${env.WORKSPACE}/xmllintOutput.txt"

    if (failedTestCases.trim()) { // not empty
        def failedTestCasesSplit = failedTestCases.replace("\"", "").split("name=")
        failedTestCasesSplit.each() {
            if (failedTestCasesSplit.trim()) { // not empty string
                JBOT_TOTAL_FAILED_TEST_CASES += 1
            }
        }
        JBOT_FAILED_TEST_CASES = failedTestCasesSplit
    }

    JBOT_TOTAL_TEST_CASES = sh(script: "cd bdk-intergation-tests/bdk.integrationtest/target/failsafe-reports && xmllint --xpath '//completed/text()' failsafe-summary.xml", returnStdout: true)
}

def getMessageMLNotificationTemplate(sbeBranch, agentBranch, isJbotExecuted, jbotTotal, jbotFailedTotal, jbotFailedTestCases) {
    Date date = new Date()
    def currentDay = new SimpleDateFormat("MM/dd/yyyy").format(date)

    def jbotSummary = "<span class='tempo-text-color--gray'><h4>Java BDK tests summary: Java BDK Tests did not run</h4></span>"
    def jbotDetails = ""
    def pbotSummary = "<span class='tempo-text-color--gray'><h4>Python BDK Tests summary: Python BDK Tests did not run</h4></span>"
    def pbotDetails = ""
    if (isJbotExecuted) {

        if (jbotFailedTotal == 0) {
            jbotSummary = "<span class='tempo-text-color--green'><h4>Java BDK tests summary: SUCCESS</h4></span>"
        } else {
            jbotSummary = "<span class='tempo-text-color--red'><h4>Java BDK tests summary: {JBOT_FAILED_TOTAL}/{JBOT_TOTAL} TESTS FAILED</h4></span><p><br/>Click for details</p>"
            jbotFailedTestCases.each() {
                if (it.trim()) { // not empty string
                    jbotDetails += "<br/><p>${it}</p>"
                }
            }
        }

        jbotSummary = jbotSummary.replace('{JBOT_TOTAL}', jbotTotal.toString())
        jbotSummary = jbotSummary.replace('{JBOT_FAILED_TOTAL}', jbotFailedTotal.toString())
    }

    def messageML = """<messageML>
                            <br/>
                            <h2>BDK Integration Tests - SBE: {SBE_BRANCH} - Agent {AGENT_BRANCH} ({CURRENT_DATE})</h2>
                            <span><a href='${env.BUILD_URL}'>Job: ${env.JOB_BASE_NAME} Build: ${env.BUILD_NUMBER}</a></span>
                            <div>
                                <card class="barStyle" accent="tempo-bg-color--blue" iconSrc="./images/favicon.png">
                                    <header>
                                        ${jbotSummary}
                                    </header>
                                    <body>
                                        ${jbotDetails}
                                    </body>
                                </card>
                            </div>

                            <div>
                                <card class="barStyle" accent="tempo-bg-color--yellow" iconSrc="./images/favicon.png">
                                    <header>
                                        ${pbotSummary}
                                    </header>
                                    <body>
                                        ${pbotDetails}
                                    </body>
                                </card>
                            </div>
                      </messageML>"""
    messageML = messageML.replace('{SBE_BRANCH}', sbeBranch)
    messageML = messageML.replace('{AGENT_BRANCH}', agentBranch)
    messageML = messageML.replace('{CURRENT_DATE}', currentDay)
    return messageML
}
