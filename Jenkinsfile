pipeline {
  agent any
  tools {
    maven 'Maven3'
    jdk 'JDK11'
  }
  stages {
    stage ('Build') {
      when {
        allOf {
          not { branch 'master' }
          not { branch 'dev' }
        }
      }
      steps {
        lock('Docker') {
          sh 'mvn clean install -Pjacoco -U -DskipTests' 
        }
      }
    }
    stage ('Build & Deploy') {
      when {
        anyOf {
          branch 'dev'
          branch 'master'
        }
      }
      steps {
        lock('Docker') {
          sh 'mvn clean deploy -Pjacoco,postgres,publish-sql -U -DskipTests' 
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

