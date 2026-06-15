#!/bin/bash
# Sets the project's Java home to Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
# Refresh command lookup cache so shells pick up the updated PATH immediately.
hash -r 2>/dev/null || true
rehash 2>/dev/null || true
echo "JAVA_HOME set to: $JAVA_HOME"
echo "PATH updated with: $JAVA_HOME/bin"
