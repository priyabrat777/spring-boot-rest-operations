package com.enterprise.api.audit;

import com.enterprise.api.entity.AuditLog;
import com.enterprise.api.entity.AuditableEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Entity Listener that automatically creates audit log entries
 * for all operations on AuditableEntity instances.
 * 
 * This listener captures:
 * - Entity state changes (old vs new values)
 * - Operation type (CREATE, UPDATE, DELETE)
 * - User context and request information
 */
@Component
public class AuditEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventListener.class);
    
    private static AuditService auditService;
    private static ObjectMapper objectMapper;
    private static AuditAware auditAware;

    @Autowired
    public void setAuditService(AuditService auditService) {
        AuditEventListener.auditService = auditService;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        AuditEventListener.objectMapper = objectMapper;
    }

    @Autowired
    public void setAuditAware(AuditAware auditAware) {
        AuditEventListener.auditAware = auditAware;
    }

    /**
     * Called after an entity is persisted (created).
     */
    @PostPersist
    public void onPostPersist(Object entity) {
        if (entity instanceof AuditableEntity auditableEntity) {
            try {
                createAuditLog(auditableEntity, AuditLog.AuditOperation.CREATE, null, entityToMap(auditableEntity));
            } catch (Exception e) {
                logger.error("Failed to create audit log for entity creation: {}", entity.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Called after an entity is updated.
     */
    @PostUpdate
    public void onPostUpdate(Object entity) {
        if (entity instanceof AuditableEntity auditableEntity) {
            try {
                // Note: In a real implementation, you would need to capture the old values
                // This could be done using @PreUpdate and storing in ThreadLocal
                createAuditLog(auditableEntity, AuditLog.AuditOperation.UPDATE, null, entityToMap(auditableEntity));
            } catch (Exception e) {
                logger.error("Failed to create audit log for entity update: {}", entity.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Called after an entity is removed (deleted).
     */
    @PostRemove
    public void onPostRemove(Object entity) {
        if (entity instanceof AuditableEntity auditableEntity) {
            try {
                createAuditLog(auditableEntity, AuditLog.AuditOperation.DELETE, entityToMap(auditableEntity), null);
            } catch (Exception e) {
                logger.error("Failed to create audit log for entity deletion: {}", entity.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Creates an audit log entry for the given entity and operation.
     */
    private void createAuditLog(AuditableEntity entity, AuditLog.AuditOperation operation, 
                               Map<String, Object> oldValues, Map<String, Object> newValues) {
        
        if (auditService == null || auditAware == null) {
            logger.warn("Audit service or audit aware not initialized, skipping audit log creation");
            return;
        }

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityName(entity.getClass().getSimpleName());
            auditLog.setEntityId(getEntityId(entity));
            auditLog.setOperation(operation);
            auditLog.setPerformedBy(auditAware.getCurrentAuditorName());
            
            if (oldValues != null) {
                auditLog.setOldValues(mapToJson(oldValues));
            }
            
            if (newValues != null) {
                auditLog.setNewValues(mapToJson(newValues));
            }

            // Capture request context if available
            captureRequestContext(auditLog);

            auditService.saveAuditLog(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to create audit log for entity: {}", entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Extracts the entity ID using reflection.
     */
    private String getEntityId(Object entity) {
        try {
            Field[] fields = entity.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    return value != null ? value.toString() : "null";
                }
            }
            
            // Check superclass fields
            Class<?> superClass = entity.getClass().getSuperclass();
            while (superClass != null && !superClass.equals(Object.class)) {
                Field[] superFields = superClass.getDeclaredFields();
                for (Field field : superFields) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        Object value = field.get(entity);
                        return value != null ? value.toString() : "null";
                    }
                }
                superClass = superClass.getSuperclass();
            }
            
        } catch (Exception e) {
            logger.warn("Failed to extract entity ID for audit log", e);
        }
        return "unknown";
    }

    /**
     * Converts entity to a map of field names and values.
     */
    private Map<String, Object> entityToMap(Object entity) {
        Map<String, Object> map = new HashMap<>();
        
        try {
            Field[] fields = entity.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                map.put(field.getName(), value);
            }
            
            // Include superclass fields (like audit fields)
            Class<?> superClass = entity.getClass().getSuperclass();
            while (superClass != null && !superClass.equals(Object.class)) {
                Field[] superFields = superClass.getDeclaredFields();
                for (Field field : superFields) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    map.put(field.getName(), value);
                }
                superClass = superClass.getSuperclass();
            }
            
        } catch (Exception e) {
            logger.warn("Failed to convert entity to map for audit log", e);
        }
        
        return map;
    }

    /**
     * Converts a map to JSON string.
     */
    private String mapToJson(Map<String, Object> map) {
        if (objectMapper == null) {
            return map.toString();
        }
        
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to convert map to JSON for audit log", e);
            return map.toString();
        }
    }

    /**
     * Captures HTTP request context information for the audit log.
     */
    private void captureRequestContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
            }
        } catch (Exception e) {
            logger.debug("Failed to capture request context for audit log", e);
        }
    }

    /**
     * Extracts the client IP address from the HTTP request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}