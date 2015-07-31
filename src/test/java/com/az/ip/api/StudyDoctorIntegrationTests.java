package com.az.ip.api;

import com.az.ip.api.persistence.jpa.DoctorRepository;
import com.az.ip.api.persistence.jpa.JpaDoctor;
import com.az.ip.api.persistence.jpa.JpaStudy;
import com.az.ip.api.persistence.jpa.StudyRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.inject.Inject;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Created by magnus on 31/07/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class StudyDoctorIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(StudyIntegrationTests.class);
    private static final String BASE_URI = "/api/studies";
    private static final String PROTOCOL = "http";

    @Value("${local.server.port}")
    int port;

    @Value("${mysuer:demo}")
    String user;

    @Value("${mypwd:omed.1}")
    String pwd;

    @Inject
    StudyRepository studyRepository;

    @Inject
    DoctorRepository doctorRepository;

    @Test
    public void testStudyDoctor() {

        JpaStudy study = createTestDbStudyEntity("S-1");
        JpaDoctor doctor = createTestDbDoctorEntity("D-1");

        doctorRepository.save(doctor);

        study.getAssigendDoctors().add(doctor);
        studyRepository.save(study);

        JpaDoctor doctor2 = doctorRepository.findByUsername("D-1");

        assertEquals(1, doctor2.getAssigendInStudies().size());
    }

    private JpaStudy createTestDbStudyEntity(String name) {
        return new JpaStudy(name, "description", new Date(), new Date());
    }

    private JpaDoctor createTestDbDoctorEntity(String username) {
        return new JpaDoctor(username, "F1", "L1");
    }

}