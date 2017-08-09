package org.smartregister.path.domain;

/**
 * Created by raihan@mpower-social.com on 18-May-17.
 */
public class Stock {
    private Long id;
    private String vaccineTypeId;
    private String transactionType;
    private String providerid;
    private int value;
    private Long dateCreated;
    private String toFrom;
    private String syncStatus;
    private Long dateUpdated;

    public static final String issued = "issued";
    public static final String received = "received";
    public static final String loss_adjustment = "loss_adjustment";

    public Stock(Long id, String transactionType, String providerid, int value, Long dateCreated, String toFrom, String syncStatus, Long dateUpdated, String vaccineTypeId) {
        this.id = id;
        this.transactionType = transactionType;
        this.providerid = providerid;
        this.value = value;
        this.dateCreated = dateCreated;
        this.toFrom = toFrom;
        this.syncStatus = syncStatus;
        this.dateUpdated = dateUpdated;
        this.vaccineTypeId = vaccineTypeId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVaccineTypeId() {
        return vaccineTypeId;
    }

    public void setVaccineTypeId(String vaccineTypeId) {
        this.vaccineTypeId = vaccineTypeId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getToFrom() {
        return toFrom;
    }

    public void setToFrom(String toFrom) {
        this.toFrom = toFrom;
    }

    public Long getUpdatedAt() {
        return dateUpdated;
    }

    public void setUpdatedAt(Long dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String sync_status) {
        this.syncStatus = sync_status;
    }

    public String getProviderid() {
        return providerid;
    }

    public void setProviderid(String providerid) {
        this.providerid = providerid;
    }
}
