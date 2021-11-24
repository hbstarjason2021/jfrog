pipeline {
    agent any
    stages {
        stage('Build') {
            agent {
                docker {
                    image 'maven:3.6.2-jdk-11'
                    args '-v /root/.m2:/root/.m2'
                    args '-v /var/run/docker.sock:/var/run/docker.sock'
                    //reuseNode true
                }
            }
            steps {
               sh 'mvn --version'
                
               //sh  'git clone https://gitee.com/starjason/springboot-devops-demo.git'
               git([url: 'https://gitee.com/starjason/springboot-devops-demo.git', branch: 'master'])
               
               //git([url: 'https://gitee.com/starjason/project-examples.git', branch: 'master'])
               //sh 'mvn clean package -Dmaven.test.skip=true -s ./settings.xml'
               
            //Jfrog    
        script {
            def server = Artifactory.server 'jfrog'
            def rtMaven = Artifactory.newMavenBuild()
            def buildInfo = Artifactory.newBuildInfo()
		    rtMaven.tool = 'maven36'
		          //  /opt/ci/jenkins/data/tools/hudson.tasks.Maven_MavenInstallation/maven3.6
         
        /**   
             def SONAR_HOST_URL = 'http://47.94.8.19:31854'
             def SONAR_PROJECT_KEY = "jfrog"
             def SONAR_SOURCES = '.'
             def surl="${SONAR_HOST_URL}/api/measures/component?componentKey=${SONAR_PROJECT_KEY}&metricKeys=alert_status,quality_gate_details,coverage,new_coverage,bugs,new_bugs,reliability_rating,vulnerabilities,new_vulnerabilities,security_rating,sqale_rating,sqale_index,sqale_debt_ratio,new_sqale_debt_ratio,duplicated_lines_density&additionalFields=metrics,periods"
             def response=httpRequest consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', ignoreSslErrors: true, url: surl
             def propssonar = readJSON text: response.content
                if (propssonar.component.measures) {
                    propssonar.component.measures.each{ measure ->
                        def val
                        if (measure.periods){
                            val = measure.periods[0].value
                        }else {
                            val = measure.value
                        }
                        rtMaven.deployer.addProperty("sonar.quality.${measure.metric}", val)
                    }
                }
        **/    
               //env.JRE_HOME = '/opt/jdk8/jre'
               //env.JAVA_HOME = '/opt/jdk8/'
            //env.JRE_HOME = '/usr/lib/jvm/java-8-openjdk-amd64/jre'
            //env.JAVA_HOME = '/usr/lib/jvm/java-8-openjdk-amd64/jre/'
            env.JAVA_HOME = '/usr/local/openjdk-11'
        
        //rtMaven.deployer releaseRepo: 'guide-maven-dev-local', snapshotRepo: 'guide-maven-dev-local', server: server
        //rtMaven.resolver releaseRepo: 'guide-maven-dev-local', snapshotRepo: 'guide-maven-dev-local', server: server
        rtMaven.deployer releaseRepo: 'guide-maven-dev-local', snapshotRepo: 'guide-maven-dev-local', server: server
        
        rtMaven.deployer.addProperty("unit-test", "pass").addProperty("qa-team", "zhang", "ling")
        
        rtMaven.deployer.addProperty("qulity.gate.sonarUrl", "http://47.94.8.19:31854/dashboard/index/jfrog")
        
        
        //rtMaven.resolver releaseRepo: 'guide-maven-remote', snapshotRepo: 'guide-maven-remote', server: server
        rtMaven.resolver releaseRepo: 'ali', snapshotRepo: 'ali', server: server
        buildInfo = Artifactory.newBuildInfo()

        rtMaven.run pom: 'pom.xml', goals: '-B clean install -Dmaven.test.failure.ignore=true', buildInfo: buildInfo
        
        rtMaven.run pom: 'pom.xml', goals: 'clean install sonar:sonar -Dsonar.projectName=jfrog-demo -Dsonar.projectKey=jfrog -Dsonar.host.url=http://47.94.8.19:31854 -Dsonar.login=08f17ad77a8ea3e6d24c25a40e6f2a59c9486c8d -Dsonar.sources=. -Dsonar.tests=. -Dsonar.test.inclusions=**/*Test*/** -Dsonar.exclusions=**/*Test*/**', buildInfo: buildInfo
        
        //rtMaven.run pom: 'pom.xml', goals: 'mvn clean package -Dmaven.test.skip=true -s ./settings.xml', buildInfo: buildInfo
        //rtMaven.run pom: 'maven-examples/maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
        server.publishBuildInfo buildInfo
                 
        //rtMaven.deployer.deployArtifacts  buildInfo 

       // rtMaven.deployer.addProperty("qulity.gate.sonarUrl", SONAR_HOST_URL + "/dashboard/index/" + SONAR_PROJECT_KEY) 
       
                } 

            }
        }
    }
}
