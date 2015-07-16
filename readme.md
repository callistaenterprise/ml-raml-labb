
# spring initializr and spring cli

http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#getting-started-installing-the-cli

    gvm install springboot 1.3.0.M2

    spring --version
    >>> Spring CLI v1.3.0.M2

    ls ~/.gvm/springboot/
    >>> 1.2.1.RELEASE/ 1.2.5.RELEASE/ 1.3.0.M2/      current/

Add to ~/.bash_profile for auto-complete support:
    . ~/.gvm/springboot/current/shell-completion/bash/spring

Try autocompete
    spring <HIT TAB HERE>
    >>> grab  help  jar  run  test  version
  
(Minimal web-app med groovy)[http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#getting-started-cli-example]
  

    spring init --boot-version=1.2.5.RELEASE --build=maven --java-version=1.8 --dependencies=actuator,security,jersey,data-mongodb --packaging=war sample-web-app-mvn

    spring init --boot-version=1.2.5.RELEASE --build=maven --java-version=1.8 --dependencies=actuator,security,jersey,data-mongodb --packaging=jar sample-app-mvn

    spring init --boot-version=1.2.5.RELEASE --build=gradle --java-version=1.8 --dependencies=actuator,security,jersey,data-mongodb --packaging=war sample-web-app

    spring init --boot-version=1.2.5.RELEASE --build=gradle --java-version=1.8 --dependencies=actuator,security,jersey,data-mongodb --packaging=jar sample-app

    spring init --boot-version=1.3.0.M2 --build=gradle --java-version=1.8 --packaging=jar --dependencies=actuator,security,jersey,data-mongodb \
    --groupId=com.az.ip.api --artifactId=az-ip-api-server --version=1.0.0-SNAPSHOT az-ip-api-server

    #--boot-version=1.2.5.RELEASE --packageName=com.az.ip.api.server --description=IP API server 

# Docker

    mvn package
    docker build -t az-1 .
    docker run -it --rm -p 8080:8080 az-1
    
    curl -H "Content-Type: application/json" -X POST -d '{"username":"U11","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' docker:8080/patients -i
    curl -H "Content-Type: application/json" -X POST -d '{"username":"U21","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' docker:8080/patients -i
    curl -H "Content-Type: application/json" -X POST -d '{"username":"U31","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' docker:8080/patients -i
    curl "docker:8080/patients" -i
    curl "docker:8080/patients/21" -i
    curl -H "Content-Type: application/json" -X POST -d '{"username":"U31","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' docker:8080/patients -i
    curl "docker:8080/patients/41" -i
    
    mvn -Dtest=*SystemIntegrationTests test
