package com.example.beinterviewprep.task.service;

import com.example.beinterviewprep.task.domain.TaskStatus;
import java.time.LocalDate;

public record TaskCommand(String title, String description, TaskStatus status, LocalDate dueDate) {}
