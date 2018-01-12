package org.smartregister.path.domain;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.repository.CohortRepository;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by keyman on 11/01/18.
 */
public class Cohort implements Serializable {
    private Long id;
    private String month;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMonth() {
        return month;
    }

    public Date getMonthAsDate() {
        if (StringUtils.isBlank(month)) {
            return null;
        }
        try {
            return CohortRepository.DF_YYYYMM.parse(month);
        } catch (ParseException e) {
            Log.e(Cohort.class.getName(), e.getMessage(), e);
            return null;
        }
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public void setMonth(Date date) {
        if (date != null) {
            try {
                this.month = CohortRepository.DF_YYYYMM.format(date);
            } catch (Exception e) {
                Log.e(Cohort.class.getName(), e.getMessage(), e);
            }
        }
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

