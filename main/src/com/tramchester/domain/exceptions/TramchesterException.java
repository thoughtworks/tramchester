package com.tramchester.domain.exceptions;


@SuppressWarnings("serial")
public class TramchesterException extends Exception {
    public TramchesterException(String message) {
        super(message);
    }

    public TramchesterException(String msg, Exception e) {
        super(msg,e);
    }
}
