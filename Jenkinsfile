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
          '''
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
