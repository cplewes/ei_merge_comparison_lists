#!/bin/bash

# Compilation script for decompiled AGFA RIS classes

echo "Starting compilation of decompiled Java classes..."

# Read classpath from file
CLASSPATH=$(cat classpath.txt)

# Compile Java source files
echo "Compiling Java files with classpath containing $(echo $CLASSPATH | tr ':' '\n' | wc -l) JAR files..."

javac -cp "$CLASSPATH" -d build/classes -sourcepath build/src \
    build/src/com/agfa/ris/client/lta/textarea/docks/dockables/ComparisonStudiesDock.java \
    build/src/com/agfa/ris/client/lta/textarea/studylist/ComposedStudyListController.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Compiled classes are in build/classes/"
    ls -la build/classes/com/agfa/ris/client/lta/textarea/
else
    echo "Compilation failed!"
    exit 1
fi