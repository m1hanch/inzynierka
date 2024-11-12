package org.bugReportSystem.requests;

import jakarta.annotation.Nullable;
import org.springframework.web.multipart.MultipartFile;

public record UserUpdateRequest(
        String firstname, String lastname, String email, String password,
        String calendarUrl, Integer availableFromHour, Integer availableToHour, String availableDays,
        Integer meetingDuration, @Nullable MultipartFile profilePicture
) {
}
