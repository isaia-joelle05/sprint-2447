@echo off
@REM Compile Framework

set DESTINATION_DIR="C:\Users\isaia\Documents\S4\MrNaina\frame\testSprint1\WEB-INF"
set LIB_SERVLET="C:\apache-tomcat-9.0.74\apache-tomcat-9.0.74\lib\servlet-api.jar"

if not exist "%DESTINATION_DIR%\lib" (
    mkdir "%DESTINATION_DIR%\lib"
)

REM complilation des fichiers java
cd src
javac -cp "%LIB_SERVLET%" -d ..\classes *.java

REM rendre les classes en jar
jar -cf .\framework.jar -C ..\classes .

REM envoyer le jar vers le dossier dans test
copy .\framework.jar "%DESTINATION_DIR%\lib"

echo Compilation terminée
