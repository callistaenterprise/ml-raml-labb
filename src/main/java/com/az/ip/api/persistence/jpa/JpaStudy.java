package com.az.ip.api.persistence.jpa;

import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class JpaStudy extends AbstractEntity {

    @Column(unique=true)
    private String name;

    /**
     * Constructor for new (not yet persited) entities, without specifying id and version
     *
     * @param name
     */
    public JpaStudy(String name) {

        Assert.hasText(name);

        this.name = name;
    }

    /**
     * Constructor for existing (already persisted) entities, specifying id, version together with the actual business information regarding the entity
     *
     * @param id
     * @param version
     * @param name
     */
    public JpaStudy(String id, int version, String name) {
        this(name);
        setIdAndVersionForExistingEntity(id, version);
    }

    /**
     * The default constructor is required by the JPA implementation, but can be set protected to protect is from public visibility
     */
    protected JpaStudy() {}

    public String getName() {
        return name;
    }

    public void setName(String username) {
        this.name = username;
    }
}