#!groovy

@Library('EdgeLabJenkins@master') _

def version = version()
def full_workflow = (env.BRANCH_NAME == 'master')

def maven_image = "maven:3.6.1-jdk-8-slim"

node('default') {
  // Common steps for every jobs:
  stage('Checkout branch') {
    checkout scm
  }

  docker.image(maven_image).inside {
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
}
