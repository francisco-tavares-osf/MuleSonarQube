import os 
import re
import json

ISSUES = []

REQUIRED_FOLDERS = ['src/main/mule','src/test']
REQUIRED_FILES = ['pom.xml','README.md']
FILENAME_REGEX = r'^[a-z0-9]+(-[a-z0-9]+)\.xml' #kebab-case
PROJECT_ROOT = os.getcwd() 
PROJECT_NAME = os.path.basename(PROJECT_ROOT)

#Rules
RULES = [
    {
        "id" : "project-name-format",
        "name" : "Project name format",
        "description": "Project name must follow kebab-case naming convention",
        "engineId" : "custom-structure-check",
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MAJOR",
        "impacts": [
                {
                    "softwareQuality":"MAINTAINABILITY",
                    "severity": "MEDIUM"
                }     
        ]
    },
    {
        "id" : "main-app-name",
        "name" : "Main application XML name match",
        "description": "Main .xml file in src/main/mule must be named after the project folder",
        "engineId" : "custom-structure-check",
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MAJOR",
        "impacts": [
                {
                    "softwareQuality":"MAINTAINABILITY",
                    "severity": "MEDIUM"
                }     
        ]
    },
    {
        "id": "dataweave-modules-CamelCase",
        "name": "DataWeave modules CamelCase",
        "description": "DataWeave files in modules must be in CamelCase",
        "engineId": "custom-structure-check",
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MINOR",
        "impacts": [
            {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "LOW"
            }
        ]
    },
    {
        "id": "dataweave-kebab-case",
        "name": "DataWeave files kebab-case",
        "description": "DataWeave files must be in kebab-case",
        "engineId": "custom-structure-check",
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MINOR",
        "impacts": [
            {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "LOW"
            }
        ]
    },
    {
        "id": "yaml-kebab-case",
        "name": "YAML files kebab-case",
        "description": "YAML files must be in kebab-case",
        "engineId": "custom-structure-check",
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MINOR",
        "impacts": [
            {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "LOW"
            }
        ]
    },
    {
        "id": "json-example-format",
        "name": "JSON example format",
        "description": "Example JSON files must follow the naming convention",
        "engineId": "custom-structure-check",
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MINOR",
        "impacts": [
            {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "LOW"
            }
        ]
    },
    {
        "id": "main-raml-name",
        "name": "Main RAML name match",
        "description": "Main RAML file must have the same name as the project",
        "engineId": "custom-structure-check",
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MAJOR",
        "impacts": [
            {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "MEDIUM"
            }
        ]
    },
    {  
        "id": "resource-location",
        "name": "Resource files location",
        "description": "Ensure 'src/main/resources' directory exists for DataWeave, YAML, JSON, and RAML files",
        "engineId": "custom-structure-check",            
        "cleanCodeAttribute": "IDENTIFIABLE",
        "type": "CODE_SMELL",
        "severity": "MAJOR",
        "impacts": [
            {
                "softwareQuality": "RELIABILITY",
                "severity": "HIGH"
            }
        ]
    }
]


def add_issue(file_path, line, message,rule_id):
    ISSUES.append({
        "ruleId" : rule_id,
        "primaryLocation": {
            "message" : message,
            "filePath" : file_path,
            "textRange" : {
                "startLine" : line,
                "endLine": line
            }
        }
    })

#Rule 1 : Projeto deve estar em kebab-case com formato {project-identifier}-{context}-{name}-{layer}-{identifier}
def check_project_name():
    expected_name_pattern= r'^[a-z0-9]+(-[a-z0-9]+)*-[a-z0-9]+-[a-z0-9]+(-[a-z0-9]+)*(-[a-z0-9]+)?+oel'
    if not re.match(expected_name_pattern,PROJECT_NAME):
        add_issue("project",1,f"Project name '{PROJECT_NAME}' is not the expected kebab-case format","project-name-format")

#Rule 2 : Main app deve ter o mesmo nome que o nome do projeto mais ".xml"
def check_main_app_filename():
    expected_file = PROJECT_NAME + ".xml"
    main_path = os.path.join(PROJECT_ROOT,"src","main","mule",expected_file)
    if not os.path.isfile(main_path):
        add_issue("src/main/mule/repo.xml",1,f"Main application file '{expected_file}' not found in src/main/mule", "main-app-name")

