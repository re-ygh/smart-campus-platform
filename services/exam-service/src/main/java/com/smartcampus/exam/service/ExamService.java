package com.smartcampus.exam.service;

import com.smartcampus.exam.client.NotificationClient;
import com.smartcampus.exam.messaging.ExamEventPublisher;
import com.smartcampus.exam.model.*;
import com.smartcampus.exam.repository.*;
import com.smartcampus.exam.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final StudentAnswerRepository answerRepository;
    private final NotificationClient notificationClient;
    private final ExamEventPublisher eventPublisher;

    public Course createCourse(String title, String description, String teacher) {
        Course course = Course.builder()
                .title(title)
                .description(description)
                .teacherUsername(teacher)
                .tenantId(TenantContext.getTenantId())
                .build();
        return courseRepository.save(course);
    }

    public List<Course> listCourses() {
        return courseRepository.findByTenantId(TenantContext.getTenantId());
    }

    public Exam createExam(Long courseId, String title, LocalDateTime start, Integer durationMinutes) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        if (!course.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        Exam exam = Exam.builder()
                .course(course)
                .title(title)
                .startTime(start)
                .durationMinutes(durationMinutes != null ? durationMinutes : 60)
                .tenantId(TenantContext.getTenantId())
                .build();
        Exam saved = examRepository.save(exam);
        
        // Publish exam created event
        eventPublisher.publish("exam.created", Map.of(
                "examId", saved.getId(),
                "examTitle", saved.getTitle(),
                "courseTitle", course.getTitle(),
                "teacherUsername", course.getTeacherUsername(),
                "startTime", saved.getStartTime().toString(),
                "durationMinutes", saved.getDurationMinutes(),
                "tenantId", saved.getTenantId()
        ));
        
        return saved;
    }

    public List<Exam> listExams() {
        return examRepository.findByTenantId(TenantContext.getTenantId());
    }

    public List<Exam> listExamsForTeacher(String teacherUsername) {
        return examRepository.findByCourse_TeacherUsernameAndTenantId(teacherUsername, TenantContext.getTenantId());
    }

    @Transactional
    public ExamAttempt startExam(Long examId, String student, String authHeader) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!exam.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        
        // Check if student already has an attempt
        List<ExamAttempt> existing = attemptRepository.findByTenantIdAndStudentUsername(TenantContext.getTenantId(), student);
        boolean alreadyAttempted = existing.stream().anyMatch(a -> a.getExam().getId().equals(examId));
        if (alreadyAttempted) {
            throw new IllegalStateException("Student already attempted this exam");
        }
        
        // Calculate max score from questions
        List<Question> questions = questionRepository.findByExamIdAndTenantId(examId, TenantContext.getTenantId());
        int maxScore = questions.stream().mapToInt(Question::getPoints).sum();
        
        ExamAttempt attempt = ExamAttempt.builder()
                .exam(exam)
                .studentUsername(student)
                .startedAt(LocalDateTime.now())
                .maxScore(maxScore)
                .isCompleted(false)
                .tenantId(TenantContext.getTenantId())
                .build();
        ExamAttempt saved = attemptRepository.save(attempt);
        
        // Publish exam started event
        eventPublisher.publish("exam.started", Map.of(
                "examId", examId,
                "examTitle", exam.getTitle(),
                "studentUsername", student,
                "teacherUsername", exam.getCourse().getTeacherUsername(),
                "tenantId", TenantContext.getTenantId()
        ));
        
        return saved;
    }

    public List<ExamAttempt> attemptsForStudent(String student) {
        return attemptRepository.findByTenantIdAndStudentUsername(TenantContext.getTenantId(), student);
    }

    public Question addQuestion(Long examId, String questionText, List<String> options, Integer correctOptionIndex, Integer points) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!exam.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        
        Question question = Question.builder()
                .exam(exam)
                .questionText(questionText)
                .options(options)
                .correctOptionIndex(correctOptionIndex)
                .points(points)
                .tenantId(TenantContext.getTenantId())
                .build();
        return questionRepository.save(question);
    }

    public List<Question> listQuestions(Long examId) {
        return questionRepository.findByExamIdAndTenantId(examId, TenantContext.getTenantId());
    }

    public List<Question> listQuestionsForStudent(Long examId) {
        // Return questions without correct answers for students
        List<Question> questions = questionRepository.findByExamIdAndTenantId(examId, TenantContext.getTenantId());
        // Don't expose correctOptionIndex to students
        questions.forEach(q -> q.setCorrectOptionIndex(null));
        return questions;
    }

    @Transactional
    public ExamAttempt submitAnswers(Long attemptId, Map<Long, Integer> answers, String student) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        
        if (!attempt.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        
        if (!attempt.getStudentUsername().equals(student)) {
            throw new IllegalArgumentException("This attempt belongs to another student");
        }
        
        if (attempt.getIsCompleted()) {
            throw new IllegalStateException("Exam already submitted");
        }
        
        // Get all questions for this exam
        List<Question> questions = questionRepository.findByExamIdAndTenantId(
                attempt.getExam().getId(), TenantContext.getTenantId());
        
        int totalScore = 0;
        
        // Save student answers and calculate score
        for (Question question : questions) {
            Integer selectedOption = answers.get(question.getId());
            if (selectedOption == null) {
                selectedOption = -1; // Mark as unanswered
            }
            
            boolean isCorrect = selectedOption.equals(question.getCorrectOptionIndex());
            if (isCorrect) {
                totalScore += question.getPoints();
            }
            
            StudentAnswer answer = StudentAnswer.builder()
                    .attempt(attempt)
                    .questionId(question.getId())
                    .selectedOptionIndex(selectedOption)
                    .isCorrect(isCorrect)
                    .tenantId(TenantContext.getTenantId())
                    .build();
            answerRepository.save(answer);
        }
        
        attempt.setScore(totalScore);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setIsCompleted(true);
        ExamAttempt savedAttempt = attemptRepository.save(attempt);
        
        // Publish exam completed event
        eventPublisher.publish("exam.completed", Map.of(
                "attemptId", savedAttempt.getId(),
                "examId", attempt.getExam().getId(),
                "examTitle", attempt.getExam().getTitle(),
                "studentUsername", student,
                "teacherUsername", attempt.getExam().getCourse().getTeacherUsername(),
                "score", totalScore,
                "maxScore", attempt.getMaxScore(),
                "tenantId", savedAttempt.getTenantId()
        ));
        
        return savedAttempt;
    }

    public ExamAttempt getAttemptResult(Long attemptId, String student) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        
        if (!attempt.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        
        if (!attempt.getStudentUsername().equals(student)) {
            throw new IllegalArgumentException("This attempt belongs to another student");
        }
        
        if (!attempt.getIsCompleted()) {
            throw new IllegalStateException("Exam not yet completed");
        }
        
        return attempt;
    }

    public List<StudentAnswer> getStudentAnswers(Long attemptId) {
        return answerRepository.findByAttemptIdAndTenantId(attemptId, TenantContext.getTenantId());
    }

    @Transactional
    public void deleteExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!exam.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        
        // Delete all questions
        List<Question> questions = questionRepository.findByExamAndTenantId(exam, TenantContext.getTenantId());
        questionRepository.deleteAll(questions);
        
        // Delete all attempts and their answers
        List<ExamAttempt> attempts = attemptRepository.findByExamAndTenantId(exam, TenantContext.getTenantId());
        for (ExamAttempt attempt : attempts) {
            List<StudentAnswer> answers = answerRepository.findByAttemptAndTenantId(attempt, TenantContext.getTenantId());
            answerRepository.deleteAll(answers);
        }
        attemptRepository.deleteAll(attempts);
        
        examRepository.delete(exam);
    }
}

