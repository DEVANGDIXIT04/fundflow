package com.fundflow.exception;

/**
 * Thrown when a request is well-formed but violates a domain rule,
 * e.g. issuing a call that is not in DRAFT status. Mapped to HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
