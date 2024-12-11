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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskRepository taskRepository;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter connect(String token, Authentication authentication) {
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(username, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(username);
            System.out.println("Connection completed for user: " + username);
        });

        emitter.onTimeout(() -> {
            emitters.remove(username);
            System.out.println("Connection timed out for user: " + username);
        });

        System.out.println("SSE connection established for user: " + username);

        return emitter;
    }

    @Override
    public List<NotificationDto> fetchAllNotifications(Long receiverId) {
        List<Notification> notifications = notificationRepository.findAllByReceiverId(receiverId);
        List<NotificationDto> notificationDtos = new ArrayList<>();
        for(Notification notification: notifications) {
            notificationDtos.add(getNotificationDto(notification));
        }
        return notificationDtos;
    }

    @Override
    public void dismissNotification(Long notificationId) {
        int rowsAffected = notificationRepository.dismissNotification(notificationId);
        if(rowsAffected == 0) {
            throw new RuntimeException("Cannot dismiss notification");
        }
    }

    @Override
    public void remindManager(Long taskId) {
        Employee admin = employeeRepository.findByUsername("admin").orElseThrow(() -> new UsernameNotFoundException("No admin found"));
        Long managerId = projectRepository.findManagerIdByTaskId(taskId);
        Employee manager = employeeRepository.findById(managerId).orElseThrow(() -> new UsernameNotFoundException("No manager found"));
        Long employeeId = taskRepository.findEmployeeIdByTaskId(taskId);
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new UsernameNotFoundException("No employee found"));
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("No task found"));
        Notification notification = new Notification();
        notification.setSender(admin);
        notification.setReceiver(manager);
        notification.setSubject(employee);
        notification.setReadStatus(false);
        notification.setTitle("Task rating reminder");
        notification.setDescription(employee.getName() + " has a task which has not been rated");
        notification.setTask(task);
        notificationRepository.save(notification);
        NotificationDto notificationDto = getNotificationDto(notification);
        sendNotificationToManager(manager.getUsername(), notificationDto);
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

    public void sendTaskToManager(String managerUsername, TaskDto taskDto) {
        SseEmitter emitter = emitters.get(managerUsername);
        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("taskCreated")
                        .data(taskDto));
            }
            catch (IOException e) {
                emitters.remove(managerUsername);
            }
        }
    }

    public void removeTaskFromManager(String managerUsername, TaskDto taskDto) {
        SseEmitter emitter = emitters.get(managerUsername);
        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("taskEdited")
                        .data(taskDto));
            }
            catch(IOException e) {
                emitters.remove(managerUsername);
            }
        }
    }

    public void sendNotificationToManager(String managerUsername, NotificationDto notificationDto) {
        SseEmitter emitter = emitters.get(managerUsername);
        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificationDto));
            }
            catch(IOException e) {
                emitters.remove(managerUsername);
            }
        }
    }

    public void removeNotificationFromManager(String managerUsername, NotificationDto notificationDto) {
        SseEmitter emitter = emitters.get(managerUsername);
        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("deleteNotification")
                        .data(notificationDto));
            }
            catch(IOException e) {
                emitters.remove(managerUsername);
            }
        }
    }

}
