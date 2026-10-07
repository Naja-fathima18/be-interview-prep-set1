package com.example.beinterviewprep.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(length = 1000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskStatus status;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Task(
      String title, String description, TaskStatus status, LocalDate dueDate, Instant createdAt) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.dueDate = dueDate;
    this.createdAt = createdAt;
  }

  public void update(String title, String description, TaskStatus status, LocalDate dueDate) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.dueDate = dueDate;
  }
}
