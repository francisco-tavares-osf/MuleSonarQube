def getProjectName() {
    if (fileExists('mule-artifact.json')) {
        def artifactJson = readJSON file: 'mule-artifact.json'
        return artifactJson.name ?: "unknown-mule-project"
    } else if (fileExists('pom.xml')) {
        def pomContent = readFile 'pom.xml'
        def match = pomContent =~ /<artifactId>(.*?)<\/artifactId>/
        if (match) {
            return match[0][1]
        }
    }
    return "unknown-project"
}

def generateProjectKey(projectName, prNum) {
    def sanitizedName = projectName.toLowerCase().replaceAll(/[^a-z0-9-_]/, '-')
    return "${SONAR_BASE_KEY}:${sanitizedName}:pr-${prNum}"
}

def analyzeMuleProject(prNum, prHash, projectName, projectKey) {
    stage("PR #${prNum} analysis") {
        withSonarQubeEnv('SonarQube') {
            sh """
                sonar-scanner \\
                -Dsonar.projectKey=${projectKey} \\
                -Dsonar.projectName=${projectName} \\
                -Dsonar.projectVersion='PR-${prNum}' \\
                -Dsonar.sources=. \\
                -Dsonar.host.url=${SONAR_HOST} \\
                -Dsonar.login=${SONAR_TOKEN} \\
                -Dsonar.sourceEncoding=UTF-8 \\
                -Dsonar.exclusions=target/**,**/*.jar \\
                -Dsonar.modules.recalculateRoot=true \\
                -Dsonar.mule.app=true \\
                -Dsonar.language=mule
            """
        }
        timeout(time: SONAR_TIMEOUT_MINUTES, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: false
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
    
        timeout(time: MULESOFT_TIMEOUT, unit: 'SECONDS') {
            sh """
                curl -X POST \\
                -H 'Content-Type: application/json' \\
                -d '${prInfo}' \\
                ${MULE_ENDPOINT}
            """
        }
        echo "Project ${projectName} with PR number #${prNum} done and results sent."
    }
}

pipeline {
    agent any
    
    environment {
        MAIN_BRANCH = 'main'
        REPOSITORY_URL = 'https://github.com/francisco-tavares-osf/MuleSonarQube.git'
        GITHUB_TOKEN = credentials('github-token')
        SONAR_TOKEN = credentials('sonarqube-token')

        SONAR_HOST = 'http://localhost:9000' 
        
        MULE_ENDPOINT = 'http://localhost:8081/caminho'
        
        SONAR_METRICS = 'bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,reliability_rating,security_rating'
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
                                script: """curl -s -H "Authorization: Bearer ${GITHUB_TOKEN}" \\
                                           -H "Accept: application/vnd.github+json" \\
                                           https://api.github.com/repos/francisco-tavares-osf/MuleSonarQube/pulls""",
                                returnStdout: true
                            ).trim()
                            
                            echo "API Response: ${response}"
                            
                            def prs = readJSON text: response

                            if (prs && prs.size() > 0) {
                                echo "Processing PRs..."
                                
                                prs.each { pr -> 
                                    def prHash = pr.head.sha
                                    def prNum = pr.number
                                    def branchName = "pr-${prNum}-analysis"
                                    def projectName = getProjectName()
                                    def projectKey = generateProjectKey(projectName, prNum)
                                    
                                    echo "Processing PR #${prNum} (${prHash})"
                                    
                                    sh "git checkout -b ${branchName} ${prHash}"

                                    analyzeMuleProject(prNum, prHash, projectName, projectKey)
                                    
                                    sh "git checkout ${MAIN_BRANCH}"
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
            