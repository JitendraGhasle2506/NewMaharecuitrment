package com.maharecruitment.gov.in.invoice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TaxInvoiceNotFoundException extends TaxInvoiceException {

    public TaxInvoiceNotFoundException(String message) {
        super(message);
    }
}
