package org.smartregister.path.domain;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.repository.CumulativeIndicatorRepository;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by keyman on 11/01/18.
 */
public class CumulativeIndicator implements Serializable {
    private Long id;
    private Long cumulativeId;
    private String month;
    private String vaccine;
    private Long value;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCumulativeId() {
        return cumulativeId;
    }

    public void setCumulativeId(Long cumulativeId) {
        this.cumulativeId = cumulativeId;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Date getMonthAsDate() {
        if (StringUtils.isBlank(month)) {
            return null;
        }
        try {
            return CumulativeIndicatorRepository.DF_YYYYMM.parse(month);
        } catch (ParseException e) {
            Log.e(Cohort.class.getName(), e.getMessage(), e);
            return null;
        }
    }

    public void setMonth(Date date) {
        if (date != null) {
            try {
                this.month = CumulativeIndicatorRepository.DF_YYYYMM.format(date);
            } catch (Exception e) {
                Log.e(Cohort.class.getName(), e.getMessage(), e);
            }
        }
    }

    public String getVaccine() {
        return vaccine;
    }

    public void setVaccine(String vaccine) {
        this.vaccine = vaccine;
    }


    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
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

