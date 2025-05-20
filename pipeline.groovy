pipeline {
    agent any
    
    environment {
        MAIN_BRANCH = 'main'
        REPOSITORY_URL = 'https://github.com/francisco-tavares-osf/MuleSonarQube.git'
        GITHUB_TOKEN = credentials('github-token')
        SONAR_TOKEN = credentials('sonar-token')
        SONAR_HOST = 'http://sonarqubemule:9000' 
        SONAR_BASE_KEY = 'mule-projects'
        
        MULE_ENDPOINT = 'http://localhost:8081/caminho'
        MULESOFT_TIMEOUT = 1
        
        SONAR_METRICS = 'bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,reliability_rating,security_rating'
        SONAR_TIMEOUT_MINUTES=1
    }
        
    stages {
        stage('Clone Repository') {
            steps {
                script {
                    cleanWs()
                    echo "Cloning repository ${REPOSITORY_URL}"
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh "git clone https://${GITHUB_TOKEN}@github.com/francisco-tavares-osf/MuleSonarQube.git repo"
                    }
                    
                    dir('repo') {
                        //lista todas as branches
                        sh "git branch -a"
                        sh "git checkout ${MAIN_BRANCH}"
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
                                           https://api.github.com/repos/francisco-tavares-osf/MuleSonarQube/pulls""",
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

                                    //Procura diretorios que tenham o src/main/resources para ser usado dinamicamente para analise
                                    def sourceDirs = sh(script: "find /var/jenkins_home/workspace/SonarPipeline/repo -type d -path '*/src/main/resources' | paste -sd ',' -", returnStdout: true).trim()
                                    // Verifica se encontrou algum diretório
                                    if (sourceDirs) {
                                        // Chama a função analyzeMuleProject com os diretórios encontrados
                                        analyzeMuleProject(prNum, prHash, projectName,projectKey, sourceDirs)
                                    } else {
                                        echo "Nenhum diretório 'src/main/resources' encontrado para PR #${prNum}."
                                    }
                                 
                                    //Volta para branch principal
                                    sh "git checkout ${MAIN_BRANCH}"
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
    return "${SONAR_BASE_KEY}:${sanitizedName}:pr-${prNum}"
}

def analyzeMuleProject(prNum, prHash, projectName, projectKey,sourceDirs) {
    stage("PR #${prNum} analysis") {
        // Verifica o diretório de trabalho atual
        sh 'echo "Current directory:"'
        sh 'pwd'
        
        // Lista os arquivos e diretórios no diretório atual
        sh 'echo "Listing current directory contents:"'
        sh 'ls -lah'
        
        // Copia o script Python para o local desejado
        sh 'mv /var/jenkins_home/my_script.py /var/jenkins_home/workspace/SonarPipeline/repo/training4-american-ws/src/main/resources/my_script.py'
        
        sh 'ls -lah'

        // Executa o script Python
        sh 'python3 /var/jenkins_home/workspace/SonarPipeline/repo/training4-american-ws/src/main/resources/my_script.py'

        // Lista os arquivos e diretórios após a execução do script Python
        sh 'echo "Listing directory contents after script execution:"'
        sh 'ls -lah'
        
        // Remove o script Python após a execução
        sh 'mv /var/jenkins_home/workspace/SonarPipeline/repo/training4-american-ws/src/main/resources/my_script.py /var/jenkins_home/my_script.py'
        
        withSonarQubeEnv('SonarQube') {
            sh """
                echo "Current directory before sonar-scanner:"
                pwd
                
                echo "Listing directory contents before sonar-scanner:"
                ls -lah
                
                sonar-scanner \\
                -Dsonar.projectKey=${projectKey}-plugin \\
                -Dsonar.projectName=${projectName}-plugin \\
                -Dsonar.projectVersion='PR-${prNum}' \\
                -Dsonar.sources=. \\
                -Dsonar.host.url=${SONAR_HOST} \\
                -Dsonar.login=${SONAR_TOKEN} \\
                -Dsonar.sourceEncoding=UTF-8 \\
                -Dsonar.exclusions=target/**,**/*.jar \\
                -Dsonar.modules.recalculateRoot=true \\
                -Dsonar.mule.app=true \\
                -Dsonar.language=mule \\
                -Dsonar.verbose=true \\
                -X
            """
        }

        withSonarQubeEnv('SonarQube') {

            //  /var/jenkins_home/workspace/SonarPipeline/repo
            sh """
                echo "Current directory before sonar-scanner:"
                pwd
                
                
                echo "Listing directory contents before sonar-scanner:"
                ls -lah
                
                sonar-scanner \\
                -Dsonar.projectKey=${projectKey}-structure \\
                -Dsonar.projectName=${projectName}-structure \\
                -Dsonar.projectVersion='PR-${prNum}' \\
                -Dsonar.sources=${sourceDirs} \\
                -Dsonar.externalIssuesReportPaths=sonar-structure-rules-issues.json \\
                -Dsonar.host.url=${SONAR_HOST} \\
                -Dsonar.login=${SONAR_TOKEN} \\
                -Dsonar.sourceEncoding=UTF-8 \\
                -Dsonar.exclusions=target/**,**/*.jar \\
                -Dsonar.modules.recalculateRoot=true \\
                -Dsonar.mule.app=true \\
                -Dsonar.language=mule \\
                -Dsonar.verbose=true \\
                -X

                rm /var/jenkins_home/workspace/SonarPipeline/repo/sonar-structure-rules-issues.json
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
            curl -s -u ${SONAR_TOKEN}: "${SONAR_HOST}/api/measures/component?component=${projectKey}&metricKeys=${SONAR_METRICS}"
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
        
    /*
        timeout(time: MULESOFT_TIMEOUT, unit: 'SECONDS') {
            sh """
                curl -X POST \\
                -H 'Content-Type: application/json' \\
                -d '${prInfo}' \\
                ${MULE_ENDPOINT}
            """
        }
        */
        echo "Project ${projectName} with PR number #${prNum} done and results sent."
    }
}           