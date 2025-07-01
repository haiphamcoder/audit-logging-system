package com.haiphamcoder.auditlog.common.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.haiphamcoder.auditlog.common.utils.MapperUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogDto {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("action")
    private String action;

    @JsonProperty("resource")
    private String resource;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("request_path")
    private String requestPath;

    @JsonProperty("status")
    private String status;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("additional_data")
    private Map<String, Object> additionalData;

    @Override
    public String toString() {
        return MapperUtils.toJson(this);
    }
}
