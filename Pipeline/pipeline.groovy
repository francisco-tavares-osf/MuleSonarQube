
pipeline {
    agent any
    
    environment {
        GITHUB_TOKEN = credentials('github-token')
        SONAR_TOKEN = credentials('sonar-token')
        
        SONAR_TIMEOUT_MINUTES=1
    }
    stages {  
        stage('Load Configs'){
            steps{
                script{
                    def props = readProperties file: '/var/jenkins_home/workspace/jenkins-sonar-config.properties'
                    
                    env.PIPELINE_NAME = props['jenkins.pipelineName']
                    env.MAIN_BRANCH = props['git.mainBranchName']
                    env.REPOSITORY_URL = props['git.gitRepository']
                    env.API_PATH = props['git.apiPath']
                    env.CLONE_PATH = props['git.clonePath']
                    env.SONAR_BASE_KEY = props['sonar.baseKey']
                    env.SONAR_HOST = props['sonar.host.url']
                    env.SONAR_METRICS = props['sonar.sonarMetrics']
                    env.PYTHON_SCRIPT_NAME = props['script.name']
                }
            }
        }
        stage('Clone Repository') {
            steps {
                script {
                    cleanWs()
                    echo "Cloning repository ${env.REPOSITORY_URL}"
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh "git clone https://${GITHUB_TOKEN}${env.CLONE_PATH} repo"
                    }
                    dir('repo') {
                        
                        sh 'git reset --hard'
                        sh 'git clean -fdx' 
                        //lista todas as branches
                        sh "git branch -a"
                        sh "git checkout ${env.MAIN_BRANCH}"
                        
                        
                    }
                }
            }
        }
        
        stage('Fetch PRs') {
            steps {
                script {
                    dir('repo') {
                        echo "Getting PRs to main branch"
                       
                        withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                            def response = sh (
                                script: """curl -X GET -s -H "Authorization: Bearer ${GITHUB_TOKEN}" \\
                                           -H "Accept: application/vnd.github+json" \\
                                           ${env.API_PATH}""",
                                returnStdout: true
                            ).trim()
        
                            echo "API Response: ${response}"
                            
                            def prs = readJSON text: response

                            if (prs && prs.size() > 0) {
                                echo "Processing PRs..."
                                
                                prs.each { pr -> 
                                
                                //Secure Hash Algorithm (sha) é um identifier, e essencial para criar branches e accuracy
                                    def prHash = pr.head.sha
                                    def prNum = pr.number
                                    def branchName = "pr-${prNum}-analysis"
                                    def projectName = "Mule-Sonar-${prNum}"
                                    def projectKey = generateProjectKey(projectName, prNum)

                                    echo "Processing PR #${prNum} (${prHash})"

                                    //Cria um branch para cada PR
                                    sh "git checkout -b ${branchName} ${prHash}" 
                                  
                                    // Chama a função analyzeMuleProject com os diretórios encontrados
                                    analyzeMuleProject(prNum, prHash, projectName,projectKey)
                                    
                                 
                                    //Volta para branch principal
                                    sh "git checkout ${env.MAIN_BRANCH}"
                                    //Elimina o branch criado do PR
                                    sh "git branch -D ${branchName}"
                                }
                            } else {
                                echo "Nenhum PR encontrado para análise"
                            }
                        }
                    }
                        
                }
            }
        }
    }
}
def generateProjectKey(projectName, prNum) {
    def sanitizedName = projectName.toLowerCase().replaceAll(/[^a-z0-9-_]/, '-')
    return "${env.SONAR_BASE_KEY}:${sanitizedName}:pr-${prNum}"
}

def analyzeMuleProject(prNum, prHash, projectName, projectKey) {
    stage("PR #${prNum} analysis") {
        // Copia o script Python para o local desejado
        sh "mv /var/jenkins_home/${env.PYTHON_SCRIPT_NAME} /var/jenkins_home/workspace/${env.PIPELINE_NAME}/repo/${env.PYTHON_SCRIPT_NAME}"
        
        // Executa o script Python
        sh "python3 /var/jenkins_home/workspace/${env.PIPELINE_NAME}/repo/${env.PYTHON_SCRIPT_NAME}"
        
        // Remove o script Python após a execução
        sh "mv /var/jenkins_home/workspace/${env.PIPELINE_NAME}/repo/${env.PYTHON_SCRIPT_NAME} /var/jenkins_home/${env.PYTHON_SCRIPT_NAME}"
        sh "cat /var/jenkins_home/workspace/${env.PIPELINE_NAME}/repo/sonar-structure-rules-issues.json"
        withSonarQubeEnv('SonarQube') {

           sh """
                sonar-scanner \\
                -Dsonar.projectKey=${projectKey}-structure \\
                -Dsonar.projectName=${projectName}-structure \\
                -Dsonar.projectVersion='PR-${prNum}' \\
                -Dsonar.sources=. \\
                -Dsonar.externalIssuesReportPaths=sonar-structure-rules-issues.json \\
                -Dsonar.host.url=${env.SONAR_HOST} \\
                -Dsonar.login=${SONAR_TOKEN} \\
                -Dsonar.sourceEncoding=UTF-8 \\
                -Dsonar.exclusions=target/**,**/*.jar \\
                -Dsonar.modules.recalculateRoot=true \\
                -Dsonar.mule.app=true \\
                -Dsonar.language=mule \\
                -Dsonar.verbose=true \\
                -X

                rm /var/jenkins_home/workspace/${env.PIPELINE_NAME}/repo/sonar-structure-rules-issues.json
            """
        }


        
        stage('Quality Gate'){
            timeout(time: SONAR_TIMEOUT_MINUTES, unit: 'MINUTES') {
                def qualityGate = waitForQualityGate()
                if (qualityGate.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qualityGate.status}"
                }
            }
        }
        
        def sonarResults = sh(script: """
            curl -s -u ${SONAR_TOKEN}: "${env.SONAR_HOST}/api/measures/component?component=${projectKey}&metricKeys=${env.SONAR_METRICS}"
        """, returnStdout: true).trim()
        
        def prInfo = """
        {
           "pr_number": "${prNum}",
           "commit_hash": "${prHash}",
           "project_name": "${projectName}",
           "project_key": "${projectKey}",
           "analysis_timestamp": "${new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone('UTC'))}",
           "sonar_results": ${sonarResults}
        }
        """
        echo "${sonarResults}"
        
        echo "Project ${projectName} with PR number #${prNum} done and results sent."
    }
}           