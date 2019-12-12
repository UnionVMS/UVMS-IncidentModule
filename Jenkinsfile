def digit

pipeline {
  agent any
  parameters {
    booleanParam(defaultValue: false, name: 'RELEASE', description: 'Create a release (This will only work from develop branch)')
    choice(choices: ['Incremental', 'Minor', 'Major'], name: 'RELEASE_TYPE', description: 'Type of release')
  }
  tools {
    maven 'Maven3'
    jdk 'JDK11'
  }
  stages {
    stage('Build') {
      when {
        expression { !params.RELEASE }
      }
      steps {
        lock('Docker') {
          sh 'mvn clean deploy -Pjacoco,postgres,publish-sql -U -DskipTests' 
        }
      }
    }
    stage('SonarQube analysis') {
      when {
        allOf {
          not { branch 'master' }
          expression { !params.RELEASE }
        }
      }
      steps{ 
        withSonarQubeEnv('Sonarqube.com') {
          sh 'mvn $SONAR_MAVEN_GOAL -Dsonar.dynamicAnalysis=reuseReports -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_AUTH_TOKEN $SONAR_EXTRA_PROPS'
        }
      }
    }
    stage('Release') {
      when {
        allOf {
          //branch 'develop'
          expression { params.RELEASE }
        }
      }
      steps {
        script {
          switch (params.RELEASE_TYPE) {
            case 'Incremental':
              digit = 2
              break
            case 'Minor':
              digit = 1
              break
            case 'Major':
              digit = 0
              break
          }
        }
        //sh 'mvn -B gitflow:release'
        echo "$RELEASE"
        echo "$RELEASE_TYPE"
        echo "$digit"
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

