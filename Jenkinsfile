#!/usr/bin/env groovy

String POSTGRES_DB_CONFIG = '-Ddatabase.url=jdbc:postgresql://localhost:5432/process-engine -Ddatabase.username=camunda -Ddatabase.password=camunda'
String MARIADB_DB_CONFIG = '-Ddatabase.url=jdbc:mariadb://localhost:3306/process-engine -Ddatabase.username=camunda -Ddatabase.password=camunda'
String PG_96 = '9.6.18'
String MDB_102 = '10.2.33'
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

String getPostgresAgent(String dockerTag = '9.6.18', Integer cpuLimit = 1){
  String memoryLimit = cpuLimit * 2;
  """
  - name: postgres
    image: postgres:${dockerTag}
    env:
    - name: TZ
      value: Europe/Berlin
    - name: POSTGRES_DB
      value: process-engine
    - name: POSTGRES_USER
      value: camunda
    - name: POSTGRES_PASSWORD
      value: camunda
    resources:
      limits:
        cpu: ${cpuLimit}
        memory: ${memoryLimit}Gi
      requests:
        cpu: ${cpuLimit}
        memory: ${memoryLimit}Gi
  """
}
String getMariaDbAgent(String dockerTag = '10.2', Integer cpuLimit = 1){
  String memoryLimit = cpuLimit * 2;
  """
  - name: mariadb
    image: mariadb:${dockerTag}
    env:
    - name: TZ
      value: Europe/Berlin
    - name: MYSQL_DATABASE
      value: process-engine
    - name: MYSQL_USER
      value: camunda
    - name: MYSQL_PASSWORD
      value: camunda
    resources:
      limits:
        cpu: ${cpuLimit}
        memory: ${memoryLimit}Gi
      requests:
        cpu: ${cpuLimit}
        memory: ${memoryLimit}Gi
  """
}


