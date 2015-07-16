package com.az.ip.api

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration;


import wslite.http.auth.HTTPBasicAuthorization
import wslite.rest.RESTClient
import wslite.rest.RESTClientException

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
class PatientSystemIntegrationTests {

    RESTClient client = new RESTClient("http://docker:8080")

    @Before
    def void before() {
        client.authorization = new HTTPBasicAuthorization("demo", "123")
    }

    @Test
    def void testItAll() {

        // Get current number of patients
        def response = client.get(path: "/patients", accept: "application/json")
        assert response.statusCode == 200
//        println response.contentAsString
        def cnt = response.json.size()


        // Create a patient with a random username
        def rndUsername = UUID.randomUUID().toString()
        response = client.post(path: '/patients') {
            type "application/json"
            charset "UTF-8"
            json username: rndUsername, patientID: "1234", firstname: "F1", lastname: "L1", weight: 100, height:200
        }
        assert response.statusCode == 200

        // Verify that there now is one more patient
        response = client.get(path: "/patients", accept: "application/json")
        assert response.statusCode == 200
        assert response.json.size() == cnt + 1

        // Get the new patient
        response = client.get(path: "/patients/" + rndUsername, accept: "application/json")
        assert response.statusCode == 200
        assert response.json.username == rndUsername

        // Get an unknown patient, verify 404
        def rndUnknownUsername = UUID.randomUUID().toString()
        try {
            client.get(path: "/patients/" + rndUnknownUsername, accept: "application/json")
        } catch (RESTClientException ex) {
            assert ex.message == "404 Not Found"
            assert ex.response.contentLength == 0
        }

        // Verify duplication handling by inserting the same patient again
        try {
            response = client.post(path: '/patients') {
                type "application/json"
                charset "UTF-8"
                json username: rndUsername, patientID: "1234", firstname: "F1", lastname: "L1", weight: 100, height:200
            }
        } catch (RESTClientException ex) {
            assert ex.message == "409 Conflict"
            assert ex.response.contentLength > 0
        }

        // TODO Update and remove the patient...

//        def json = response.json
//
//        assert json.size() == 3
//        assert json.totalPages == 1
//        assert json.totalElements == 3
//        assert json.content.size() == 3
//        assert json.content.firstname.any() {
//            firstname ->
//                firstname.equals("Boyd")
//                firstname.equals("Carter")
//                firstname.equals("Dave")
//        }
    }

//    def assert200OK(response) {
//        assert 200 == response.statusCode
//        assert "OK" == response.statusMessage
//    }

}