package com.docket.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;

/**
 * Structured fields extracted from an invoice by Gemini.
 * Field names/order here define the JSON schema sent to Gemini's
 * structured-output config (see prompt/ExtractInvoicePrompt.java) -
 * keep them in sync.
 */
public class InvoiceExtractionDto {

    @NotBlank
    private String vendorName;

    @NotBlank
    private String invoiceNumber;

    @NotBlank
    private String invoiceDate;

    private String dueDate;

    @NotBlank
    private String totalAmount;

    @NotEmpty
    @Valid
    private List<LineItem> lineItems;

    public static class LineItem {
        @NotBlank
        private String description;
        private String quantity;
        private String unitPrice;
        private String amount;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getQuantity() { return quantity; }
        public void setQuantity(String quantity) { this.quantity = quantity; }
        public String getUnitPrice() { return unitPrice; }
        public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
    }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public List<LineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<LineItem> lineItems) { this.lineItems = lineItems; }
}
