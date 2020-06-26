pipeline {
    parameters { 
        string(name: 'gitRef', defaultValue: 'master', description: 'Git branch, tag, or commitId')
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
        timeout(time: 1, unit: 'MINUTES')
        ansiColor('xterm')
    }
    agent none
    stages {
        stage('load library') {
            steps {
                library "jenkins-global-library@${params.gitRef}"
            }
        }
        stage ('env') {
            agent {
                docker { 
                    image 'ubuntu:20.04'
                }
            }
            steps {
                sh 'env | sort'
            }
        }
    }
}

def agentEnv = agentContainer.fromRepo('ci/Dockerfile.ci')
