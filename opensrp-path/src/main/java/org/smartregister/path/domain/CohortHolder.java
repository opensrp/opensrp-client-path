package org.smartregister.path.domain;

import java.util.Date;

/**
 * Created by keyman on 1/15/18.
 */

public class CohortHolder {
    private Long cohortId;
    private Long size;

    public CohortHolder(Long cohortId, Long size) {
        this.cohortId = cohortId;
        this.size = size;
    }

    public CohortHolder(Long cohortId) {
        this.cohortId = cohortId;
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
}
