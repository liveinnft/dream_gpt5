#!/usr/bin/env sh

APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

WRAPPER_JAR="$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar"

exec java $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  -cp "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"