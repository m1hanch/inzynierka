package org.bugReportSystem.requests;

public record ChangePasswordRequest(String currentPassword, String newPassword, String retNewPassword) {
}
