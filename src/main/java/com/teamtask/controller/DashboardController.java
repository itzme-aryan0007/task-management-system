package com.teamtask.controller;

import com.teamtask.model.Task;
import com.teamtask.model.User;
import com.teamtask.repo.ProjectRepository;
import com.teamtask.repo.TaskRepository;
import com.teamtask.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private UserRepository userRepo;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        Map<String, Object> stats = new LinkedHashMap<>();

        List<Task> allTasks = isAdmin
                ? taskRepo.findAll()
                : taskRepo.findByAssignedTo(currentUser);

        // Total tasks
        stats.put("totalTasks", allTasks.size());

        // Tasks by status
        Map<String, Long> byStatus = allTasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(), Collectors.counting()));
        stats.put("tasksByStatus", byStatus);

        // Overdue tasks
        long overdue = allTasks.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isBefore(LocalDate.now())
                        && t.getStatus() != Task.Status.DONE)
                .count();
        stats.put("overdueTasks", overdue);

        // Tasks per user (admin only)
        if (isAdmin) {
            List<Object[]> perUser = taskRepo.countTasksPerUser();
            Map<String, Long> tasksPerUser = new LinkedHashMap<>();
            for (Object[] row : perUser) {
                tasksPerUser.put((String) row[0], (Long) row[1]);
            }
            stats.put("tasksPerUser", tasksPerUser);
            stats.put("totalProjects", projectRepo.count());
            stats.put("totalUsers", userRepo.count());
        }

        // Priority breakdown
        Map<String, Long> byPriority = allTasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPriority().name(), Collectors.counting()));
        stats.put("tasksByPriority", byPriority);

        return ResponseEntity.ok(stats);
    }
}
