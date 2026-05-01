package com.teamtask.repo;

import com.teamtask.model.Task;
import com.teamtask.model.Task.Status;
import com.teamtask.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByAssignedTo(User user);

    List<Task> findByStatus(Status status);

    List<Task> findByDueDateBeforeAndStatusNot(LocalDate date, Status status);

    @Query("SELECT t.assignedTo.name, COUNT(t) FROM Task t WHERE t.assignedTo IS NOT NULL GROUP BY t.assignedTo.id")
    List<Object[]> countTasksPerUser();

    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countByStatus();

    List<Task> findByProjectIdAndAssignedTo(Long projectId, User user);
}
