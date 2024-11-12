package org.bugReportSystem.requests;

import java.util.Optional;

public record IssueUpdateRequest(Optional<String> assignee, String description, Integer id, String priority, String reporterEmail,
                                 String column, String title) {
}
