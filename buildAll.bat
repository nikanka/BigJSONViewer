cd ../BigJSONParser
call mvn clean package
call mvn install:install-file -Dfile=target/BigJSONParser-1.0.jar -DlocalRepositoryPath=../BigJSONViewer/lib  -DpomFile=pom.xml
cd ../BigJSONViewer
call buildViewer.bat