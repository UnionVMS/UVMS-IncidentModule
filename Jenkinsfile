pipeline {
  agent any
  tools {
    maven 'Maven3'
    jdk 'JDK11'
  }
  stages {
    stage ('Build') {
      steps {
        lock('Docker') {
          sh 'mvn clean install -Pdocker,jacoco,postgres,publish-sql -U -DskipTests' 
        }
      }
    }
    stage('SonarQube analysis') {
      steps{ 
        withSonarQubeEnv('Sonarqube.com') {
          sh 'mvn $SONAR_MAVEN_GOAL -Dsonar.dynamicAnalysis=reuseReports -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_AUTH_TOKEN $SONAR_EXTRA_PROPS'
        }
      }
    }
  }
  post {
    always {
      archiveArtifacts artifacts: '**/target/*.war'
      // junit '**/target/surefire-reports/*.xml'
    }
  }
}

