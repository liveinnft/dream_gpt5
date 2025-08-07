@ECHO OFF
set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
java -Xmx64m -Xms64m -cp "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*