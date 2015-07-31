package com.az.ip.api.persistence.jpa;

import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class JpaDoctor extends AbstractEntity {

    @Column(unique=true)
    private String username;

    private String firstname;
    private String lastname;

    @ManyToMany(mappedBy="doctors", fetch = FetchType.EAGER)
    private Set<JpaStudy> studies = new HashSet<>();

    /**
     * Constructor for new (not yet persited) entities, without specifying id and version
     *
     * @param username
     * @param firstname
     * @param lastname
     */
    public JpaDoctor(String username, String firstname, String lastname) {

        Assert.hasText(username);
        Assert.hasText(firstname);
        Assert.hasText(lastname);

        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    /**
     * Constructor for existing (already persisted) entities, specifying id, version together with the actual business information regarding the entity
     *
     * @param id
     * @param version
     * @param username
     * @param firstname
     * @param lastname
     */
    public JpaDoctor(String id, int version, String username, String firstname, String lastname) {
        this(username, firstname, lastname);
        setIdAndVersionForExistingEntity(id, version);
    }

    /**
     * The default constructor is required by the JPA implementation, but can be set protected to protect is from public visibility
     */
    protected JpaDoctor() {}

    public String getUsername() {
        return username;
    }
    public String getFirstname() {
        return firstname;
    }
    public String getLastname() {
        return lastname;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Set<JpaStudy> getAssigendInStudies() {
        return studies;
    }

}