pipeline {
  agent any
  environment {
    PROJECT_VERSION = sh script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true
    NEXT_VERSION = PROJECT_VERSION.replace("-SNAPSHOT","")
  }
  parameters {
    booleanParam(defaultValue: false, name: 'release-start')
    booleanParam(defaultValue: false, name: 'release-finish')
    string(defaultValue: "$NEXT_VERSION", name: 'release')
  }
  tools {
    maven 'Maven3'
    jdk 'JDK11'
  }
  stages {
    stage ('Echo properties') {
      steps {
        echo "$VERSION"
        echo "$VERSION2"
      }
    }
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

