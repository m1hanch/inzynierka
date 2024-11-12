package org.bugReportSystem.requests;

import java.util.Set;

public record UserRegistrationRequest(
        String firstname,
        String lastname,
        String email,
        String password
) {
}
