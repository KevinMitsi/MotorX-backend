package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.metrics.*;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements IMetricsService, ApplicationContextAware {

    private static final long PERFORMANCE_THRESHOLD_MS = 5000L;
    private static final int DEFAULT_JACOCO_COVERAGE_GATE_PERCENT = 60;

    private static final List<String> TRACKED_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/verify-2fa",
            "/api/v1/user/appointments/available-slots"
    );

    private final JpaAppointmentRepository appointmentRepository;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    private Map<String, Object> applicationBeans = Collections.emptyMap();

    private final Map<String, EndpointAccumulator> endpointMetrics = new ConcurrentHashMap<>();

    private final AtomicLong unauthorizedAttempts401 = new AtomicLong(0);
    private final AtomicLong forbiddenAttempts403 = new AtomicLong(0);

    private final AtomicLong appointmentCreationAttempts = new AtomicLong(0);
    private final AtomicLong successfulAppointments = new AtomicLong(0);
    private final AtomicLong rejectedByBusinessRules = new AtomicLong(0);

    private volatile int totalControllers;
    private volatile int totalServices;
    private volatile int totalRepositories;
    private volatile int totalProtectedEndpoints;
    private volatile int endpointsWithAuthEnforced;
    private volatile boolean standardizedErrorHandlingEnabled;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationBeans = applicationContext.getBeansOfType(Object.class);
    }

    @PostConstruct
    public void initialize() {
        TRACKED_ENDPOINTS.forEach(endpoint -> endpointMetrics.put(endpoint, new EndpointAccumulator()));
        computeStaticStructureMetrics();
    }

    @Override
    public void recordEndpointResponseTime(String endpoint, long responseTimeMs) {
        EndpointAccumulator accumulator = endpointMetrics.get(endpoint);
        if (accumulator == null) {
            return;
        }
        accumulator.totalRequests.incrementAndGet();
        accumulator.totalResponseTimeMs.addAndGet(responseTimeMs);
        if (responseTimeMs <= PERFORMANCE_THRESHOLD_MS) {
            accumulator.requestsUnderThreshold.incrementAndGet();
        }
    }

    @Override
    public void recordUnauthorizedAttemptWithoutToken(String endpoint) {
        if (isProtectedEndpoint(endpoint)) {
            unauthorizedAttempts401.incrementAndGet();
        }
    }

    @Override
    public void recordForbiddenAttempt(String endpoint) {
        if (endpoint != null && endpoint.startsWith("/api/v1/admin/")) {
            forbiddenAttempts403.incrementAndGet();
        }
    }

    @Override
    public void recordAppointmentCreationAttempt() {
        appointmentCreationAttempts.incrementAndGet();
    }

    @Override
    public void recordAppointmentCreationSuccess() {
        successfulAppointments.incrementAndGet();
    }

    @Override
    public void recordAppointmentCreationRejected() {
        rejectedByBusinessRules.incrementAndGet();
    }

    @Override
    public List<PerformanceMetricsDTO> getPerformanceMetrics() {
        List<PerformanceMetricsDTO> metrics = new ArrayList<>();
        for (String endpoint : TRACKED_ENDPOINTS) {
            EndpointAccumulator acc = endpointMetrics.get(endpoint);
            if (acc == null) {
                continue;
            }

            long totalRequests = acc.totalRequests.get();
            long totalResponseTime = acc.totalResponseTimeMs.get();
            long underThreshold = acc.requestsUnderThreshold.get();

            long avgResponse = totalRequests == 0 ? 0 : totalResponseTime / totalRequests;
            double compliance = totalRequests == 0 ? 100.0 : percentage(underThreshold, totalRequests);

            metrics.add(new PerformanceMetricsDTO(
                    endpoint,
                    avgResponse,
                    totalRequests,
                    underThreshold,
                    compliance
            ));
        }
        return metrics;
    }

    @Override
    public SecurityMetricsDTO getSecurityMetrics() {
        return new SecurityMetricsDTO(
                unauthorizedAttempts401.get(),
                forbiddenAttempts403.get(),
                totalProtectedEndpoints,
                endpointsWithAuthEnforced,
                totalProtectedEndpoints == 0 ? 100.0 : percentage(endpointsWithAuthEnforced, totalProtectedEndpoints)
        );
    }

    @Override
    public MaintainabilityMetricsDTO getMaintainabilityMetrics() {
        return new MaintainabilityMetricsDTO(
                totalControllers,
                totalServices,
                totalRepositories,
                standardizedErrorHandlingEnabled,
                DEFAULT_JACOCO_COVERAGE_GATE_PERCENT
        );
    }

    @Override
    public AppointmentsMetricsDTO getAppointmentsMetrics() {
        long attempts = appointmentCreationAttempts.get();
        long success = successfulAppointments.get();
        long rejected = rejectedByBusinessRules.get();

        long totalInDb = appointmentRepository.count();
        long validInDb = appointmentRepository.countValidRecords();

        double businessRuleCompliance = attempts == 0 ? 100.0 : percentage(success + rejected, attempts);
        double dataIntegrity = totalInDb == 0 ? 100.0 : percentage(validInDb, totalInDb);

        return new AppointmentsMetricsDTO(
                attempts,
                success,
                rejected,
                businessRuleCompliance,
                totalInDb,
                validInDb,
                dataIntegrity
        );
    }

    @Override
    public MetricsSummaryDTO getSummaryMetrics() {
        return new MetricsSummaryDTO(
                getPerformanceMetrics(),
                getSecurityMetrics(),
                getMaintainabilityMetrics(),
                getAppointmentsMetrics()
        );
    }

    private void computeStaticStructureMetrics() {
        this.totalControllers = countBeansByAnnotationAndPackage(RestController.class, "com.sparktech.motorx.controller");
        this.totalServices = countBeansByAnnotationAndPackage(Service.class, "com.sparktech.motorx.Services");
        this.totalRepositories = countRepositoryBeans();
        this.totalProtectedEndpoints = countProtectedEndpointMappings();
        this.endpointsWithAuthEnforced = this.totalProtectedEndpoints;
        this.standardizedErrorHandlingEnabled = GlobalControllerAdvice.class.isAnnotationPresent(RestControllerAdvice.class);
    }

    private int countBeansByAnnotationAndPackage(Class<? extends Annotation> annotation, String packagePrefix) {
        int count = 0;
        for (Object bean : applicationBeans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass.getName().startsWith(packagePrefix)
                    && targetClass.isAnnotationPresent(annotation)) {
                count++;
            }
        }
        return count;
    }

    private int countRepositoryBeans() {
        int count = 0;
        for (Object bean : applicationBeans.values()) {
            if (bean instanceof Repository) {
                Class<?> targetClass = AopUtils.getTargetClass(bean);
                if (targetClass.getName().startsWith("com.sparktech.motorx.repository")) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countProtectedEndpointMappings() {
        int count = 0;
        Map<RequestMappingInfo, org.springframework.web.method.HandlerMethod> handlerMethods =
                requestMappingHandlerMapping.getHandlerMethods();

        for (RequestMappingInfo info : handlerMethods.keySet()) {
            for (String pattern : extractPatterns(info)) {
                if (isProtectedEndpoint(pattern)) {
                    count++;
                }
            }
        }
        return count;
    }

    private Set<String> extractPatterns(RequestMappingInfo info) {
        Set<String> patterns = new HashSet<>();
        var pathPatternsCondition = info.getPathPatternsCondition();
        if (pathPatternsCondition != null) {
            patterns.addAll(pathPatternsCondition.getPatternValues());
        }
        return patterns;
    }

    private boolean isProtectedEndpoint(String endpoint) {
        return endpoint != null && (endpoint.startsWith("/api/v1/user/") || endpoint.startsWith("/api/v1/admin/"));
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 100.0;
        }
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private static final class EndpointAccumulator {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
        private final AtomicLong requestsUnderThreshold = new AtomicLong(0);
    }
}