package com.az.ip.api

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import wslite.http.auth.HTTPBasicAuthorization
import wslite.rest.RESTClient
import wslite.rest.RESTClientException

import static org.junit.Assert.fail

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
class PatientSystemIntegrationTests {

    @Value('${myhost:docker}')
    String host

    @Value('${myport:8080}')
    String port

    @Value('${mysuer:demo}')
    String user

    @Value('${mypwd:omed.1}')
    String pwd

    String baseUrl
    RESTClient client

    @Before
    def void before() {
        baseUrl = "https://$host:$port"
        println "Perform tests using base url: $baseUrl"
        client = createRestClient(baseUrl)
        client.authorization = new HTTPBasicAuthorization(user, pwd)
    }

    @Test
    public void testLoginErrorNoHttps() {
        RESTClient invalidClient = createRestClient("http://$host:$port")

        try {
            invalidClient.get(path: "/patients", accept: "application/json")
            fail("Expected an error due to http access to a https protected resource");

        } catch (RESTClientException ex) {
            assert ex.message == "Unexpected end of file from server"
            assert ex.response == null
        }
    }

    @Test
    public void testLoginErrorNoCredentials() {
        RESTClient invalidClient = createRestClient(baseUrl)

        try {
            invalidClient.get(path: "/patients", accept: "application/json")
            fail("Expected an error due to access to a protected resource without any credentials specified");

        } catch (RESTClientException ex) {
            assert ex.message == "404 Not Found"
            assert ex.response.contentLength == 0
            assert ex.response.headers.get("WWW-Authenticate") == 'Basic realm="Spring"'
        }
    }

    @Test
    public void testLoginErrorInvalidCredentials() {
        RESTClient invalidClient = createRestClient(baseUrl)
        invalidClient.authorization = new HTTPBasicAuthorization("non-exisintg-user", "invalid-password")

        try {
            invalidClient.get(path: "/patients", accept: "application/json")
            fail("Expected an error due to access to a protected resource with incorrect credentials specified");

        } catch (RESTClientException ex) {
            assert ex.message == "404 Not Found"
            assert ex.response.contentLength == 0
            assert ex.response.headers.get("WWW-Authenticate") == 'Basic realm="Spring"'
        }
    }

    @Test
    def void testItAll() {

        // Get current number of patients
        def response = client.get(path: "/patients", accept: "application/json")
        assert response.statusCode == 200
        def cnt = response.json.size()

//        println response.contentAsString
        println "No of existing patients: " + cnt

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

    }

    private RESTClient createRestClient(String baseUrl) {
        client = new RESTClient(baseUrl)
        client.httpClient.sslTrustAllCerts = true
        return client
    }

}