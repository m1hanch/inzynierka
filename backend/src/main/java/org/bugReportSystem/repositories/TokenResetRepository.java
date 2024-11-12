package org.bugReportSystem.repositories;

import org.bugReportSystem.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenResetRepository extends JpaRepository<PasswordResetToken, Integer> {

    PasswordResetToken findByUserEmail(String email);

    PasswordResetToken findByToken(String token);

}