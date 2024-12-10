package com.beehyv.server.repository;

import com.beehyv.server.entity.Task;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO employee_tasks(employee_id, tasks_id) " +
            "VALUES(:employeeId, :taskId)",
            nativeQuery = true)
    void mapTaskIdWithEmployeeId(Long taskId, Long employeeId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO project_tasks(project_id, tasks_id) " +
            "VALUES(:projectId, :taskId)",
            nativeQuery = true)
    void mapTaskIdWithProjectId(Long taskId, Long projectId);

    @Query(value = "SELECT employee_id FROM employee_tasks " +
            "WHERE tasks_id = :taskId",
            nativeQuery = true)
    Long findEmployeeIdByTaskId(Long taskId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM employee_tasks " +
            "WHERE tasks_id = :taskId",
            nativeQuery = true)
    void deleteTaskMappingFromEmployee(Long taskId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM project_tasks " +
            "WHERE tasks_id = :taskId",
            nativeQuery = true)
    void deleteTaskMappingFromProject(Long taskId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM task " +
            "WHERE id = :taskId",
            nativeQuery = true)
    void deleteTaskById(Long taskId);
}
