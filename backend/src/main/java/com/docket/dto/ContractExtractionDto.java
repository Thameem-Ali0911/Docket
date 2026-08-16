package com.docket.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;

public class ContractExtractionDto {

    @NotNull(message = "contractTitle is required")
    private String contractTitle;

    @NotNull(message = "parties is required")
    private List<String> parties;

    @NotNull(message = "effectiveDate is required")
    private String effectiveDate;

    @NotNull(message = "governingLaw is required")
    private String governingLaw;

    @NotNull(message = "termOrDuration is required")
    private String termOrDuration;

    @NotNull(message = "totalValue is required")
    private String totalValue;

    public String getContractTitle() { return contractTitle; }
    public void setContractTitle(String contractTitle) { this.contractTitle = contractTitle; }

    public List<String> getParties() { return parties; }
    public void setParties(List<String> parties) { this.parties = parties; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getGoverningLaw() { return governingLaw; }
    public void setGoverningLaw(String governingLaw) { this.governingLaw = governingLaw; }

    public String getTermOrDuration() { return termOrDuration; }
    public void setTermOrDuration(String termOrDuration) { this.termOrDuration = termOrDuration; }

    public String getTotalValue() { return totalValue; }
    public void setTotalValue(String totalValue) { this.totalValue = totalValue; }
}
