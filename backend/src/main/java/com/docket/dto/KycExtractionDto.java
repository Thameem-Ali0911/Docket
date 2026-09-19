package com.docket.dto;

import java.util.Map;
import jakarta.validation.constraints.NotNull;

public class KycExtractionDto {

    @NotNull(message = "fullName is required")
    private String fullName;

    @NotNull(message = "idType is required")
    private String idType;

    @NotNull(message = "idNumber is required")
    private String idNumber;

    @NotNull(message = "dateOfBirth is required")
    private String dateOfBirth;

    @NotNull(message = "nationality is required")
    private String nationality;

    @NotNull(message = "issueDate is required")
    private String issueDate;

    @NotNull(message = "expiryDate is required")
    private String expiryDate;

    @NotNull(message = "address is required")
    private String address;

    @NotNull(message = "verificationStatus is required")
    private String verificationStatus;

    private Map<String, Double> fieldConfidences;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public Map<String, Double> getFieldConfidences() { return fieldConfidences; }
    public void setFieldConfidences(Map<String, Double> fieldConfidences) { this.fieldConfidences = fieldConfidences; }
}
