## MuleSonarQube
MuleSoft code validation tool integrated with Jenkins and SonarQube.


# Overview

This repository contains all the necessary files to analyze MuleSoft projects. The project is organized into folders to enhance navigability. Key files include:

 - Pipeline Folder: Contains pipeline.groovy, which includes the JenkinsFile for running the pipeline and the properties files that should be configured for each individual repository/project
 - Script Folder: Contains structureScript.py, which validates the project's structure and naming conventions for files and folders.
 - Plugin Folder: rules developed in the mule-validation-sonarqube-plugin-1.0.6-mule.jar plugin are detailed in the rules-4.xml file.To update or implement new rules, modify this file and redeploy the plugin.

# Docker Setup
Docker and Docker Compose files are provided to build images and containers for SonarQube and Jenkins. These files are located in the Docker folder.

