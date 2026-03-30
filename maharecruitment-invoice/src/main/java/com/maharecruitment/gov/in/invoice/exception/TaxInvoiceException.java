package com.maharecruitment.gov.in.invoice.exception;

public class TaxInvoiceException extends RuntimeException {

    public TaxInvoiceException(String message) {
        super(message);
    }

    public TaxInvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
