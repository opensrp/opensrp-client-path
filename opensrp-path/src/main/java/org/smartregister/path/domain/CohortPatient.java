package org.smartregister.path.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by keyman on 11/01/18.
 */
public class CohortPatient implements Serializable {
    private Long id;
    private String baseEntityId;
    private Long cohortId;
    private String validVaccines;
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

    public Long getCohortId() {
        return cohortId;
    }

    public void setCohortId(Long cohortId) {
        this.cohortId = cohortId;
    }

    public String getValidVaccines() {
        return validVaccines;
    }

    public void setValidVaccines(String validVaccines) {
        this.validVaccines = validVaccines;
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

