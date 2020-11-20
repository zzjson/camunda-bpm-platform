#!/usr/bin/env groovy

// https://github.com/camunda/jenkins-global-shared-library
@Library('camunda-ci') _

String getMavenAgent(Integer mavenCpuLimit = 4, String dockerTag = '3.6.3-openjdk-8'){
  String mavenForkCount = mavenCpuLimit;
  String mavenMemoryLimit = mavenCpuLimit * 2;
  """
metadata:
  labels:
    agent: ci-cambpm-camunda-cloud-build
spec:
  nodeSelector:
    cloud.google.com/gke-nodepool: agents-n1-standard-32-netssd-preempt
  tolerations:
  - key: "agents-n1-standard-32-netssd-preempt"
    operator: "Exists"
    effect: "NoSchedule"
  containers:
  - name: maven
    image: maven:${dockerTag}
    command: ["cat"]
    tty: true
    env:
    - name: LIMITS_CPU
      value: ${mavenForkCount}
    - name: TZ
      value: Europe/Berlin
    resources:
      limits:
        cpu: ${mavenCpuLimit}
        memory: ${mavenMemoryLimit}Gi
      requests:
        cpu: ${mavenCpuLimit}
        memory: ${mavenMemoryLimit}Gi
  """
}

String getChromeAgent(Integer cpuLimit = 1){
  String memoryLimit = cpuLimit * 2;
  """
  - name: chrome
    image: 'gcr.io/ci-30-162810/chrome:78v0.1.2'
    command: ["cat"]
    tty: true
    env:
    - name: TZ
      value: Europe/Berlin
    resources:
      limits:
        cpu: ${cpuLimit}
        memory: ${memoryLimit}Gi
      requests:
        cpu: ${cpuLimit}
        memory: ${memoryLimit}Gi
  """
}


