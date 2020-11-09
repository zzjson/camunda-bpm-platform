#!/usr/bin/env groovy

// https://github.com/camunda/jenkins-global-shared-library
// @Library('camunda-ci') _

String getMavenAgent(Integer mavenCpuLimit = 4, String dockerTag = '3.2.5'){
  String mavenForkCount = mavenCpuLimit;
  String mavenMemoryLimit = mavenCpuLimit * 2;
  """
metadata:
  labels:
    agent: ci-cambpm
spec:
  nodeSelector:
    cloud.google.com/gke-nodepool: agents-n1-standard-32-netssd-preempt
  tolerations:
  - key: "agents-n1-standard-32-netssd-preempt"
    operator: "Exists"
    effect: "NoSchedule"
  containers:
  - name: maven
    image: maven
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


pipeline {
  agent none
  stages {
    stage('build') {
      agent {
        kubernetes {
          yaml getMavenAgent()
        }
      }
      steps {
        container("maven"){
          sh '''
            mvn --version
            java -version
          '''
        }
      }
    }
  }
}