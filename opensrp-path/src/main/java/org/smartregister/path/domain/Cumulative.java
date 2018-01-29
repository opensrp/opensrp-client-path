package org.smartregister.path.domain;

import android.util.Log;

import org.smartregister.path.repository.CumulativeRepository;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by keyman on 22/01/18.
 */
public class Cumulative implements Serializable {
    private Long id;
    private Integer year;
    private Long csoNumber;
    private Long zeirNumber;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Date getYearAsDate() {
        if (year == null) {
            return null;
        }
        try {
            return CumulativeRepository.DF_YYYY.parse(year.toString());
        } catch (ParseException e) {
            Log.e(Cohort.class.getName(), e.getMessage(), e);
            return null;
        }
    }

    public void setYear(Date date) {
        if (date != null) {
            try {
                this.year = Integer.valueOf(CumulativeRepository.DF_YYYY.format(date));
            } catch (Exception e) {
                Log.e(Cohort.class.getName(), e.getMessage(), e);
            }
        }
    }

    public Long getCsoNumber() {
        return csoNumber;
    }

    public void setCsoNumber(Long csoNumber) {
        this.csoNumber = csoNumber;
    }

    public Long getZeirNumber() {
        return zeirNumber;
    }

    public void setZeirNumber(Long zeirNumber) {
        this.zeirNumber = zeirNumber;
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

