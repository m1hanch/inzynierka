package org.bugReportSystem.entities;

import jakarta.persistence.*;
import org.bugReportSystem.enums.IssueColumn;
import org.bugReportSystem.enums.Priority;


@Entity
@Table(name = "issues")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Integer id;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_column")
    private IssueColumn column;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "reporterEmail", nullable = false)
    private String reporterEmail;

    public Issue() {
    }

    public Issue(String title, IssueColumn column, Priority priority, String description, String reporterEmail) {
        this.title = title;
        this.column = column;
        this.priority = priority;
        this.description = description;
        this.reporterEmail = reporterEmail;
    }

    public Issue(String title, String description, String reporterEmail) {
        this.title = title;
        this.description = description;
        this.reporterEmail = reporterEmail;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public IssueColumn getColumn() {
        return column;
    }

    public void setIssueColumn(IssueColumn column) {
        this.column = column;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User user) {
        this.assignee = user;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }
}
