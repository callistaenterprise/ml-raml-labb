
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

    docker build -f Dockerfile-build -t magnuslarsson/az-ip-build .

    docker run -it --rm magnuslarsson/az-ip-build /bin/bash
    docker push magnuslarsson/az-ip-build

    mvn spring-boot:run
    mvn package
    docker build -t az-1 .
    docker run -it --rm -p 8080:8080 az-1

    docker run -it --rm -p 8080:8080 magnuslarsson/az-ip-api-server

    curl --user demo:omed.1 -H "Content-Type: application/json" -X POST -d '{"username":"U11","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' https://docker:8080/patients -ik
    curl --user demo:omed.1 -H "Content-Type: application/json" -X POST -d '{"username":"U21","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' https://docker:8080/patients -ik
    curl --user demo:omed.1 -H "Content-Type: application/json" -X POST -d '{"username":"U31","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' https://docker:8080/patients -ik
    curl --user demo:omed.1 "https://docker:8080/patients" -ik
    curl --user demo:omed.1 "https://docker:8080/patients/U21" -ik
    curl --user demo:omed.1 -H "Content-Type: application/json" -X POST -d '{"username":"U31","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}' https://docker:8080/patients -ik
    curl --user demo:omed.1 "https://docker:8080/patients/U41" -ik

    mvn -Dtest=*SystemIntegrationTests test

# Shippable test formation

    AWS_HOST=mt-fo-lb-168423950.us-east-1.elb.amazonaws.com
    AWS_PORT=59406
    mvn -Dtest=*SystemIntegrationTests test -Dmyhost=$AWS_HOST -Dmyport=$AWS_PORT -Dmuuser=demo -Dmypwd=omed.1

    $ curl "https://$AWS_HOST:$AWS_PORT" -ik
    HTTP/1.1 401 Unauthorized

    curl --user demo:omed.1 "https://$AWS_HOST:$AWS_PORT/api/patients" -ik

    telnet $AWS_HOST $AWS_PORT

# RAML access

    sed -i.org 's/baseUri: https:\/\/localhost:8080\/api/baseUri: \/api/' src/main/resources/public/raml/az-ip-api.raml
    rm src/main/resources/public/raml/az-ip-api.raml.org

## CORS header:

    curl -H "Origin: mee" https://192.168.1.74:8080/raml/az-ip-api.raml -skv > /dev/null

URL in RAML Console:

    https://localhost:8080/raml/az-ip-api.raml

# CURL testsvit

1 > POST http://localhost:52006/api/studies
1 > accept: application/json, application/*+json
1 > accept-encoding: gzip,deflate
1 > authorization: Basic ZGVtbzpvbWVkLjE=
1 > connection: Keep-Alive
1 > content-length: 86
1 > content-type: application/json;charset=UTF-8
1 > host: localhost:52006
1 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)
{"name":"S-2","description":"descr","startdate":1438869685556,"enddate":1438869685556}

1 < 200
1 < Content-Type: application/json
{"id":"e29fc237-0884-4b2a-8561-5353dfa569f4","version":0,"name":"S-2","description":"descr","startdate":1438869685556,"enddate":1438869685556}


2 > POST http://localhost:52006/api/doctors
2 > accept: application/json, application/*+json
2 > accept-encoding: gzip,deflate
2 > authorization: Basic ZGVtbzpvbWVkLjE=
2 > connection: Keep-Alive
2 > content-length: 51
2 > content-type: application/json;charset=UTF-8
2 > host: localhost:52006
2 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)
{"username":"D-2","firstname":"F1","lastname":"L1"}

2 < 200
2 < Content-Type: application/json
{"id":"2d63987a-2727-4098-97fe-e56a2e7c2aa3","version":0,"username":"D-2","firstname":"F1","lastname":"L1"}


3 > POST http://localhost:52006/api/studies/e29fc237-0884-4b2a-8561-5353dfa569f4/assignedDoctors
3 > accept: text/plain, application/json, application/*+json, */*
3 > accept-encoding: gzip,deflate
3 > authorization: Basic ZGVtbzpvbWVkLjE=
3 > connection: Keep-Alive
3 > content-length: 45
3 > content-type: application/json;charset=UTF-8
3 > host: localhost:52006
3 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)
{"id":"2d63987a-2727-4098-97fe-e56a2e7c2aa3"}

3 < 200


4 > GET http://localhost:52006/api/studies/e29fc237-0884-4b2a-8561-5353dfa569f4/assignedDoctors
4 > accept: application/json, application/*+json
4 > accept-encoding: gzip,deflate
4 > authorization: Basic ZGVtbzpvbWVkLjE=
4 > connection: Keep-Alive
4 > host: localhost:52006
4 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

4 < 200
4 < Content-Type: application/json
[{"id":"2d63987a-2727-4098-97fe-e56a2e7c2aa3"}]


5 > GET http://localhost:52006/api/doctors/2d63987a-2727-4098-97fe-e56a2e7c2aa3/assignedInStudies
5 > accept: application/json, application/*+json
5 > accept-encoding: gzip,deflate
5 > authorization: Basic ZGVtbzpvbWVkLjE=
5 > connection: Keep-Alive
5 > host: localhost:52006
5 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

5 < 200
5 < Content-Type: application/json
[{"id":"e29fc237-0884-4b2a-8561-5353dfa569f4"}]


6 > POST http://localhost:52006/api/patients
6 > accept: application/json, application/*+json
6 > accept-encoding: gzip,deflate
6 > authorization: Basic ZGVtbzpvbWVkLjE=
6 > connection: Keep-Alive
6 > content-length: 96
6 > content-type: application/json;charset=UTF-8
6 > host: localhost:52006
6 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)
{"username":"D-2","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}

6 < 200
6 < Content-Type: application/json
{"id":"4943a22d-5086-4481-bb14-6abf60e35151","version":0,"username":"D-2","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}


7 > POST http://localhost:52006/api/doctors/2d63987a-2727-4098-97fe-e56a2e7c2aa3/assignedInStudies/e29fc237-0884-4b2a-8561-5353dfa569f4/patients
7 > accept: text/plain, application/json, application/*+json, */*
7 > accept-encoding: gzip,deflate
7 > authorization: Basic ZGVtbzpvbWVkLjE=
7 > connection: Keep-Alive
7 > content-length: 45
7 > content-type: application/json;charset=UTF-8
7 > host: localhost:52006
7 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)
{"id":"4943a22d-5086-4481-bb14-6abf60e35151"}

