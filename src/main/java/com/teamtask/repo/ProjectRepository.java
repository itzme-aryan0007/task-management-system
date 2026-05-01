package com.teamtask.repo;

import com.teamtask.model.Project;
import com.teamtask.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByAdmin(User admin);
    List<Project> findByMembersContaining(User user);
}
