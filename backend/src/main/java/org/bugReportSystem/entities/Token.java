package org.bugReportSystem.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "token")

public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Integer id;
    @Column(name = "user_id", updatable = false)
    private Integer userID;
    @Column(name = "content", nullable = false)
    private String content;

    public Token() {
    }

    public Token(Integer userID, String content) {
        this.userID = userID;
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer id) {
        this.userID = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return Objects.equals(id, token.id) && Objects.equals(userID, token.userID) && Objects.equals(content, token.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userID, content);
    }

    @Override
    public String toString() {
        return "Token{" +
                "id=" + id +
                ", userID=" + userID +
                ", content='" + content + '\'' +
                '}';
    }
}