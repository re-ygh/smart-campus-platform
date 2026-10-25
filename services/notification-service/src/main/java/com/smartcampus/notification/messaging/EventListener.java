package com.smartcampus.notification.messaging;

import com.smartcampus.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "order.events")
    public void handleOrderEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            
            log.info("Received order event: {}", eventType);
            
            String userId = (String) payload.get("userId");
            String tenantId = (String) payload.get("tenantId");
            
            switch (eventType) {
                case "order.confirmed":
                    // Notify student about successful order
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> tickets = (List<Map<String, Object>>) payload.get("tickets");
                    
                    if (tickets != null && !tickets.isEmpty()) {
                        for (Map<String, Object> ticket : tickets) {
                            String ticketTitle = (String) ticket.get("title");
                            String ticketType = (String) ticket.get("type");
                            String studentMsg = String.format("✅ Successfully registered for %s: '%s'!", 
                                    ticketType != null ? ticketType.toLowerCase() : "event", ticketTitle);
                            notificationService.createWithTenant(userId, tenantId, studentMsg);
                            
                            // Notify assigned teacher if exists
                            String assignedTeacher = (String) ticket.get("assignedTeacher");
                            if (assignedTeacher != null && !assignedTeacher.isEmpty()) {
                                String teacherMsg = String.format("📋 Student %s registered for your %s: '%s'", 
                                        userId, ticketType != null ? ticketType.toLowerCase() : "event", ticketTitle);
                                notificationService.createWithTenant(assignedTeacher, tenantId, teacherMsg);
                            }
                        }
                    } else {
                        // Fallback if no ticket details
                        String confirmMsg = String.format("✅ Order #%s confirmed successfully!", 
                                payload.get("orderId"));
                        notificationService.createWithTenant(userId, tenantId, confirmMsg);
                    }
                    break;
                    
                case "order.failed":
                    String reason = (String) payload.getOrDefault("reason", "Unknown error");
                    String failMsg = String.format("❌ Order #%s failed: %s", 
                            payload.get("orderId"), reason);
                    notificationService.createWithTenant(userId, tenantId, failMsg);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing order event", e);
        }
    }

    @RabbitListener(queues = "reservation.events")
    public void handleReservationEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            
            log.info("Received reservation event: {}", eventType);
            
            String userId = (String) payload.get("userId");
            String tenantId = (String) payload.get("tenantId");
            String resourceName = (String) payload.get("resourceName");
            
            switch (eventType) {
                case "reservation.created":
                    // Only notify the student who made the reservation
                    String createMsg = String.format("✅ Reservation confirmed for '%s'! 📅", resourceName);
                    notificationService.createWithTenant(userId, tenantId, createMsg);
                    break;
                    
                case "reservation.cancelled":
                    // Only notify the student whose reservation was cancelled
                    String cancelMsg = String.format("🚫 Your reservation for '%s' has been cancelled.", resourceName);
                    notificationService.createWithTenant(userId, tenantId, cancelMsg);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing reservation event", e);
        }
    }

    @RabbitListener(queues = "exam.events")
    public void handleExamEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            
            log.info("Received exam event: {}", eventType);
            
            String tenantId = (String) payload.get("tenantId");
            
            switch (eventType) {
                case "exam.created":
                    // Only notify the teacher who created it
                    String examTitle = (String) payload.get("examTitle");
                    String courseTitle = (String) payload.get("courseTitle");
                    String teacher = (String) payload.get("teacherUsername");
                    
                    String teacherMsg = String.format("✅ Exam '%s' for course '%s' created successfully!", 
                            examTitle, courseTitle);
                    notificationService.createWithTenant(teacher, tenantId, teacherMsg);
                    break;
                    
                case "exam.started":
                    // Notify teacher when a student starts their exam
                    String studentWhoStarted = (String) payload.get("studentUsername");
                    String teacherUsername = (String) payload.get("teacherUsername");
                    String examTitleStarted = (String) payload.get("examTitle");
                    
                    if (teacherUsername != null) {
                        String teacherNotif = String.format("📝 Student %s started exam '%s'", 
                                studentWhoStarted, examTitleStarted);
                        notificationService.createWithTenant(teacherUsername, tenantId, teacherNotif);
                    }
                    break;
                    
                case "exam.completed":
                    // Notify student about their result
                    String student = (String) payload.get("studentUsername");
                    String examTitle2 = (String) payload.get("examTitle");
                    Integer score = ((Number) payload.get("score")).intValue();
                    Integer maxScore = ((Number) payload.get("maxScore")).intValue();
                    
                    String resultMsg = String.format("✅ You completed '%s'! Score: %d/%d (%.1f%%)", 
                            examTitle2, score, maxScore, (score * 100.0 / maxScore));
                    notificationService.createWithTenant(student, tenantId, resultMsg);
                    
                    // Also notify teacher about completion
                    String teacherUsername2 = (String) payload.get("teacherUsername");
                    if (teacherUsername2 != null) {
                        String teacherNotif2 = String.format("📊 Student %s completed exam '%s' - Score: %d/%d", 
                                student, examTitle2, score, maxScore);
                        notificationService.createWithTenant(teacherUsername2, tenantId, teacherNotif2);
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing exam event", e);
        }
    }

    @RabbitListener(queues = "ticket.events")
    public void handleTicketEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            
            log.info("Received ticket event: {}", eventType);
            
            String tenantId = (String) payload.get("tenantId");
            
            if ("ticket.created".equals(eventType)) {
                String title = (String) payload.get("title");
                String type = (String) payload.get("type");
                String assignedTeacher = (String) payload.get("assignedTeacher");
                String createdBy = (String) payload.get("createdBy");
                
                // Notify assigned teacher if exists
                if (assignedTeacher != null && !assignedTeacher.isEmpty()) {
                    String teacherMsg = String.format("🎓 New %s '%s' has been assigned to you!", 
                            type.toLowerCase(), title);
                    notificationService.createWithTenant(assignedTeacher, tenantId, teacherMsg);
                }
                
                // Notify faculty who created it
                String facultyMsg = String.format("✅ %s '%s' created successfully!", type, title);
                notificationService.createWithTenant(createdBy, tenantId, facultyMsg);
            }
        } catch (Exception e) {
            log.error("Error processing ticket event", e);
        }
    }
}

