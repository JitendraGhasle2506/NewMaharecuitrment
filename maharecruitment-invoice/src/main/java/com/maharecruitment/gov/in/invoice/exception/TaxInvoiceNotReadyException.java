package com.maharecruitment.gov.in.invoice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TaxInvoiceNotReadyException extends TaxInvoiceException {

    public TaxInvoiceNotReadyException(String message) {
        super(message);
    }
}
