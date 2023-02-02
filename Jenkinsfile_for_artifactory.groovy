pipeline{
    
    agent { label "docker-t3micro" }
    
    options{
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        BUILD_NUM_ENV = currentBuild.getNumber()
    }

    stages{
        stage('Clean'){
            steps{
                cleanWs()
            }
        }
        stage('Git pull'){
            steps{
               sh '''
               git clone https://github.com/dhutsj/k8s-practice.git
               '''
            }
        }
        stage('Docker Build'){
            steps{
                dir("./k8s-practice/k8s_resource_yaml"){
                    script {
                        def server = Artifactory.newServer url: 'https://dev.artifactory.com/artifactory', username: 'tsj', password: 'xxx'
                        def buildInfo = Artifactory.newBuildInfo()
                        def rtDocker = Artifactory.docker server: server
                        def tagDockerApp = "docker-local.dev.artifactory.com/tsjtest:latest"
                        println "Docker App Build"
                        // docker.build(tagDockerApp)
                        sh '''
                        docker build -t docker-local.dev.artifactory.com/tsjtest:latest .
                        '''
                        println "Docker push" + tagDockerApp + " : " + "docker-local"
                        buildInfo = rtDocker.push(tagDockerApp, "docker-local", buildInfo)
                        println "Docker Buildinfo"
                        buildInfo.env.capture = true
                        buildInfo.env.collect()
                        server.publishBuildInfo buildInfo
                        echo "BUILD NUMBER is ${BUILD_NUM_ENV}"
                    }
                }
            }
        }
        stage('Xray Scan') {
            steps{
                 script {
                     def server = Artifactory.newServer url: 'https://dev.artifactory.com/artifactory', username: 'tsj', password: 'xxx'
                     def XRAY_SCAN = "YES"
                     if (XRAY_SCAN == "YES") {
                         def xrayConfig = [
                            'buildName'     : env.JOB_NAME,
                            'buildNumber'   : env.BUILD_NUM_ENV.toString(),
                            'failBuild'     : false
                          ]
                          def xrayResults = server.xrayScan xrayConfig
                          echo xrayResults as String
                     } else {
                          println "No Xray scan performed. To enable set XRAY_SCAN = YES"
                     }
                     sleep 6
                }
            }
        }
    }

    post{
        always{
            script{
                println("always")
            }
        }
        success{
            script{
                currentBuild.description = "\n Run success!"
            }
        }
        failure{
            script{
                currentBuild.description = "\n Run failed!"
            }
        }
        aborted{
            script{
                currentBuild.description = "\n Aborted job!"
            }
        }
    }
}
