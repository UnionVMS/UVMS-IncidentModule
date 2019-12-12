pipeline {
  agent any
  parameters {
    booleanParam(defaultValue: true, name: 'isAwesome')
  }
  tools {
    maven 'Maven3'
    jdk 'JDK11'
  }
  stages {
    stage ('Build') {
      steps {
        lock('Docker') {
          sh 'mvn clean deploy -Pjacoco,postgres,publish-sql -U -DskipTests' 
        }
      }
    }
    stage('SonarQube analysis') {
      when {
        not { branch 'master' }
      }
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

