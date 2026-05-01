package com.teamtask.controller;

import com.teamtask.dto.TaskRequestDTO;
import com.teamtask.dto.TaskResponseDTO;
import com.teamtask.model.Project;
import com.teamtask.model.Task;
import com.teamtask.model.User;
import com.teamtask.repo.ProjectRepository;
import com.teamtask.repo.TaskRepository;
import com.teamtask.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private UserRepository userRepo;

    // Create task (admin only)
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @Valid @RequestBody TaskRequestDTO request,
            Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        if (!currentUser.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        Project project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setProject(project);

        if (request.getAssignedToId() != null) {
            User assignee = userRepo.findById(request.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignedTo(assignee);
        }

        return ResponseEntity.ok(TaskResponseDTO.fromTask(taskRepo.save(task)));
    }

    // Get all tasks (admin) or own tasks (member)
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getTasks(Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        List<Task> tasks;

        if (currentUser.getRole().name().equals("ADMIN")) {
            tasks = taskRepo.findAll();
        } else {
            tasks = taskRepo.findByAssignedTo(currentUser);
        }

        return ResponseEntity.ok(tasks.stream()
                .map(TaskResponseDTO::fromTask)
                .collect(Collectors.toList()));
    }

    // Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> getTask(
            @PathVariable Long id, Authentication auth) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User currentUser = (User) auth.getPrincipal();

        if (!currentUser.getRole().name().equals("ADMIN")
                && (task.getAssignedTo() == null
                    || !task.getAssignedTo().getId().equals(currentUser.getId()))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(TaskResponseDTO.fromTask(task));
    }

    // Get tasks by project
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponseDTO>> getTasksByProject(
            @PathVariable Long projectId, Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        List<Task> tasks;

        if (currentUser.getRole().name().equals("ADMIN")) {
            tasks = taskRepo.findByProjectId(projectId);
        } else {
            tasks = taskRepo.findByProjectIdAndAssignedTo(projectId, currentUser);
        }

        return ResponseEntity.ok(tasks.stream()
                .map(TaskResponseDTO::fromTask)
                .collect(Collectors.toList()));
    }

    // Update task - admin can update all fields, member can only update status
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Long id,
            @RequestBody TaskRequestDTO request,
            Authentication auth) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User currentUser = (User) auth.getPrincipal();

        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isAssignee = task.getAssignedTo() != null
                && task.getAssignedTo().getId().equals(currentUser.getId());

        if (!isAdmin && !isAssignee) {
            return ResponseEntity.status(403).build();
        }

        if (isAdmin) {
            task.setTitle(request.getTitle());
            task.setDescription(request.getDescription());
            task.setDueDate(request.getDueDate());
            task.setPriority(request.getPriority());
            if (request.getAssignedToId() != null) {
                User assignee = userRepo.findById(request.getAssignedToId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                task.setAssignedTo(assignee);
            } else {
                task.setAssignedTo(null);
            }
        }

        // Both admin and member can update status
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        return ResponseEntity.ok(TaskResponseDTO.fromTask(taskRepo.save(task)));
    }

    // Update status only (member-friendly endpoint)
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User currentUser = (User) auth.getPrincipal();

        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isAssignee = task.getAssignedTo() != null
                && task.getAssignedTo().getId().equals(currentUser.getId());

        if (!isAdmin && !isAssignee) {
            return ResponseEntity.status(403).build();
        }

        String newStatus = body.get("status");
        task.setStatus(Task.Status.valueOf(newStatus));
        return ResponseEntity.ok(TaskResponseDTO.fromTask(taskRepo.save(task)));
    }

    // Delete task (admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable Long id) {
        taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        taskRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
    }
}
