package scripts.jenkins

@Library("sym-pipeline") _

echo "Simulating environment variables/job parameters"
targetPodName = "develop"
targetPodHost = "https://develop.symphony.com"
targetPodAdminUsername = "bdk-integration-tests-user-3"
targetPodAdminPassword = "OHaXXjI+vQ+2WDNZG6yCnQ"

bdkIntegrationTestsBotUsername = "bdk-integration-tests-service-user-TEST1"
workerBotUsername = "bdk-integration-tests-worker-bot-TEST1"
def privateKeyContent = "default-value"
def publicKeyContent = "default-value"
def botPid = "default-value"

/* BEGIN OF EXECUTION FLOW */
node() {
    def environment = []
    runWithFrozenHashes([]) {
        try {
            stage("Install required packages") {
                sh "wget -nc -q https://github.com/mikefarah/yq/releases/download/v4.18.1/yq_linux_amd64"
                sh "mv yq_linux_amd64 yq && chmod +x yq"
                sh "./yq --version"
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

            stage("Checkout and configure bdk-intergation-tests repository") {
                withCredentials([
                        [$class: 'StringBinding', credentialsId: 'symphonyjenkinsauto-token', variable: 'TOKEN']]) {

                    sh "mkdir bdk-intergation-tests"
                    checkoutBdkIntergationTestsBranch("symphony-soufiane", "main")

                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/test/resources/rsa", "${bdkIntegrationTestsBotUsername}-private.pem", privateKeyContent)
                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/test/resources/rsa", "${bdkIntegrationTestsBotUsername}-public.pem", publicKeyContent)

                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/test/resources/rsa", "${workerBotUsername}-private.pem", privateKeyContent)
                    copyContentToFile("bdk-intergation-tests/bdk.integrationtest/src/test/resources/rsa", "${workerBotUsername}-public.pem", publicKeyContent)

                    updateBdkIntergationTestsConfig(targetPodName, targetPodHost, targetPodAdminUsername, targetPodAdminPassword)
                }
            }

            stage("Create JBot, PBot and integration test bot service accounts") {
                sh "cd bdk-intergation-tests//bdk.integrationtest/src/test/java/com/symphony/bdk/integrationtest \
                        && javac InitContextMain.java \
                        && cd ../../../.. \
                        && java com/symphony/bdk/integrationtest/InitContextMain"
            }

            stage("Checkout and configure JBot repository") {
                withCredentials([
                        [$class: 'StringBinding', credentialsId: 'symphonyjenkinsauto-token', variable: 'TOKEN']]) {

                    sh "mkdir jbot"
                    checkoutJbotBranch("symphony-soufiane", "main")
                    copyContentToFile("jbot/rsa", "privatekey.pem", privateKeyContent)
                    updateJbotConfig(targetPodHost.replace("https://", ""))
                    sh "cd jbot && ./gradlew bootRun &"
                    botPid = sh(script:"echo \$!", returnStdout: true)
                }
            }

            stage("Checkout and configure PBot repository") {
                println("to be implemented")
            }

            stage("Executing BDK Integration Tests") {
                withCredentials([
                        [$class: 'StringBinding', credentialsId: 'symphonyjenkinsauto-token', variable: 'TOKEN']]) {
                    configureMavenSettings()
                    executeBdkIntegrationTests(targetPodName)
                }
            }

            stage("Report results") {
                println("to be implemented")
            }
        } catch (error) {
            echo "Error while trying to checkout bdk-intergation-tests repository: ${error}"
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

def copyContentToFile(folder, filename, content) {
    echo "Copying content to ${folder}/${filename}"

    sh "cd ${folder} \
            && touch ${filename} \
            && echo \"${content}\" > ${filename}"
}

def updateBdkIntergationTestsConfig(podName, podHost, adminUsername, adminPassword) {
    sh "cd bdk-intergation-tests \
            && cd bdk.integrationtest/src/test/resources/pod_configs \
            && cat ${podName}.yaml"
    sh "./yq -i '.pods.${podName}.url = \"${podHost}\"' bdk-intergation-tests/bdk.integrationtest/src/test/resources/pod_configs/${podName}.yaml"
    sh "./yq -i '.pods.${podName}.adminUsername = \"${adminUsername}\"' bdk-intergation-tests/bdk.integrationtest/src/test/resources/pod_configs/${podName}.yaml"
    sh "./yq -i '.pods.${podName}.adminPassword = \"${adminPassword}\"' bdk-intergation-tests/bdk.integrationtest/src/test/resources/pod_configs/${podName}.yaml"
    sh "cat bdk-intergation-tests/bdk.integrationtest/src/test/resources/pod_configs/${podName}.yaml"
}

def updateJbotConfig(podHost) {
   sh "./yq -i '.bdk.host = \"${podHost}\"' jbot/src/main/resources/application.yaml"
}

def executeBdkIntegrationTests(podName) {
    echo "Executing BDK Integration Tests"
    sh  "cd bdk-intergation-tests/bdk.integrationtest && mvn clean install -B -Pci -Dfile.encoding='UTF-8' -DepodDeploymentName='deploymentnametochange' -DpodsEnvironment='${podName}' -DusingPods='${podName}' "
}

def configureMavenSettings() {
    echo "Configuring Maven Settings to be able to get latest version of used Artifacts"
    sh "mkdir /root/.m2 && cp /data/maven/settings.xml /root/.m2/settings.xml"
}