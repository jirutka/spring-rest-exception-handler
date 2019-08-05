#!groovy

@Library('EdgeLabJenkins@master') _

def app_name = "spring-rest-exception-handler"

def worker_image = "edgelab/jenkins-worker:v11.4.2"
def flavor = "default"

def version = version()
def full_workflow = (env.BRANCH_NAME == 'master')

awsDockerNode(app_name, flavor, worker_image) {

    // Common steps for every jobs:
    stage('Checkout branch') {
        checkout scm
    }

    stage("Compile and test") {
        mavenw("versions:set -DnewVersion='${version}'", "clean verify")

        stage("Publish JUnit test result report") {
            junit '**/target/surefire-reports/TEST-*.xml'
            junit testResults: '**/target/failsafe-reports/TEST-*.xml', allowEmptyResults: true
        }
    }

    stageWhen('Publish ', full_workflow) {
        mavenw("versions:set -DnewVersion='${version}'", "deploy -DskipTests -Dgpg.skip -am")
    }
}
