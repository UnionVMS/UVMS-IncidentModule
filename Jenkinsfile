def digit

pipeline {
  agent any
  options {
    lock resource: 'Docker'
  }
  parameters {
    booleanParam(defaultValue: false, name: 'RELEASE', description: 'Create a release (This will only work from develop branch)')
    choice(choices: ['Incremental', 'Minor', 'Major'], name: 'RELEASE_TYPE', description: 'Type of release')
  }
  tools {
    maven 'Maven3'
    jdk 'JDK11'
  }
  triggers {
    issueCommentTrigger('.*test me.*')
  }
  stages {
    stage('Remove docker containers') {
      when {
        expression { !params.RELEASE }
      }
      steps {
        sh 'docker ps -a -q|xargs -r docker rm -f'
      }
    }
    stage('Build') {
      when {
        expression { !params.RELEASE }
      }
      steps {
        sh 'mvn clean deploy -Pdocker,jacoco,postgres,publish-sql -U'
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
          branch 'develop'
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
        git branch: 'master', url: 'git@github.com:UnionVMS/UVMS-IncidentModule.git', credentialsId: 'bae67ea8-994c-429a-8a03-49b6ca0d3392'
        git branch: 'develop', url: 'git@github.com:UnionVMS/UVMS-IncidentModule.git', credentialsId: 'bae67ea8-994c-429a-8a03-49b6ca0d3392'
        sh "mvn -B gitflow:release -DskipTestProject -DversionDigitToIncrement=${digit}"
      }
    }
  }
  post {
    always {
      deleteDir()
      //archiveArtifacts artifacts: '**/target/*.war'
      // junit '**/target/surefire-reports/*.xml'
    }
  }
}
