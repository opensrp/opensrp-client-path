package org.smartregister.path.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by keyman on 1/15/18.
 */

public class CoverageHolder implements Serializable {
    private Long id;
    private Long size;
    private Date date;

    public CoverageHolder(Long id, Date date, Long size) {
        this.id = id;
        this.date = date;
        if (size != null && size > 0L) {
            this.size = size;
        }
    }

    public CoverageHolder(Long id, Date date) {
        this.id = id;
        this.date = date;
    }

    public void setSize(Long size) {
        if (size != null && size > 0L) {
            this.size = size;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getSize() {
        return size;
    }

    public Date getDate() {
        return date;
    }
}
