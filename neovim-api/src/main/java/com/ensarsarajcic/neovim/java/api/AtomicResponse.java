package com.ensarsarajcic.neovim.java.api;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"results", "error"})
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AtomicResponse {
    @JsonProperty("results")
    private List<Object> results;
    @JsonProperty("error")
    private ErrorDescriptor error;
    AtomicResponse(
            @JsonProperty("results")
                    List<Object> results,
            @JsonProperty("error")
                    ErrorDescriptor error
    ) {
        this.results = results;
        this.error = error;
    }


    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"failedReqIdx", "errorType", "errorMessage"})
    static final class ErrorDescriptor {
        @JsonProperty("failedReqIdx")
        private int failedReqIdx;
        @JsonProperty("errorType")
        private int errorType;
        @JsonProperty("errorMessage")
        private int errorMessage;
        ErrorDescriptor(
                @JsonProperty("failedReqIdx")
                        int failedReqIdx,
                @JsonProperty("errorType")
                        int errorType,
                @JsonProperty("errorMessage")
                        int errorMessage
        ) {
            this.failedReqIdx = failedReqIdx;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }
  }
}
