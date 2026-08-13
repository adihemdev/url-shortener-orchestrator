package com.cs.urlshortenerorchestrator.targetapp.service;

public class ShortCodeGenerationException extends RuntimeException {
    public ShortCodeGenerationException(String message) {
        super(message);
    }

    public ShortCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

