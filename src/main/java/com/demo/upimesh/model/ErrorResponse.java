package com.demo.upimesh.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized error response for all API endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Object details;
    private String path;

    /**
     * Builder pattern support (manual implementation since Lombok @Builder doesn't work with annotation processor)
     */
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private Object details;
        private String path;

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder details(Object details) {
            this.details = details;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponse build() {
            ErrorResponse response = new ErrorResponse();
            response.timestamp = this.timestamp;
            response.status = this.status;
            response.error = this.error;
            response.message = this.message;
            response.details = this.details;
            response.path = this.path;
            return response;
        }
    }
}
