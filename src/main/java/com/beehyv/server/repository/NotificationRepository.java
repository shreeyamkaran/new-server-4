package com.beehyv.server.repository;

import com.beehyv.server.entity.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByReceiverId(Long receiverId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE notification " +
            "SET read_status = true " +
            "WHERE id = :notificationId",
            nativeQuery = true)
    int dismissNotification(Long notificationId);
}