pipeline{
  agent none
  stages{
    stage("Compile all the things!"){
      agent {
        kubernetes {
          yaml getMavenAgent()
        }
      }
      steps{
        container("maven"){
          // Install dependencies
          sh '''
            curl -s -O https://deb.nodesource.com/node_14.x/pool/main/n/nodejs/nodejs_14.6.0-1nodesource1_amd64.deb
            dpkg -i nodejs_14.6.0-1nodesource1_amd64.deb
            npm set unsafe-perm true
            apt -qq update && apt install -y g++ make
          '''
          configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
            sh """
              mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU clean source:jar install -D skipTests -Dmaven.repo.local=\$(pwd)/.m2
            """
          }
          stash name: "artifactStash", includes: ".m2/org/camunda/**/*-SNAPSHOT/**", excludes: "**/*.zip,**/*.tar.gz"
        }
      }
    }
    stage("Top Level Components"){
      agent {
        kubernetes {
          yaml getMavenAgent()
        }
      }
      stages {
        stage('Unstash') {
          steps{
            container("maven"){
              unstash "artifactStash"
            }
          }
        }
        stage('Top Level Components Tests') {
          failFast true
          parallel {
            stage('XML model') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd model-api/xml-model && mvn -s \$MAVEN_SETTINGS_XML -B test
                    """
                  }
                }
              }
            }
            stage('BPMN model') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd model-api/bpmn-model && mvn -s \$MAVEN_SETTINGS_XML -B test
                    """
                  }
                }
              }
            }
            stage('DMN model') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd model-api/dmn-model && mvn -s \$MAVEN_SETTINGS_XML -B test
                    """
                  }
                }
              }
            }
            stage('CMMN model') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd model-api/cmmn-model && mvn -s \$MAVEN_SETTINGS_XML -B test
                    """
                  }
                }
              }
            }
            stage('camunda-commons-typed-values tests') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd typed-values && mvn -s \$MAVEN_SETTINGS_XML -B test
                    """
                  }
                }
              }
            }
            stage('DMN engine tests') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-dmn && mvn -s \$MAVEN_SETTINGS_XML -B verify
                    """
                  }
                }
              }
            }
            stage('sql-scripts') {
              agent {
                kubernetes {
                  yaml getMavenAgent()
                }
              }
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd distro/sql-script && mvn -s \$MAVEN_SETTINGS_XML -B install -Pcheck-sql,h2
                    """
                  }
                }
              }
            }
          }
        }
      }
    }
    stage('Engine UNIT & QA Tests') {
      failFast true
      parallel {
        stage('Engine UNIT & Authorization tests - H2') {
          agent {
            kubernetes {
              yaml getMavenAgent(16)
            }
          }
          stages {
            stage('Engine UNIT tests') {
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU test -Pdatabase,h2-in-memory
                    """
                  }
                }
              }
            }
            stage("Engine UNIT: Authorizations Tests") {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,h2-in-memory,cfgAuthorizationCheckRevokesAlways -B -T\$LIMITS_CPU
                    """
                  }
                }
              }
            }
          }
        }
        stage('Engine UNIT & Authorization tests - PostgreSQL 9.6') {
          agent {
            kubernetes {
              yaml getMavenAgent(16) + getPostgresAgent(PG_96)
            }
          }
          stages {
            stage('Engine UNIT tests') {
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU test -Pdatabase,postgresql ${POSTGRES_DB_CONFIG}
                    """
                  }
                }
              }
            }
            stage("Engine UNIT: Authorizations Tests") {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,postgresql,cfgAuthorizationCheckRevokesAlways ${POSTGRES_DB_CONFIG} -B -T\$LIMITS_CPU
                    """
                  }
                }
              }
            }
          }
        }
        stage('Engine UNIT & Authorization tests - MariaDB 10.2') {
          agent {
            kubernetes {
              yaml getMavenAgent(16) + getMariaDbAgent(MDB_102)
            }
          }
          stages {
            stage('Engine UNIT tests') {
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU test -Pdatabase,mariadb ${MARIADB_DB_CONFIG}
                    """
                  }
                }
              }
            }
            stage("Engine UNIT: Authorizations Tests") {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,mariadb,cfgAuthorizationCheckRevokesAlways ${MARIADB_DB_CONFIG} -B -T\$LIMITS_CPU
                    """
                  }
                }
              }
            }
          }
        }
        stage("Engine UNIT: History Level Activity Tests") {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd engine/ && mvn -s \$MAVEN_SETTINGS_XML verify -Pcfghistoryactivity -B
                """
              }
            }
          }
        }
        stage("Engine UNIT: History Level Audit Tests") {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd engine/ && mvn -s \$MAVEN_SETTINGS_XML verify -Pcfghistoryaudit -B
                """
              }
            }
          }
        }
        stage("Engine UNIT: History Level None Tests") {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd engine/ && mvn -s \$MAVEN_SETTINGS_XML verify -Pcfghistorynone -B
                """
              }
            }
          }
        }
        stage('Engine UNIT: DB-Table-Prefix tests') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd engine/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdb-table-prefix -B
                """
              }
            }
          }
        }
        stage('Engine UNIT: CDI Integration / Plugins / Spring Integration Tests') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          stages {
            stage("Engine UNIT: CDI Integration Tests") {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-cdi/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,h2-in-memory -B
                    """
                  }
                }
              }
            }
            stage("Engine UNIT: Plugins Tests") {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-plugins/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,h2-in-memory -B
                    """
                  }
                }
              }
            }
            stage("Engine UNIT: Spring Integration Tests") {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-spring/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,h2-in-memory -B
                    """
                  }
                }
              }
            }
          }
        }
        stage('Engine UNIT: Plugins-enabled Engine UNIT tests') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd engine/ && mvn -s \$MAVEN_SETTINGS_XML test -Pcheck-plugins -B
                """
              }
            }
          }
        }
        stage('QA: Instance Migration & Rolling Update Tests - H2') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          stages {
            stage('QA: Instance Migration Tests') {
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd qa/test-db-instance-migration && mvn -s \$MAVEN_SETTINGS_XML -B verify -Pinstance-migration,h2
                    """
                  }
                }
              }
            }
            stage('QA: Rolling Update Tests') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd qa/test-db-rolling-update && mvn -s \$MAVEN_SETTINGS_XML -B verify -Prolling-update,h2
                    """
                  }
                }
              }
            }
          }
        }
        stage('QA: Instance Migration & Rolling Update Tests - PostgreSQL 9.6') {
          agent {
            kubernetes {
              yaml getMavenAgent() + getPostgresAgent(PG_96)
            }
          }
          stages {
            stage('QA: Instance Migration Tests') {
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd qa/test-db-instance-migration && mvn -s \$MAVEN_SETTINGS_XML -B verify -Pinstance-migration,postgresql ${POSTGRES_DB_CONFIG}
                    """
                  }
                }
              }
            }
            stage('QA: Rolling Update Tests') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd qa/test-db-rolling-update && mvn -s \$MAVEN_SETTINGS_XML -B verify -Prolling-update,postgresql ${POSTGRES_DB_CONFIG}
                    """
                  }
                }
              }
            }
          }
        }
        stage('QA: Instance Migration & Rolling Update Tests - MariaDB 10.2') {
          agent {
            kubernetes {
              yaml getMavenAgent() + getMariaDbAgent(MDB_102)
            }
          }
          stages {
            stage('QA: Instance Migration Tests') {
              steps{
                container("maven"){
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd qa/test-db-instance-migration && mvn -s \$MAVEN_SETTINGS_XML -B verify -Pinstance-migration,mariadb ${MARIADB_DB_CONFIG}
                    """
                  }
                }
              }
            }
            stage('QA: Rolling Update Tests') {
              steps{
                container("maven"){
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd qa/test-db-rolling-update && mvn -s \$MAVEN_SETTINGS_XML -B verify -Prolling-update,mariadb ${MARIADB_DB_CONFIG}
                    """
                  }
                }
              }
            }
          }
        }
        stage('QA: Upgrade old engine from 7.13 - H2') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pold-engine,h2
                """
              }
            }
          }
        }
        stage('QA: Upgrade old engine from 7.13 - PostgreSQL 9.6') {
          agent {
            kubernetes {
              yaml getMavenAgent() + getPostgresAgent(PG_96)
            }
          }
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pold-engine,postgresql ${POSTGRES_DB_CONFIG}
                """
              }
            }
          }
        }
        stage('QA: Upgrade old engine from 7.13 - MariaDB 10.2') {
          agent {
            kubernetes {
              yaml getMavenAgent() + getMariaDbAgent(MDB_102)
            }
          }
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pold-engine,mariadb ${MARIADB_DB_CONFIG}
                """
              }
            }
          }
        }
        stage('QA: Upgrade database from 7.13 - H2') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/test-db-upgrade && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pupgrade-db,h2
                """
              }
            }
          }
        }
        stage('QA: Upgrade database from 7.13 - PosgreSQL 9.6') {
          agent {
            kubernetes {
              yaml getMavenAgent() + getPostgresAgent(PG_96)
            }
          }
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/test-db-upgrade && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pupgrade-db,postgresql ${POSTGRES_DB_CONFIG}
                """
              }
            }
          }
        }
        stage('QA: Upgrade database from 7.13 - MariaDB 10.2') {
          agent {
            kubernetes {
              yaml getMavenAgent() + getMariaDbAgent(MDB_102)
            }
          }
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/test-db-upgrade && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pupgrade-db,mariadb ${MARIADB_DB_CONFIG}
                """
              }
            }
          }
        }
      }
    }
    stage("Rest API & Webapps Tests"){
      when {
        anyOf {
          branch 'hackdays-master';
          allOf {
            changeRequest();
            expression {
              pullRequest.labels.contains('rest-api')
            }
          }
        }
      }
      failFast true
      parallel {
        stage('Rest API - Jersey2') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          stages {
            stage('Rest API UNIT Jersey2 tests') {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-rest/engine-rest/ && mvn -s \$MAVEN_SETTINGS_XML test -Pjersey2 -B
                    """
                  }
                }
              }
            }
            stage("Rest API JAX-RS2 Jersey2 tests") {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-rest/engine-rest-jaxrs2/ && mvn -s \$MAVEN_SETTINGS_XML test -Pjersey2 -B
                    """
                  }
                }
              }
            }
          }
        }
        stage('Rest API - Resteasy / Resteasy3') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          stages {
            stage('Rest API UNIT Resteasy tests') {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-rest/engine-rest/ && mvn -s \$MAVEN_SETTINGS_XML test -Presteasy -B
                    """
                  }
                }
              }
            }
            stage('Rest API UNIT Resteasy3 tests') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-rest/engine-rest/ && mvn -s \$MAVEN_SETTINGS_XML test -Presteasy3 -B
                    """
                  }
                }
              }
            }
            stage("Rest API JAX-RS2 Resteasy3 tests") {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-rest/engine-rest-jaxrs2/ && mvn -s \$MAVEN_SETTINGS_XML test -Presteasy3 -B
                    """
                  }
                }
              }
            }
          }
        }
        stage('Rest API - CXF / Wink') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          stages {
            stage('Rest API UNIT CXF tests') {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-rest/engine-rest/ && mvn -s \$MAVEN_SETTINGS_XML test -Pcxf -B
                    """
                  }
                }
              }
            }
            stage('Rest API UNIT Wink tests') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd engine-rest/engine-rest/ && mvn -s \$MAVEN_SETTINGS_XML clean test -Pwink -B
                    """
                  }
                }
              }
            }
          }
        }
        stage('WLS-compatibility tests') {
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  mvn -s \$MAVEN_SETTINGS_XML verify -Pcheck-engine,wls-compatibility,jersey -B
                """
              }
            }
          }
        }
        stage('Wildfly-compatibility tests') {
          when {
            anyOf {
              branch 'hackdays-master';
              allOf {
                changeRequest();
                expression {
                  pullRequest.labels.contains('h2')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd engine-rest/engine-rest/ && mvn -s \$MAVEN_SETTINGS_XML test -Pwildfly-compatibility,resteasy -B
                """
              }
            }
          }
        }
        stage('Webapp - H2') {
          when {
            anyOf {
              branch 'hackdays-master';
              allOf {
                changeRequest();
                expression {
                  pullRequest.labels.contains('h2')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent()
            }
          }
          stages {
            stage('Webapp UNIT tests') {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,h2-in-memory -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
            stage('Webapp UNIT: DB-Table-prefix tests') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdb-table-prefix -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
            stage('Webapp UNIT: Authorizations tests') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,h2-in-memory,cfgAuthorizationCheckRevokesAlways -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
          }
        }
        stage('Webapp - PostgreSQL 9.6') {
          when {
            anyOf {
              branch 'hackdays-master';
              allOf {
                changeRequest();
                expression {
                  pullRequest.labels.contains('postgresql')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent() + getPostgresAgent(PG_96)
            }
          }
          stages {
            stage('Webapp UNIT tests') {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,postgresql ${POSTGRES_DB_CONFIG} -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
            stage('Webapp UNIT: Authorizations tests') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,postgresql,cfgAuthorizationCheckRevokesAlways ${POSTGRES_DB_CONFIG} -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
          }
        }
        stage('Webapp - MariaDB 10.2') {
          when {
            anyOf {
              branch 'hackdays-master';
              allOf {
                changeRequest();
                expression {
                  pullRequest.labels.contains('mariadb')
                }
              }
            }
          }
          agent {
            kubernetes {
              yaml getMavenAgent() + getMariaDbAgent(MDB_102)
            }
          }
          stages {
            stage('Webapp UNIT tests') {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,mariadb ${MARIADB_DB_CONFIG} -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
            stage('Webapp UNIT: Authorizations tests') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,mariadb,cfgAuthorizationCheckRevokesAlways ${MARIADB_DB_CONFIG} -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
          }
        }
          stages {
            stage('Webapp UNIT tests') {
              steps {
                container("maven") {
                  unstash "artifactStash"
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,mysql ${MYSQL_DB_CONFIG} -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
            stage('Webapp UNIT: Authorizations tests') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd webapps/ && mvn -s \$MAVEN_SETTINGS_XML test -Pdatabase,mysql,cfgAuthorizationCheckRevokesAlways ${MYSQL_DB_CONFIG} -Dskip.frontend.build=true -B
                    """
                  }
                }
              }
            }
          }
        }
      }
    }
    stage("JDK Tests") {
      failFast true
      parallel {
        // There are no Maven Docker Images with Oracle JDK versions.
        // We will probably need to provide our own custom OracleJDK-Maven Docker Images
        stage('OpenJDK 8 tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-8')
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU install source:jar source:test-jar -Pdistro,distro-ce,distro-wildfly -B
                """
              }
            }
          }
        }
        stage('OpenJDK 11 tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-11')
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU install source:jar source:test-jar -pl '!distro/jbossas7/subsystem' -Pdistro,distro-ce,distro-wildfly -B
                """
              }
            }
          }
        }
        stage('OpenJDK 14 tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-14')
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU install source:jar source:test-jar -pl '!distro/jbossas7/subsystem' -Pdistro,distro-ce,distro-wildfly -B
                """
              }
            }
          }
        }
        stage('IBM JDK 8 tests') {
          agent {
            kubernetes {
              yaml getMavenAgent(3, '3.6.3-ibmjava-8')
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU install source:jar source:test-jar -Pdistro,distro-ce,distro-wildfly -B
                """
              }
            }
          }
        }
      }
    }
    stage("Engine & Webapps IT Tests") {
      failFast true
      parallel {
        stage('Engine IT: Tomcat tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-8') + getPostgresAgent(PG_96)
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/ && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Ptomcat,postgresql,engine-integration ${POSTGRES_DB_CONFIG} -B
                """
              }
            }
          }
        }
        stage('Engine IT: Wildfly tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-8') + getPostgresAgent(PG_96)
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/ && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pwildfly,postgresql,engine-integration ${POSTGRES_DB_CONFIG} -B
                """
              }
            }
          }
        }
        stage('Engine IT: Wildfly XA tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-8') + getPostgresAgent(PG_96)
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/ && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pwildfly,postgresql,postgresql-xa,engine-integration ${POSTGRES_DB_CONFIG} -B
                """
              }
            }
          }
        }
        stage('Engine IT: Wildfly Domain tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-8')
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/ && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pwildfly-domain,h2,engine-integration -B
                """
              }
            }
          }
        }
        stage('Engine IT: Wildfly Servlet tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-8')
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa/ && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU verify -Pwildfly,wildfly-servlet,h2,engine-integration -B
                """
              }
            }
          }
        }
        stage('Distro: Wildfly Subsystem UNIT tests') {
          agent {
            kubernetes {
              yaml getMavenAgent( 3, '3.6.3-openjdk-8')
            }
          }
          steps {
            container("maven") {
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd distro/wildfly/subsystem && mvn -s \$MAVEN_SETTINGS_XML -B -T\$LIMITS_CPU test -B
                """
              }
            }
          }
        }
      }
    }
    stage("Webapp IT"){
      agent {
        kubernetes {
          yaml getMavenAgent()
        }
      }
      stages {
        stage('Build Distro') {
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd distro/license-book && mvn -s \$MAVEN_SETTINGS_XML install -B
                  cd ../tomcat/assembly && mvn -s \$MAVEN_SETTINGS_XML install -B
                  cd ../../wildfly/assembly && mvn -s \$MAVEN_SETTINGS_XML install -B
                """
              }
            }
          }
        }
        stage('webapp-IT-tomcat-h2') {
          steps {
            container("maven") {
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa && mvn -s \$MAVEN_SETTINGS_XML install -Ptomcat,h2,webapps-integration -B
                """
              }
            }
          }
        }
        stage('webapp-IT-standalone-tomcat') {
          steps {
            container("maven") {
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd qa && mvn -s \$MAVEN_SETTINGS_XML install -Ptomcat-vanilla,webapps-integration-sa -B
                """
              }
            }
          }
        }
      }
    }
    stage("Webapp IT spring"){
      agent {
        kubernetes {
          yaml getMavenAgent() + getChromeAgent()
        }
      }
      stages {
        stage('Unstash 2 + license book') {
          steps{
            container("maven"){
              unstash "artifactStash"
              configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                sh """
                  export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                  cd distro/license-book && mvn -s \$MAVEN_SETTINGS_XML install -B
                """
              }
            }
          }
        }
        stage("Webapp IT spring tests") {
          parallel {
            stage('Run: IT') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd distro/run && mvn -s \$MAVEN_SETTINGS_XML install -B
                      mvn -s \$MAVEN_SETTINGS_XML install -Pintegration-test-camunda-run -B
                    """
                  }
                }
              }
            }
            stage('Spring Boot Starter: IT') {
              steps {
                container("maven") {
                  configFileProvider([configFile(fileId: 'maven-nexus-settings', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh """
                      export MAVEN_OPTS="-Dmaven.repo.local=\$(pwd)/.m2"
                      cd spring-boot-starter && mvn -s \$MAVEN_SETTINGS_XML install -B
                      mvn -s \$MAVEN_SETTINGS_XML install -Pintegration-test-spring-boot-starter -B
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