#!/usr/bin/env groovy

// https://github.com/camunda/jenkins-global-shared-library
// @Library('camunda-ci') _

String getMavenAgent(Integer mavenCpuLimit = 4){
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
    stage('ASSEMBLY') {
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
            # Install dependencies
            curl -s -O https://deb.nodesource.com/node_14.x/pool/main/n/nodejs/nodejs_14.6.0-1nodesource1_amd64.deb
            dpkg -i nodejs_14.6.0-1nodesource1_amd64.deb
            npm set unsafe-perm true
            apt -qq update && apt install -y g++ make
          '''
          configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
            sh """
              mvn -s \$MAVEN_SETTINGS_XML -T\$LIMITS_CPU clean source:jar install -D skipTests -Dmaven.repo.local=\$(pwd)/.m2  -B
            """
          }
          stash name: "platform-stash", includes: ".m2/org/camunda/bpm/**/*-SNAPSHOT/**", excludes: "**/*.zip,**/*.tar.gz"
        }
      }
    }
    stage('h2 tests') {
      failfast true
      parallel {
        stage('Engine UNIT & Authorization tests - H2') {
          agent {
            kubernetes {
              yaml getMavenAgent(16)
            }
          }
          stages {
            stage('engine-UNIT-h2') {
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU test -Pdatabase,h2
                    """
                  }
                }
              }
            }
          }
        }
      }
    }
  }
} 