package com.teamtask.dto;

import com.teamtask.model.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDate dueDate;

    private Task.Priority priority = Task.Priority.MEDIUM;

    private Task.Status status = Task.Status.TODO;

    private Long assignedToId;

    @NotNull(message = "Project ID is required")
    private Long projectId;
}
