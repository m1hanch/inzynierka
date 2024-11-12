package org.bugReportSystem.requests;

public record PasswordResetRequest(String password, String retPassword) {
}