pipeline {
  agent none
  options {
    buildDiscarder(logRotator(numToKeepStr: '5')) //, artifactNumToKeepStr: '30'
  }
  stages {
    stage('ASSEMBLY') {
      agent {
        kubernetes {
          yaml getMavenAgent()
        }
      }
      steps {
        container("maven"){
          sh '''
            java -version
            # Install dependencies
            curl -s -O https://deb.nodesource.com/node_14.x/pool/main/n/nodejs/nodejs_14.6.0-1nodesource1_amd64.deb
            dpkg -i nodejs_14.6.0-1nodesource1_amd64.deb
            npm set unsafe-perm true
            apt -qq update && apt install -y g++ make
          '''
          configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
            sh """
              mvn -s \$MAVEN_SETTINGS_XML -T\$LIMITS_CPU clean install source:jar -Pdistro,distro-ce,distro-wildfly,distro-webjar -DskipTests -Dmaven.repo.local=\$(pwd)/.m2 com.mycila:license-maven-plugin:check -B
            """
          }
          stash name: "platform-stash-runtime", includes: ".m2/org/camunda/**/*-SNAPSHOT/**", excludes: "**/qa/**,**/*qa*/**,**/*.zip,**/*.tar.gz"
          stash name: "platform-stash-qa", includes: ".m2/org/camunda/bpm/**/qa/**/*-SNAPSHOT/**,.m2/org/camunda/bpm/**/*qa*/**/*-SNAPSHOT/**", excludes: "**/*.zip,**/*.tar.gz"
          stash name: "platform-stash-distro", includes: ".m2/org/camunda/bpm/**/*-SNAPSHOT/**/*.zip,.m2/org/camunda/bpm/**/*-SNAPSHOT/**/*.tar.gz"
        }
      }
    }
    stage('h2 tests') {
      parallel {
        stage('engine-UNIT-h2') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('h2-db')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent(16)
            }
          }
          steps{
            container("maven"){
              //runMaven(true, false,'engine/', '-T\$LIMITS_CPU test -Pdatabase,h2')
            }
          }
        }
        stage('engine-UNIT-authorizations-h2') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('h2-db')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent(16)
            }
          }
          steps{
            container("maven"){
              //runMaven(true, false,'engine/', '-T\$LIMITS_CPU test -Pdatabase,h2,cfgAuthorizationCheckRevokesAlways')
            }
          }
        }
        stage('engine-rest-UNIT-jersey-2') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('rest')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              runMaven(true, false,'engine-rest/engine-rest/', 'clean install -Pjersey2')
            }
          }
        }
        stage('engine-rest-UNIT-resteasy3') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('rest')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              runMaven(true, false,'engine-rest/engine-rest/', 'clean install -Presteasy3')
            }
          }
        }
        stage('webapp-UNIT-h2') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('webapp')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              runMaven(true, false,'webapps/', 'clean test -Pdatabase,h2 -Dskip.frontend.build=true')
            }
          }
        }
        stage('engine-IT-tomcat-9-h2') {// TODO change it to `postgresql-96`
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('IT')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                runMaven(true, true, 'qa/', 'clean install -Ptomcat,h2,engine-integration')
              }
            }
          }
          post {
            always {
              junit testResults: '**/target/*-reports/TEST-*.xml', keepLongStdio: true
            }
          }
        }
        stage('webapp-IT-tomcat-9-h2') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('webapp', 'IT')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent() + getChromeAgent()
            }
          }
          steps{
            container("maven"){
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                runMaven(true, true,'qa/', 'clean install -Ptomcat,h2,webapps-integration')
              }
            }
          }
          post {
            always {
              junit testResults: '**/target/*-reports/TEST-*.xml', keepLongStdio: true
            }
          }
        }
        stage('webapp-IT-standalone-wildfly') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('webapp', 'IT')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent() + getChromeAgent()
            }
          }
          steps{
            container("maven"){
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                runMaven(true, true,'qa/', 'clean install -Pwildfly-vanilla,webapps-integration-sa')
              }
            }
          }
        }
        stage('camunda-run-IT') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('webapp', 'run', 'spring-boot')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent() + getChromeAgent()
            }
          }
          steps{
            container("maven"){
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                runMaven(true, true,'distro/run/', 'clean install -Pintegration-test-camunda-run')
              }
            }
          }
          post {
            always {
              junit testResults: '**/target/*-reports/TEST-*.xml', keepLongStdio: true
            }
          }
        }
        stage('spring-boot-starter-IT') {
          when {
            anyOf {
              branch 'hackdays-ya';
              allOf {
                changeRequest();
                expression {
                  withLabels('webapp', 'spring-boot')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent() + getChromeAgent()
            }
          }
          steps{
            container("maven"){
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                runMaven(true, true,'spring-boot-starter/', 'clean install -Pintegration-test-spring-boot-starter')
              }
            }
          }
          post {
            always {
              junit testResults: '**/target/*-reports/TEST-*.xml', keepLongStdio: true
            }
          }
        }
      }
    }
    stage('db tests + CE webapps IT + EE platform') {
      parallel {
        stage('engine-api-compatibility') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              runMaven(true, false,'engine/', 'clean verify -Pcheck-api-compatibility')
            }
          }
        }
        stage('engine-UNIT-plugins') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              runMaven(true, false,'engine/', 'clean test -Pcheck-plugins')
            }
          }
        }
        stage('webapp-UNIT-database-table-prefix') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              runMaven(true, false,'webapps/', 'clean test -Pdb-table-prefix')
            }
          }
        }
        stage('EE-platform-DISTRO-dummy') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
            }
          }
        }
      }
    }
  }
  post {
    changed {
      script {
        if (!agentDisconnected()){ 
          // send email if the slave disconnected
        }
      }
    }
    always {
      script {
        if (agentDisconnected()) {// Retrigger the build if the slave disconnected
          build job: currentBuild.projectName, propagate: false, quietPeriod: 60, wait: false
        }
      }
    }
  }
}

void runMaven(boolean runtimeStash, boolean distroStash, String directory, String cmd) {
  if (runtimeStash) unstash "platform-stash-runtime"
  if (distroStash) unstash "platform-stash-distro"
  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
    sh("export MAVEN_OPTS='-Dmaven.repo.local=\$(pwd)/.m2' && cd ${directory} && mvn -s \$MAVEN_SETTINGS_XML ${cmd} -B")
  }
}
void withLabels(String... labels) {
  for ( l in labels) {
    pullRequest.labels.contains(labelName)
  }
}