7 < 200


8 > GET http://localhost:52006/api/doctors/2d63987a-2727-4098-97fe-e56a2e7c2aa3/assignedInStudies/e29fc237-0884-4b2a-8561-5353dfa569f4/patients
8 > accept: application/json, application/*+json
8 > accept-encoding: gzip,deflate
8 > authorization: Basic ZGVtbzpvbWVkLjE=
8 > connection: Keep-Alive
8 > host: localhost:52006
8 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

8 < 200
8 < Content-Type: application/json
[{"id":"4943a22d-5086-4481-bb14-6abf60e35151"}]


9 > GET http://localhost:52006/api/patients?username=D-2
9 > accept: application/json, application/*+json
9 > accept-encoding: gzip,deflate
9 > authorization: Basic ZGVtbzpvbWVkLjE=
9 > connection: Keep-Alive
9 > host: localhost:52006
9 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

9 < 200
9 < Content-Type: application/json
[{"id":"4943a22d-5086-4481-bb14-6abf60e35151","version":0,"username":"D-2","patientID":"1234","firstname":"F1","lastname":"L1","weight":100,"height":200}]


10 > GET http://localhost:52006/api/patients/4943a22d-5086-4481-bb14-6abf60e35151/studies
10 > accept: application/json, application/*+json
10 > accept-encoding: gzip,deflate
10 > authorization: Basic ZGVtbzpvbWVkLjE=
10 > connection: Keep-Alive
10 > host: localhost:52006
10 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

10 < 200
10 < Content-Type: application/json
[{"id":"e29fc237-0884-4b2a-8561-5353dfa569f4"}]


11 > GET http://localhost:52006/api/studies/e29fc237-0884-4b2a-8561-5353dfa569f4
11 > accept: application/json, application/*+json
11 > accept-encoding: gzip,deflate
11 > authorization: Basic ZGVtbzpvbWVkLjE=
11 > connection: Keep-Alive
11 > host: localhost:52006
11 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

11 < 200
11 < Content-Type: application/json
{"id":"e29fc237-0884-4b2a-8561-5353dfa569f4","version":1,"name":"S-2","description":"descr","startdate":"2015-08-06","enddate":"2015-08-06"}


12 > POST http://localhost:52006/api/patients/4943a22d-5086-4481-bb14-6abf60e35151/studies/e29fc237-0884-4b2a-8561-5353dfa569f4/measurements
12 > accept: application/json, application/*+json
12 > accept-encoding: gzip,deflate
12 > authorization: Basic ZGVtbzpvbWVkLjE=
12 > connection: Keep-Alive
12 > content-length: 62
12 > content-type: application/json;charset=UTF-8
12 > host: localhost:52006
12 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)
{"description":"descr","timestamp":1438869686987,"steps":1000}

12 < 200


13 > POST http://localhost:52006/api/patients/4943a22d-5086-4481-bb14-6abf60e35151/studies/e29fc237-0884-4b2a-8561-5353dfa569f4/measurements
13 > accept: application/json, application/*+json
13 > accept-encoding: gzip,deflate
13 > authorization: Basic ZGVtbzpvbWVkLjE=
13 > connection: Keep-Alive
13 > content-length: 62
13 > content-type: application/json;charset=UTF-8
13 > host: localhost:52006
13 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)
{"description":"descr","timestamp":1438869687022,"steps":2000}

13 < 200


14 > GET http://localhost:52006/api/patients/4943a22d-5086-4481-bb14-6abf60e35151/studies/e29fc237-0884-4b2a-8561-5353dfa569f4/measurements
14 > accept: application/json, application/*+json
14 > accept-encoding: gzip,deflate
14 > authorization: Basic ZGVtbzpvbWVkLjE=
14 > connection: Keep-Alive
14 > host: localhost:52006
14 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

14 < 200
14 < Content-Type: application/json
[{"id":"2609c8fb-194f-42e1-97da-43ffaf3716f2","version":0,"description":"descr","timestamp":1438869686987,"steps":1000},{"id":"e514089b-6cdd-4311-bca9-24e8ed353f41","version":0,"description":"descr","timestamp":1438869687022,"steps":2000}]


15 > GET http://localhost:52978/api/studies/36f5df63-84fd-43f2-8680-a3f930f74116/measurements
15 > accept: application/json, application/*+json
15 > accept-encoding: gzip,deflate
15 > authorization: Basic ZGVtbzpvbWVkLjE=
15 > connection: Keep-Alive
15 > host: localhost:52978
15 > user-agent: Apache-HttpClient/4.5 (Java/1.8.0_11)

15 < 200
15 < Content-Type: application/json
[{"id":"51734def-744d-4be2-8b35-a9e35b35a47a","version":0,"description":"descr","timestamp":1438873470546,"steps":1000},{"id":"88fc7bf6-a789-4a09-a6a3-2708027670a7","version":0,"description":"descr","timestamp":1438873470590,"steps":2000}]
