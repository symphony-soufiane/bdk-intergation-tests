package scripts.jenkins

@Library("sym-pipeline") _

node() {
    def environment = []
    runWithFrozenHashes([]) {
        try {
            stage("Start") {
                println "Starting the job"
            }
        } catch (error) {
            echo "Error while trying to checkout bdk-intergation-tests repository: ${error}"
            currentBuild.setResult("FAILURE")
            return
        }
    }
}