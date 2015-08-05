package com.az.ip.api.persistence.jpa;

import org.springframework.util.Assert;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import java.util.UUID;

@MappedSuperclass
public abstract class AbstractEntity {

    // Use UUID as primary key to avoid OAWASP A4-Insecure Direct Object References
    // See: https://www.owasp.org/index.php/Top_10_2013-A4-Insecure_Direct_Object_References
    // See: http://blog.xebia.com/2009/06/03/jpa-implementation-patterns-using-uuids-as-primary-keys/

    // Replaced with UUID:     @GeneratedValue
    // Replaced with a String: private Long id;
    @Id

    // Thw Hibernate way, see "§5.1.2.2. Identifier generator" at https://docs.jboss.org/hibernate/core/4.3/manual/en-US/html/ch05.html#mapping-declaration-id
    //    @GeneratedValue(generator = "uuid")
    //    @GenericGenerator(name = "uuid", strategy = "uuid")

    private String id;

    // Adopt version based optimistic concurrency control
    // See: http://squirrel.pl/blog/2012/11/02/version-based-optimistic-concurrency-control-in-jpahibernate/
    @Version
    private int version;

    public AbstractEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public void setIdAndVersionForExistingEntity(String id, int version) {
        Assert.hasText(id);
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AbstractEntity)) {
            return false;
        }
        AbstractEntity other = (AbstractEntity) obj;
        return id.equals(other.getId());
    }
}