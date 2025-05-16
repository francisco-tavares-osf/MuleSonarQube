FROM jenkins/jenkins:lts-jdk17

USER root

# Instalar dependências necessárias
RUN apt-get update && apt-get install -y git unzip wget python3 python3-pip && \
    ln -s /usr/bin/python3 /usr/bin/python
# Baixar e instalar o SonarQube Scanner
RUN wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-5.0.1.3006-linux.zip -O /tmp/sonar-scanner.zip && \
    unzip /tmp/sonar-scanner.zip -d /opt && \
    mv /opt/sonar-scanner-5.0.1.3006-linux /opt/sonar-scanner && \
    ln -s /opt/sonar-scanner/bin/sonar-scanner /usr/local/bin/sonar-scanner && \
    rm /tmp/sonar-scanner.zip

USER jenkins

