FROM java:8
MAINTAINER Magnus Larsson <magnus.larsson.ml@gmail.com>

EXPOSE 8080

#ADD target/az-ip-api-server-1.0.0-SNAPSHOT.jar az-ip-api-server-1.0.0-SNAPSHOT.jar
#ADD az-ip-api-server-1.0.0-SNAPSHOT.jar az-ip-api-server-1.0.0-SNAPSHOT.jar
ADD ./buildoutput/az-ip-api-server-1.0.0-SNAPSHOT.jar az-ip-api-server-1.0.0-SNAPSHOT.jar

# Regarding settings of java.security.egd, see http://wiki.apache.org/tomcat/HowTo/FasterStartUp#Entropy_Source
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/az-ip-api-server-1.0.0-SNAPSHOT.jar"]
