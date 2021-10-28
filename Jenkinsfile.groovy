#!groovy

@Library('EdgeLabJenkins') _

def version = version()
def full_workflow = (env.BRANCH_NAME == 'master')

def maven_image = "maven:3.6.1-jdk-8-slim"

node('default') {

  stage('Checkout branch') {
    checkout scm
  }

  docker.image(maven_image).inside {
    stage("Compile and test") {
      mavenw("versions:set -DnewVersion='${version}'", "clean verify")

      stage("Publish JUnit test result report") {
        junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
        junit testResults: '**/target/failsafe-reports/TEST-*.xml', allowEmptyResults: true
      }
    }

    stageWhen('Deploy artifact', full_workflow) {
      configFileProvider([configFile(fileId: 'maven-external', variable: 'MAVEN_SETTINGS')]) {
          sh "mvn --batch-mode --settings ${env.MAVEN_SETTINGS} deploy -DskipTests -Dgpg.skip -am"
      }
    }
  }
}
