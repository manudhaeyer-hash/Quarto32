$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
# Boss = solver C++ (config/Boss.cpp). Necessite g++ (MinGW-w64) dans le PATH.
g++ -O2 -o config/boss.exe config/Boss.cpp
mvn clean install
mvn exec:exec -D"exec.executable"="java" -D"exec.classpathScope"="test" -D"exec.args"="--add-opens java.base/java.lang=ALL-UNNAMED -cp %classpath Main"
