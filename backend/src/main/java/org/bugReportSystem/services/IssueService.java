package org.bugReportSystem.services;

import org.bugReportSystem.entities.Issue;
import org.bugReportSystem.enums.IssueColumn;
import org.bugReportSystem.enums.Priority;
import org.bugReportSystem.repositories.IssueRepository;
import org.bugReportSystem.repositories.UserRepository;
import org.bugReportSystem.requests.IssueUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    public IssueService(IssueRepository issueRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    public void createIssue(String title, String description, String reporterEmail) {
        issueRepository.save(new Issue(title, IssueColumn.BACKLOG, Priority.MEDIUM, description, reporterEmail));
    }

    public Iterable<Issue> getIssues() {
        return issueRepository.findAll();
    }

    public Issue getIssueById(String id) {
        return issueRepository.findById(Integer.parseInt(id)).orElseGet(() -> null);
    }

    public void updateStatus(Issue issue, String column) {
        issue.setIssueColumn(IssueColumn.valueOf(column.toUpperCase()));
        issueRepository.save(issue);
    }

    public void updateIssue(IssueUpdateRequest issue) {
        Issue issueToUpdate = issueRepository.findById(issue.id()).orElseGet(() -> null);
        if (issueToUpdate == null) {
            return;
        }
        if (issue.assignee().isPresent()) {
            issueToUpdate.setAssignee(userRepository.findById(Integer.parseInt(issue.assignee().get())).orElseGet(() -> null));
        }
        issueToUpdate.setPriority(Priority.valueOf(issue.priority().toUpperCase()));
        issueToUpdate.setIssueColumn(IssueColumn.valueOf(issue.column().toUpperCase()));
        issueToUpdate.setTitle(issue.title());
        issueToUpdate.setDescription(issue.description());
        issueToUpdate.setReporterEmail(issue.reporterEmail());
        issueRepository.save(issueToUpdate);
    }

    public void deleteIssue(String id) {
        issueRepository.deleteById(Integer.parseInt(id));
    }
}
