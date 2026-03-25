package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class ComposeResult {

    private boolean ok;
    private String message;
    private List<String> detectedRoles;

    public ComposeResult() {
    }

    public static ComposeResult success(String message, List<String> detectedRoles) {
        ComposeResult result = new ComposeResult();
        result.ok = true;
        result.message = message;
        result.detectedRoles = detectedRoles;
        return result;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetectedRoles() {
        return detectedRoles;
    }

    public void setDetectedRoles(List<String> detectedRoles) {
        this.detectedRoles = detectedRoles;
    }
}