#Rule 3 : DataWeave files
def check_dataweave_files():
    base_dir = os.path.join(PROJECT_ROOT, "src","main","resources")
   # if not os.path.isdir(base_dir):
    #    add_issue("src/main/resources/",1,"Missing 'src/main/resources' directory for DataWeave files","resource-location")

    #Iterates through all folders and subfolders from the /src/main/resources .
    for root,_,files in os.walk(base_dir):
        #Iterates each file found in the directory
        for file in files:
            if file.endswith(".dwl"):
                relative_path = os.path.relpath(root,PROJECT_ROOT).replace(os.sep,"/")
                if relative_path.startswith("src/main/resources/modules"):
                    if not re.match(r'^[a-z0-9]+(-[a-z0-9]+)*\.(yaml|yml)$',file):
                        add_issue(os.path.join(root,file),1,f"DataWeave file '{file}' in modules must be in CamelCase","dataweave-modules-CamelCase")
                else:
                    if not re.match(r'^[a-z0-9]+(-[a-z0-9]+)*\.dwl$',file):
                        add_issue(os.path.join(root,file),1,f"DataWeave file '{file}' in modules must be in kebab-case","dataweave-kebab-case")
                    
#Rule 4 :YAML files em kebab-case
def check_yaml_files():
    base_dir = os.path.join(PROJECT_ROOT, "src","main","resources")
   # if not os.path.isdir(base_dir):
    #    add_issue("src/main/resources",1,"Missing 'src/main/resources' directory for YAML configuration files","resource-location")
    
    #Iterates through all folders and subfolders from the /src/main/resources .
    for root,_,files in os.walk(base_dir):
        #Iterates each file found in the directory
        for file in files:
            if file.endswith(".yaml",".yml"):
                if not re.match(r'^[a-z0-9]+(-[a-z0-9]+)*\.(yaml|yml)$',file):
                    add_issue(os.path.join(root,file),1,f"YAML file '{file}' is not in kebab-case","yaml-kebab-case")

#Rule 5 : JSON Examples should be in kebab-case with a especific format
def check_example_json():
    base_dir = os.path.join(PROJECT_ROOT, "src","main","resources")
  #  if not os.path.isdir(base_dir):
   #     add_issue("src/main/resources",1,"Missing 'src/main/resources' directory for JSON files","resource-location")

    pattern=r'^(get|post|put|delete|patch)-[a-z0-9\-]+-(request|response)-example\.json$'
    #Iterates through all folders and subfolders from the /src/main/resources .
    for root,_,files in os.walk(base_dir):
        #Iterates each file found in the directory
        for file in files:
            if file.endswith(".json"):
                if "example" in file and not re.match(pattern,file):
                    add_issue(os.path.join(root,file),1,f"Example JSON file '{file}' does not follow naming convention","json-example-format")


#Rule 6 : Main RAML should have the same name as the project
def check_main_raml():
    base_dir = os.path.join(PROJECT_ROOT, "src","main","resources")
  #  if not os.path.isdir(base_dir):
   #     add_issue("src/main/resources",1,"Missing 'src/main/resources' directory for RAML files","resource-location")
    
    expected_file=PROJECT_NAME + ".raml"
    for root,_,files in os.walk(base_dir):
        if expected_file in files:
            return
    add_issue(root,1,f"Main RAML file '{expected_file}' not found", "main-raml-name")   


#Execution
#check_project_name()
#check_main_app_filename()
check_dataweave_files()
check_yaml_files()
check_example_json()
#check_main_raml() , this function might not be needed because usually api specification are directly imported from exchange

output_file_path = '/var/jenkins_home/workspace/SonarPipeline/repo/sonar-structure-rules-issues.json'

try:
    with open(output_file_path, "w", encoding="utf-8") as f:
        json.dump({"rules" : RULES ,"issues": ISSUES}, f, indent=2)
    print(f"Arquivo JSON criado com sucesso em {output_file_path}")
except Exception as e:
    print(f"Erro ao criar o arquivo JSON: {e}")          