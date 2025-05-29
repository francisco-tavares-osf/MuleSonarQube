import os 
import re
import json

ISSUES = []

REQUIRED_FOLDERS = ['src/main/mule','src/test','src/main/resources','src/main/resources/dwls','src/main/resources/application.properties','src/main/resources/environment.properties','src/test/resources/properties.environment']
REQUIRED_FILES = ['pom.xml','README.md','global.xml','munit-global.xml','health-check.xml']
DEFAULT_FILE_PATH = "README.md"
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
        "id": "xml-kebab-case",
        "name": "XML files kebab-case",
        "description": "XML files must be in kebab-case",
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
    },
    {  
        "id": "missing-required-file",
        "name": "Missing required files",
        "description": "Ensure required files exist.",
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
    },
    {  
        "id": "missing-required-folder",
        "name": "Missing required folder",
        "description": "Ensure required folders exist.",
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

# verifica se todos os ficheiros obrigatorios estao no projeto
def check_required_files():
    missing_files = set(REQUIRED_FILES)  # Track missing files
    for root, _, files in os.walk(PROJECT_ROOT):
        for file in files:
            if file in missing_files:
                missing_files.remove(file)
    
    for missing_file in missing_files:
        add_issue(os.path.join(PROJECT_ROOT, DEFAULT_FILE_PATH), 1, f"Required file '{missing_file}' not found, please check development standards.", "missing-required-file")

# verifica se todos os pastas obrigatorios estao no projeto
def check_required_folders():
    missing_folders = set(REQUIRED_FOLDERS)  # Track missing folders
    for required_folder in REQUIRED_FOLDERS:
        folder_path = os.path.join(PROJECT_ROOT, required_folder)
        if os.path.isdir(folder_path):
            missing_folders.remove(required_folder)

    # Log issues for missing folders
    for missing_folder in missing_folders:
        add_issue(os.path.join(PROJECT_ROOT, DEFAULT_FILE_PATH), 1, f"Required folder '{missing_folder}' not found, please check development standards.", "missing-required-folder")

# Projeto deve estar em kebab-case com formato {project-identifier}-{context}-{name}-{layer}-{identifier}
def check_project_name():
    expected_name_pattern = r'^([a-z0-9]+|[a-z0-9]+(-[a-z0-9]+)*-[a-z0-9]+-[a-z0-9]+(-[a-z0-9]+)*(-[a-z0-9]+)?+oel)$'
    if not re.match(expected_name_pattern,PROJECT_NAME):
        add_issue(os.path.join(PROJECT_ROOT, DEFAULT_FILE_PATH),1,f"Project name '{PROJECT_NAME}' is not the expected kebab-case format","project-name-format")


# DataWeave files
def check_dataweave_files():
    #Iterates through all folders and subfolders from the /src/main/resources .
    for root,_,files in os.walk(PROJECT_ROOT):
        #Iterates each file found in the directory
        for file in files:
            if file.endswith(".dwl"):
                relative_path = os.path.relpath(root,PROJECT_ROOT).replace(os.sep,"/")
                if relative_path.startswith("src/main/resources/modules"):
                    if not re.match(r'[A-Z][a-zA-Z0-9]+\.dwl$',file):
                        add_issue(os.path.join(root,file),1,f"DataWeave file '{file}' in modules must be in CamelCase","dataweave-modules-CamelCase")
                else:
                    if not re.match(r'^[a-z0-9]+(-[a-z0-9]+)*\.dwl$',file):
                        add_issue(os.path.join(root,file),1,f"DataWeave file '{file}' in modules must be in kebab-case","dataweave-kebab-case")
#xml files should be kebab-case
def check_xml_files():
    items = os.listdir(PROJECT_ROOT)  
    for root,_,files in os.walk(PROJECT_ROOT):
        for file in files:
            if file.endswith(".xml"):
                if not re.match(r'^[a-z0-9]+(-[a-z0-9]+)*\.xml$',file):
                        add_issue(os.path.join(root,file),1,f"XML file '{file}' is not in kebab-case","xml-kebab-case")

              
# YAML files em kebab-case
def check_yaml_files():
    
    #Iterates through all folders and subfolders from the /src/main/resources .
    for root,_,files in os.walk(PROJECT_ROOT):
        files_set = set(files)
        #Iterates each file found in the directory
        for file in files:
            if file.endswith(".yaml") or file.endswith(".yml"):
                if not re.match(r'^[a-z0-9]+(-[a-z0-9]+)*\.(yaml|yml)$',file):
                    add_issue(os.path.join(root,file),1,f"YAML file '{file}' is not in kebab-case","yaml-kebab-case")
                    
                base_name = file.rsplit('.', 1)[0]
                # Verifica se o arquivo '-secure.yaml' correspondente está presente
                if not base_name.endswith('-secure'):
                    secure_file = f"{base_name}-secure.yaml"
                    if secure_file not in files_set:
                        add_issue(os.path.join(root, file), 1, f"Missing '{secure_file}' in the same directory", "missing-required-file")
                        
# JSON Examples should be in kebab-case with a especific format
def check_example_json():
    pattern=r'^(get|post|put|delete|patch)-[a-z0-9\-]+-(request|response)-example\.json$'
    for root,_,files in os.walk(PROJECT_ROOT):
        #Iterates each file found in the directory
        for file in files:
            print(f"FILE{file}")
            if file.endswith(".json"):
                print(f"JSON FILE AQUI: {file}")          
                if "example" in file and not re.match(pattern,file):
                    add_issue(os.path.join(root,file),1,f"Example JSON file '{file}' does not follow naming convention","json-example-format")




#Execution
check_project_name()
check_required_files()
check_dataweave_files()
check_yaml_files()
check_example_json()


output_file_path = os.path.join(PROJECT_ROOT, 'sonar-structure-rules-issues.json')

try:
    with open(output_file_path, "w", encoding="utf-8") as f:
        json.dump({"rules" : RULES ,"issues": ISSUES}, f, indent=2)
    print(f"Arquivo JSON criado com sucesso em {output_file_path}")
except Exception as e:
    print(f"Erro ao criar o arquivo JSON: {e}")          