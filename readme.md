
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

    # Perform a pull to ensure tha latest version (of the latest tag)...
    docker pull magnuslarsson/az-ip-api-server
    docker run -it --rm -p 8080:8080 magnuslarsson/az-ip-api-server

    host=docker
    port=8080
    hdr="--user demo:omed.1 -s -w \\n"
    curl $hdr -H "Content-Type: application/json" "http://$host:$port/api/patients"


    # 1. Register a study

    studyId=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies \
      -X POST -d '{"name":"S-1","description":"descr","startdate":1438869685556,"enddate":1438869685556}' | jq -r .id`
    echo studyId: $studyId

    # Sample response: {"id":"450e159e-76f5-421d-9ff5-f4642b7f3a71","version":0,"name":"S-1","description":"descr","startdate":1438869685556,"enddate":1438869685556}
    studyId=450e159e-76f5-421d-9ff5-f4642b7f3a71



    # 2. Register two doctors and assign them to the study

    # 2.1 Register doctor #1

    doctor1Id=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/doctors \
      -X POST -d '{"username":"D-1","firstname":"F1","lastname":"L1"}' | jq -r .id`
    echo doctor1Id: $doctor1Id

    # Sample response from curl: {"id":"81609d83-0be5-4b82-b7ef-f5ad093be3b6","version":0,"username":"D-1","firstname":"F1","lastname":"L1"}


    # 2.2 Assign a doctor #1 to the study

    curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies/$studyId/assignedDoctors -X POST -d "{\"id\":\"$doctor1Id\"}"


    # 2.3 Check studies that doctor #1 is assigned to

    doctor1AssignedStudies=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/doctors/$doctor1Id/assignedInStudies | jq -r '.[] | .id'`
    echo doctor1AssignedStudies: $doctor1AssignedStudies

    # Sample response from curl: [{"id":"450e159e-76f5-421d-9ff5-f4642b7f3a71"}]



    # 2.4 Register doctor #2

    doctor2Id=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/doctors \
      -X POST -d '{"username":"D-2","firstname":"F2","lastname":"L2"}' | jq -r .id`
    echo doctor2Id: $doctor2Id

    # Sample response from curl: {"id":"d194a6ea-d52c-478f-bec7-3545bf0e3b20","version":0,"username":"D-2","firstname":"F2","lastname":"L2"}


    # 2.5 Assign a doctor #2 to the study

    curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies/$studyId/assignedDoctors -X POST -d "{\"id\":\"$doctor2Id\"}"


    # 2.6 Check studies that doctor #2 is assigned to

    doctor2AssignedStudies=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/doctors/$doctor2Id/assignedInStudies | jq -r '.[] | .id'`
    echo doctor2AssignedStudies: $doctor2AssignedStudies

    # Sample response from curl: [{"id":"450e159e-76f5-421d-9ff5-f4642b7f3a71"}]



    # 2.7 Check assigned doctors for a study

    doctorsAssignedInStudy=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies/$studyId/assignedDoctors`
    echo doctorsAssignedInStudy: $doctorsAssignedInStudy

    # Sample response from curl: [{"id":"81609d83-0be5-4b82-b7ef-f5ad093be3b6"},{"id":"d194a6ea-d52c-478f-bec7-3545bf0e3b20"}]



    # 3. Register two patients and assign them to the study by doctor #1

    # 3.1 Register patient #1

    patient1Id=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients \
      -X POST -d '{"username":"P-1","patientID":"1234","firstname":"P1-F","lastname":"P1-L","weight":100,"height":200}' | jq -r .id`
    echo patient1Id: $patient1Id

    # Sample response from curl: {"id":"ded54024-0d76-4c6c-9595-1e907a920664","version":0,"username":"P-1","patientID":"1234","firstname":"P1-F","lastname":"P1-L","weight":100,"height":200}


    # 3.2 Assign patient #1 to the study by doctor #1

    curl $hdr -H "Content-Type: application/json" http://$host:$port/api/doctors/$doctor1Id/assignedInStudies/$studyId/patients -X POST -d "{\"id\":\"$patient1Id\"}"


    # 3.3 Register patient #2

    patient2Id=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients \
      -X POST -d '{"username":"P-2","patientID":"5678","firstname":"P2-F","lastname":"P2-L","weight":100,"height":200}' | jq -r .id`
    echo patient2Id: $patient2Id

    # Sample response from curl: {"id":"9a94ab6f-701e-41a7-b682-07d7eaf56286","version":0,"username":"P-2","patientID":"5678","firstname":"P2-F","lastname":"P2-L","weight":100,"height":200}


    # 3.4 Assign patient #2 to the study by doctor #1

    curl $hdr -H "Content-Type: application/json" http://$host:$port/api/doctors/$doctor1Id/assignedInStudies/$studyId/patients -X POST -d "{\"id\":\"$patient2Id\"}"


    # 3.5 Check assigned patients to the study by doctor #1

    doctor1Patients=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/doctors/$doctor1Id/assignedInStudies/$studyId/patients`
    echo doctor1Patients: $doctor1Patients

    # Sample response from curl: [{"id":"ded54024-0d76-4c6c-9595-1e907a920664"},{"id":"9a94ab6f-701e-41a7-b682-07d7eaf56286"}]



    # 4. Register measurements for patient #1

    # 4.1 Get all studies (typically performed during application startup)

    availableStudies=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies`
    echo availableStudies: $availableStudies

    # Sample response from curl: [{"id":"450e159e-76f5-421d-9ff5-f4642b7f3a71","version":2,"name":"S-1","description":"descr","startdate":"2015-08-06","enddate":"2015-08-06"}]


    # 4.2 Lookup patient #1 by its username

    patient1ByUsername=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients?username=P-1 | jq -r '.[] | .id'`
    echo patient1ByUsername: $patient1ByUsername

    # Sample response from curl: [{"id":"ded54024-0d76-4c6c-9595-1e907a920664","version":0,"username":"P-1","firstname":"P1-F","lastname":"P1-L"}]


    # 4.3 Lookup the studies that the patient is assigned to

    patient1Study=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient1Id/studies | jq -r '.[] | .id'`
    echo patient1Study: $patient1Study

    # Sample response from curl: [{"id":"ceb3e797-c15a-4ab0-9f1f-7f0670f1447e"}]


    # 4.4 Register measurements on patient #1

    patient1measurement1=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient1Id/studies/$patient1Study/measurements \
      -X POST -d '{"description":"descr","timestamp":1438869686987,"steps":1000}'`
    echo patient1measurement1: $patient1measurement1

    patient1measurement2=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient1Id/studies/$patient1Study/measurements \
      -X POST -d '{"description":"descr","timestamp":1438869687022,"steps":2000}'`
    echo patient1measurement2: $patient1measurement2


    # 4.5 Verify patient #1's measurements

    patient1measurements=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient1Id/studies/$patient1Study/measurements | jq .`
    echo "patient1measurements: $patient1measurements"

    # Sample response from curl: [{"id":"0879878f-480c-415c-8a45-822f2fb0d0f9","version":0,"description":"descr","timestamp":1438869686987,"steps":1000},{"id":"b485ca1f-a788-4dc8-aa44-051f84ccf5a8","version":0,"description":"descr","timestamp":1438869687022,"steps":2000}]



    # 5. Register measurements for patient #2

    # 5.1 Get all studies (typically performed during application startup)

    availableStudies=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies`
    echo availableStudies: $availableStudies

    # Sample response from curl: [{"id":"450e159e-76f5-421d-9ff5-f4642b7f3a71","version":2,"name":"S-1","description":"descr","startdate":"2015-08-06","enddate":"2015-08-06"}]


    # 5.2 Lookup patient #2 by its username

    patient2ByUsername=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients?username=P-2 | jq -r '.[] | .id'`
    echo patient2ByUsername: $patient2ByUsername

    # Sample response from curl: [{"id":"ded54024-0d76-4c6c-9595-1e907a920664","version":0,"username":"P-2","firstname":"P1-F","lastname":"P1-L"}]


    # 5.3 Lookup the studies that the patient is assigned to

    patient2Study=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient2Id/studies | jq -r '.[] | .id'`
    echo patient2Study: $patient2Study

    # Sample response from curl: [{"id":"ceb3e797-c15a-4ab0-9f1f-7f0670f1447e"}]


    # 5.4 Register measurements on patient #2

    patient2measurement1=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient2Id/studies/$patient2Study/measurements \
      -X POST -d '{"description":"descr","timestamp":1438869686987,"steps":3000}'`
    echo patient2measurement1: $patient2measurement1

    patient2measurement2=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient2Id/studies/$patient2Study/measurements \
      -X POST -d '{"description":"descr","timestamp":1438869687022,"steps":4000}'`
    echo patient2measurement2: $patient2measurement2


    # 5.5 Verify patient #2's measurements

    patient2measurements=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/patients/$patient2Id/studies/$patient2Study/measurements | jq .`
    echo "patient2measurements: $patient2measurements"

    # Sample response from curl: [{"id":"0879878f-480c-415c-8a45-822f2fb0d0f9","version":0,"description":"descr","timestamp":1438869686987,"steps":3000},{"id":"b485ca1f-a788-4dc8-aa44-051f84ccf5a8","version":0,"description":"descr","timestamp":1438869687022,"steps":4000}]





### FEL FEL FEL ###
    # 6. Get all measurements for a study

    studyMeasurements=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies/$studyId/measurements | jq .`
    echo "studyMeasurements: $studyMeasurements"

    # Sample response from curl: [{"id":"0879878f-480c-415c-8a45-822f2fb0d0f9","version":0,"description":"descr","timestamp":1438869686987,"steps":1000},{"id":"0879878f-480c-415c-8a45-822f2fb0d0f9","version":0,"description":"descr","timestamp":1438869686987,"steps":1000},{"id":"b485ca1f-a788-4dc8-aa44-051f84ccf5a8","version":0,"description":"descr","timestamp":1438869687022,"steps":2000},{"id":"b485ca1f-a788-4dc8-aa44-051f84ccf5a8","version":0,"description":"descr","timestamp":1438869687022,"steps":2000},{"id":"2f308612-2915-4d86-a276-2cbedca2dd83","version":0,"description":"descr","timestamp":1438869688095,"steps":5000},{"id":"2f308612-2915-4d86-a276-2cbedca2dd83","version":0,"description":"descr","timestamp":1438869688095,"steps":5000}]
