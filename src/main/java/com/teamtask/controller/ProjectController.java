package com.teamtask.controller;

import com.teamtask.dto.ProjectRequestDTO;
import com.teamtask.model.Project;
import com.teamtask.model.User;
import com.teamtask.repo.ProjectRepository;
import com.teamtask.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private UserRepository userRepo;

    /**
     * Any logged-in user can create a project.
     * The creator automatically becomes the project admin AND a member.
     */
    @PostMapping
    public ResponseEntity<Project> createProject(
            @Valid @RequestBody ProjectRequestDTO request,
            Authentication auth) {
        User currentUser = (User) auth.getPrincipal();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .admin(currentUser)
                .build();
        project.getMembers().add(currentUser);

        return ResponseEntity.ok(projectRepo.save(project));
    }

    /**
     * Global ADMIN sees all projects.
     * Members see only projects they belong to.
     */
    @GetMapping
    public ResponseEntity<List<Project>> getProjects(Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        if (currentUser.getRole().name().equals("ADMIN")) {
            return ResponseEntity.ok(projectRepo.findAll());
        }
        return ResponseEntity.ok(projectRepo.findByMembersContaining(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id, Authentication auth) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User currentUser = (User) auth.getPrincipal();

        boolean isGlobalAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(currentUser.getId()));

        if (!isGlobalAdmin && !isMember) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(project);
    }

    /**
     * Only the project's own admin (or global ADMIN) can edit it.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequestDTO request,
            Authentication auth) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User currentUser = (User) auth.getPrincipal();

        boolean isGlobalAdmin   = currentUser.getRole().name().equals("ADMIN");
        boolean isProjectAdmin  = project.getAdmin().getId().equals(currentUser.getId());

        if (!isGlobalAdmin && !isProjectAdmin) {
            return ResponseEntity.status(403)
                    .body(null);
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return ResponseEntity.ok(projectRepo.save(project));
    }

    /**
     * Only the project's own admin or global ADMIN can delete it.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProject(
            @PathVariable Long id, Authentication auth) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User currentUser = (User) auth.getPrincipal();

        boolean isGlobalAdmin  = currentUser.getRole().name().equals("ADMIN");
        boolean isProjectAdmin = project.getAdmin().getId().equals(currentUser.getId());

        if (!isGlobalAdmin && !isProjectAdmin) {
            return ResponseEntity.status(403).build();
        }

        projectRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }

    /**
     * Only the project admin or global ADMIN can add members.
     */
    @PostMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Project> addMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication auth) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User currentUser = (User) auth.getPrincipal();

        boolean isGlobalAdmin  = currentUser.getRole().name().equals("ADMIN");
        boolean isProjectAdmin = project.getAdmin().getId().equals(currentUser.getId());

        if (!isGlobalAdmin && !isProjectAdmin) {
            return ResponseEntity.status(403).build();
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean alreadyMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!alreadyMember) {
            project.getMembers().add(user);
        }
        return ResponseEntity.ok(projectRepo.save(project));
    }

    /**
     * Only the project admin or global ADMIN can remove members.
     */
    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Project> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication auth) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User currentUser = (User) auth.getPrincipal();

        boolean isGlobalAdmin  = currentUser.getRole().name().equals("ADMIN");
        boolean isProjectAdmin = project.getAdmin().getId().equals(currentUser.getId());

        if (!isGlobalAdmin && !isProjectAdmin) {
            return ResponseEntity.status(403).build();
        }
        if (project.getAdmin().getId().equals(userId)) {
            throw new RuntimeException("Cannot remove the project admin");
        }

        project.getMembers().removeIf(m -> m.getId().equals(userId));
        return ResponseEntity.ok(projectRepo.save(project));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<User>> getMembers(@PathVariable Long projectId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        return ResponseEntity.ok(project.getMembers());
    }
}