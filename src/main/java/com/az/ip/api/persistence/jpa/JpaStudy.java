package com.az.ip.api.persistence.jpa;

import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import java.util.Date;

import static javax.persistence.TemporalType.DATE;

@Entity
public class JpaStudy extends AbstractEntity {

    @Column(unique=true)
    private String name;

    private String description;

    @Temporal(DATE)
    private Date startdate;

    @Temporal(DATE)
    private Date enddate;

    /**
     * Constructor for new (not yet persited) entities, without specifying id and version
     *
     * @param name
     */
    public JpaStudy(String name, String description, Date startdate, Date enddate) {
        Assert.hasText(name);

        this.name = name;
        this.description = description;
        this.startdate = startdate;
        this.enddate = enddate;
    }

    /**
     * Constructor for existing (already persisted) entities, specifying id, version together with the actual business information regarding the entity
     *
     * @param id
     * @param version
     * @param name
     */
    public JpaStudy(String id, int version, String name, String description, Date startdate, Date enddate) {
        this(name, description, startdate, enddate);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartdate() {
        return startdate;
    }

    public void setStartdate(Date startdate) {
        this.startdate = startdate;
    }

    public Date getEnddate() {
        return enddate;
    }

    public void setEnddate(Date enddate) {
        this.enddate = enddate;
    }
}