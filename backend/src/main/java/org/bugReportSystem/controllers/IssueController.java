package org.bugReportSystem.controllers;

import org.bugReportSystem.dtos.IssueDTO;
import org.bugReportSystem.requests.IssueUpdateRequest;
import org.bugReportSystem.services.IssueService;
import org.bugReportSystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/issues")
public class IssueController {
    private final IssueService issueService;
    private final UserService userService;

    @Autowired
    public IssueController(IssueService issueService, UserService userService) {
        this.issueService = issueService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> addIssue(@RequestBody IssueDTO issueDTO) {
        issueService.createIssue(issueDTO.title(), issueDTO.description(), issueDTO.reporterEmail());
        return new ResponseEntity<>("Issue created successfully", HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<?> getIssues() {
        return new ResponseEntity<>(issueService.getIssues(), HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam String column) {
        var issue = issueService.getIssueById(id);
        issueService.updateStatus(issue, column);
        return new ResponseEntity<>("Issue updated successfully", HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateIssue(@PathVariable String id, @RequestBody IssueUpdateRequest issue) {
        issueService.updateIssue(issue);
        return new ResponseEntity<>("Issue updated successfully", HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIssue(@PathVariable String id) {
        issueService.deleteIssue(id);
        return new ResponseEntity<>("Issue deleted successfully", HttpStatus.OK);
    }

}
