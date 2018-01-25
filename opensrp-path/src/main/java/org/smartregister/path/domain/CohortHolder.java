package org.smartregister.path.domain;

import java.util.Date;

/**
 * Created by keyman on 1/15/18.
 */

public class CohortHolder {
    private Long cohortId;
    private Long size;
    private Date month;

    public CohortHolder(Long cohortId, Date month, Long size) {
        this.cohortId = cohortId;
        this.month = month;
        this.size = size;
    }

    public CohortHolder(Long cohortId, Date month) {
        this.cohortId = cohortId;
        this.month = month;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getCohortId() {
        return cohortId;
    }

    public Long getSize() {
        return size;
    }

    public Date getMonth() {
        return month;
    }
}
