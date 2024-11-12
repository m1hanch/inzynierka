package org.bugReportSystem.repositories;

import org.bugReportSystem.entities.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, Integer> {
}
