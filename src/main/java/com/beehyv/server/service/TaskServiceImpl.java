package com.beehyv.server.service;

import com.beehyv.server.dto.NotificationDto;
import com.beehyv.server.dto.TaskDto;
import com.beehyv.server.entity.Employee;
import com.beehyv.server.entity.Notification;
import com.beehyv.server.entity.Task;
import com.beehyv.server.repository.EmployeeRepository;
import com.beehyv.server.repository.NotificationRepository;
import com.beehyv.server.repository.ProjectRepository;
import com.beehyv.server.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationServiceImpl notificationService;

    @Override
    public List<Task> fetchAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Task fetchTaskById(Long taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    @Override
    public void updateTask(Long taskId, TaskDto task) {
        Task previousTask = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("No task found"));
        List<Notification> notifications = notificationRepository.findAllByTaskId(taskId);
        String oldStatus = previousTask.getAppraisalStatus();
        String newStatus = task.getAppraisalStatus();
        Long oldProjectId = projectRepository.findProjectIdByTaskId(taskId);
        Long newProjectId = task.getProjectId();
        previousTask.setTitle(task.getTitle());
        previousTask.setDescription(task.getDescription());
        previousTask.setDate(task.getDate());
        previousTask.setDuration(task.getDuration());
        previousTask.setAppraisalStatus(task.getAppraisalStatus());
        if(previousTask.getNumberOfRatings() > 0) {
            System.out.println("line number 54");
            Long employeeId = taskRepository.findEmployeeIdByTaskId(taskId);
            Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new UsernameNotFoundException("No employee found"));
            employee.setRatings((employee.getRatings() * employee.getNumberOfRatings() - previousTask.getRatings()) / (employee.getNumberOfRatings() - 1));
            employee.setNumberOfRatings(employee.getNumberOfRatings() - 1);
            employeeRepository.save(employee);
        }
        previousTask.setNumberOfRatings(0);
        previousTask.setRatings(0.0);
        task.setNumberOfRatings(0);
        task.setRatings(0.0);
        if(!Objects.equals(oldProjectId, newProjectId)) {
            Long oldManagerId = projectRepository.findManagerIdByTaskId(taskId);
            Employee oldManager = employeeRepository.findById(oldManagerId).orElseThrow(() -> new UsernameNotFoundException("No manager found"));
            projectRepository.updateProjectByTaskId(newProjectId, taskId);
            notificationService.removeTaskFromManager(oldManager.getUsername(), task);
            notificationService.removeTaskFromManager("admin", task);
            for(Notification notification: notifications) {
                if(!notification.getReadStatus()) {
                    NotificationDto notificationDto = getNotificationDto(notification);
                    notificationService.removeNotificationFromManager(notification.getReceiver().getUsername(), notificationDto);
                }
            }
            notificationRepository.deleteNotificationByTaskId(taskId);
            if(Objects.equals(newStatus, "APPLIED_FOR_APPRAISAL")) {
                Long newManagerId = projectRepository.findManagerIdByTaskId(taskId);
                Employee newManager = employeeRepository.findById(newManagerId).orElseThrow(() -> new UsernameNotFoundException("No manager found"));
                notificationService.sendTaskToManager(newManager.getUsername(), task);
                notificationService.sendTaskToManager("admin", task);
                Long employeeId = taskRepository.findEmployeeIdByTaskId(taskId);
                Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new UsernameNotFoundException("No employee found"));
                Notification notification = new Notification();
                notification.setSender(employee);
                notification.setReceiver(newManager);
                notification.setSubject(employee);
                notification.setReadStatus(false);
                notification.setTitle("Task added for appraisal");
                notification.setDescription(employee.getName() + " has added a task for appraisal");
                Task newTask = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("No task found"));
                notification.setTask(newTask);
                notificationRepository.save(notification);
                NotificationDto notificationDto = getNotificationDto(notification);
                notificationService.sendNotificationToManager(newManager.getUsername(), notificationDto);
                Employee admin = employeeRepository.findByUsername("admin").orElseThrow(() -> new UsernameNotFoundException("No admin found"));
                Notification notification1 = new Notification();
                notification1.setSender(employee);
                notification1.setReceiver(admin);
                notification1.setSubject(employee);
                notification1.setReadStatus(false);
                notification1.setTitle("Task added for appraisal");
                notification1.setDescription(employee.getName() + " has added a task for appraisal");
                notification1.setTask(newTask);
                notificationRepository.save(notification1);
                NotificationDto notificationDto1 = getNotificationDto(notification1);
                notificationService.sendNotificationToManager("admin", notificationDto1);
            }
            return;
        }
        projectRepository.updateProjectByTaskId(newProjectId, taskId);
        if(Objects.equals(oldStatus, "APPLIED_FOR_APPRAISAL") &&
                Objects.equals(newStatus, "DID_NOT_APPLY")) {
            Long managerId = projectRepository.findManagerIdByTaskId(taskId);
            Employee manager = employeeRepository.findById(managerId).orElseThrow(() -> new UsernameNotFoundException("No manager found"));
            notificationService.removeTaskFromManager(manager.getUsername(), task);
            notificationService.removeTaskFromManager("admin", task);
            for(Notification notification: notifications) {
                if(!notification.getReadStatus()) {
                    NotificationDto notificationDto = getNotificationDto(notification);
                    notificationService.removeNotificationFromManager(notification.getReceiver().getUsername(), notificationDto);
                }
            }
            notificationRepository.deleteNotificationByTaskId(taskId);
        }
        if(Objects.equals(oldStatus, "DID_NOT_APPLY") &&
                Objects.equals(newStatus, "APPLIED_FOR_APPRAISAL")) {
            Long managerId = projectRepository.findManagerIdByTaskId(taskId);
            Employee manager = employeeRepository.findById(managerId).orElseThrow(() -> new UsernameNotFoundException("No manager found"));
            notificationService.sendTaskToManager(manager.getUsername(), task);
            notificationService.sendTaskToManager("admin", task);
            Long employeeId = taskRepository.findEmployeeIdByTaskId(taskId);
            Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new UsernameNotFoundException("No employee found"));
            Notification notification = new Notification();
            notification.setSender(employee);
            notification.setReceiver(manager);
            notification.setSubject(employee);
            notification.setReadStatus(false);
            notification.setTitle("Task added for appraisal");
            notification.setDescription(employee.getName() + " has added a task for appraisal");
            Task newTask = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("No task found"));
            notification.setTask(newTask);
            notificationRepository.save(notification);
            NotificationDto notificationDto = getNotificationDto(notification);
            notificationService.sendNotificationToManager(manager.getUsername(), notificationDto);
            Employee admin = employeeRepository.findByUsername("admin").orElseThrow(() -> new UsernameNotFoundException("No admin found"));
            Notification notification1 = new Notification();
            notification1.setSender(employee);
            notification1.setReceiver(admin);
            notification1.setSubject(employee);
            notification1.setReadStatus(false);
            notification1.setTitle("Task added for appraisal");
            notification1.setDescription(employee.getName() + " has added a task for appraisal");
            notification1.setTask(newTask);
            notificationRepository.save(notification1);
            NotificationDto notificationDto1 = getNotificationDto(notification1);
            notificationService.sendNotificationToManager("admin", notificationDto1);
        }
    }

    @Override
    public TaskDto createTask(Long employeeId, TaskDto taskDto) {
        Task task = new Task();
        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setDate(taskDto.getDate());
        task.setDuration(taskDto.getDuration());
        task.setAppraisalStatus(taskDto.getAppraisalStatus());
        task.setRatings(taskDto.getRatings());
        task.setNumberOfRatings(taskDto.getNumberOfRatings());
        Task createdTask = taskRepository.save(task);
        taskRepository.mapTaskIdWithEmployeeId(createdTask.getId(), employeeId);
        taskRepository.mapTaskIdWithProjectId(createdTask.getId(), taskDto.getProjectId());
        taskDto.setId(createdTask.getId());
        if(Objects.equals(taskDto.getAppraisalStatus(), "APPLIED_FOR_APPRAISAL")) {
            Long managerId = projectRepository.findManagerIdByTaskId(createdTask.getId());
            Employee manager = employeeRepository.findById(managerId).orElseThrow(() -> new UsernameNotFoundException("No manager found"));
            Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new UsernameNotFoundException("No employee found"));
            Notification notification = new Notification();
            notification.setSender(employee);
            notification.setReceiver(manager);
            notification.setSubject(employee);
            notification.setReadStatus(false);
            notification.setTitle("Task added for appraisal");
            notification.setDescription(employee.getName() + " has added a task for appraisal");
            notification.setTask(createdTask);
            notificationRepository.save(notification);
            NotificationDto notificationDto = getNotificationDto(notification);
            notificationService.sendTaskToManager(manager.getUsername(), taskDto);
            notificationService.sendTaskToManager("admin", taskDto);
            notificationService.sendNotificationToManager(manager.getUsername(), notificationDto);
            Employee admin = employeeRepository.findByUsername("admin").orElseThrow(() -> new UsernameNotFoundException("No admin found"));
            Notification notification1 = new Notification();
            notification1.setSender(employee);
            notification1.setReceiver(admin);
            notification1.setSubject(employee);
            notification1.setReadStatus(false);
            notification1.setTitle("Task added for appraisal");
            notification1.setDescription(employee.getName() + " has added a task for appraisal");
            notification1.setTask(createdTask);
            notificationRepository.save(notification1);
            NotificationDto notificationDto1 = getNotificationDto(notification1);
            notificationService.sendNotificationToManager("admin", notificationDto1);
        }
        return taskDto;
    }

    private static NotificationDto getNotificationDto(Notification notification) {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setId(notification.getId());
        notificationDto.setSenderId(notification.getSender().getId());
        notificationDto.setReceiverId(notification.getReceiver().getId());
        notificationDto.setSubjectId(notification.getSubject().getId());
        notificationDto.setReadStatus(notification.getReadStatus());
        notificationDto.setTitle(notification.getTitle());
        notificationDto.setDescription(notification.getDescription());
        notificationDto.setTaskId(notification.getTask().getId());
        return notificationDto;
    }

    @Override
    public void rateTask(Long taskId, Double rating) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if(task == null) {
            return;
        }
        task.setRatings(rating);
        task.setNumberOfRatings(1);
        taskRepository.save(task);
        Long employeeId = taskRepository.findEmployeeIdByTaskId(taskId);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if(employee == null) {
            return;
        }
        employee.setRatings((employee.getRatings() * employee.getNumberOfRatings() + rating) / (employee.getNumberOfRatings() + 1));
        employee.setNumberOfRatings(employee.getNumberOfRatings() + 1);
        employeeRepository.save(employee);
    }

    @Override
    public void updateTaskRating(Long taskId, Double rating) {
        Task task = taskRepository.findById(taskId).orElse(null);
        Double oldRating = task.getRatings();
        if(task == null) {
            return;
        }
        task.setRatings(rating);
        taskRepository.save(task);
        Long employeeId = taskRepository.findEmployeeIdByTaskId(taskId);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if(employee == null) {
            return;
        }
        employee.setRatings((employee.getRatings() * employee.getNumberOfRatings() - oldRating + rating) / employee.getNumberOfRatings());
        employeeRepository.save(employee);
    }

    @Override
    public void deleteTask(Long taskId, TaskDto taskDto) {
        Long managerId = projectRepository.findManagerIdByTaskId(taskId);
        Employee manager = employeeRepository.findById(managerId).orElseThrow(() -> new UsernameNotFoundException("No manager found"));
        notificationService.removeTaskFromManager(manager.getUsername(), taskDto);
        notificationService.removeTaskFromManager("admin", taskDto);
        taskRepository.deleteTaskMappingFromEmployee(taskId);
        taskRepository.deleteTaskMappingFromProject(taskId);
        List<Notification> notifications = notificationRepository.findAllByTaskId(taskId);
        for(Notification notification: notifications) {
            if(!notification.getReadStatus()) {
                NotificationDto notificationDto = getNotificationDto(notification);
                notificationService.removeNotificationFromManager(notification.getReceiver().getUsername(), notificationDto);
            }
        }
        notificationRepository.deleteNotificationByTaskId(taskId);
        taskRepository.deleteTaskById(taskId);
    }

}
