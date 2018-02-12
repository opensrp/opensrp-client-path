package org.smartregister.path.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by keyman on 22/01/18.
 */
public class CumulativePatient implements Serializable {
    private Long id;
    private String baseEntityId;
    private String validVaccines;
    private String invalidVaccines;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public void setBaseEntityId(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }

    public String getValidVaccines() {
        return validVaccines;
    }

    public void setValidVaccines(String validVaccines) {
        this.validVaccines = validVaccines;
    }

    public String getInvalidVaccines() {
        return invalidVaccines;
    }

    public void setInvalidVaccines(String invalidVaccines) {
        this.invalidVaccines = invalidVaccines;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}

