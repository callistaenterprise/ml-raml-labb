#!/bin/sh

    # Perform a pull to ensure tha latest version (of the latest tag)...
    # docker pull magnuslarsson/az-ip-api-server
    # docker run -it --rm -p 8080:8080 magnuslarsson/az-ip-api-server

    # host=localhost
    host=docker
    port=8080
    hdr="--user demo:omed.1 -s -w \\n"


    # 1. Register a study
    echo
    echo "USECASE #1. Register a study"

    studyId=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies \
      -X POST -d '{"name":"S-1","description":"descr","startdate":1438869685556,"enddate":1438869685556}' | jq -r .id`
    echo studyId: $studyId

    # Sample response: {"id":"450e159e-76f5-421d-9ff5-f4642b7f3a71","version":0,"name":"S-1","description":"descr","startdate":1438869685556,"enddate":1438869685556}



    # 2. Register two doctors and assign them to the study
    echo
    echo "USECASE #2. Register two doctors and assign them to the study"

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
    echo
    echo "USECASE #3. Register two patients and assign them to the study by doctor #1"

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
    echo
    echo "USECASE #4. Register measurements for patient #1"

    # 4.1 Get all studies (typically performed during application startup)

    availableStudies=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies | jq .`
    echo "availableStudies: $availableStudies"

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
    echo
    echo "USECASE #5. Register measurements for patient #2"

    # 5.1 Get all studies (typically performed during application startup)

    availableStudies=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies | jq .`
    echo "availableStudies: $availableStudies"

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





    # 6. Get all measurements for a study
    echo
    echo "USECASE #6. Get all measurements for a study"

    studyMeasurements=`curl $hdr -H "Content-Type: application/json" http://$host:$port/api/studies/$studyId/measurements | jq .`
    echo "studyMeasurements: $studyMeasurements"

    # Sample response from curl: [{"id":"4dbfd012-8f1f-4bf0-98ab-b01771dc6072","version":0,"description":"descr","timestamp":1438869686987,"steps":1000},{"id":"0941f95d-bd75-4cd2-90ea-803de9dad23a","version":0,"description":"descr","timestamp":1438869687022,"steps":2000},{"id":"725c0d69-783d-46d2-8542-d883676fef51","version":0,"description":"descr","timestamp":1438869686987,"steps":1000},{"id":"fed32f4e-7897-468e-8786-7c2ed39d7e22","version":0,"description":"descr","timestamp":1438869687022,"steps":2000}]
