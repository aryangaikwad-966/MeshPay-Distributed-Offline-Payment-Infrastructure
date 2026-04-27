package com.demo.upimesh.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Audit Logging Filter
 * Logs all API requests for security auditing and compliance
 * 
 * Logs:
 * - Request timestamp
 * - HTTP method and URI
 * - Authenticated user
 * - Client IP address
 * - Response status
 * - Request/response time
 */
@Slf4j
@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final org.slf4j.Logger auditLog = org.slf4j.LoggerFactory.getLogger("AUDIT");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp,
                                   FilterChain chain) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        
        // Generate trace ID for request tracking
        String traceId = req.getHeader(TRACE_ID_HEADER);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        req.setAttribute(TRACE_ID_HEADER, traceId);
        resp.setHeader(TRACE_ID_HEADER, traceId);
        
        String method = req.getMethod();
        String uri = req.getRequestURI();
        String queryString = req.getQueryString();
        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        String clientIp = getClientIp(req);
        String user = getAuthenticatedUser();
        
        try {
            chain.doFilter(req, resp);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = resp.getStatus();
            
            // Log security-relevant requests at INFO level
            if (uri.contains("/api/bridge") || uri.contains("/api/admin") || status >= 400) {
                auditLog.info("AUDIT | method={} uri={} user={} ip={} status={} duration={}ms trace_id={}",
                             method, fullUri, user, clientIp, status, duration, traceId);
            } else {
                // Log other requests at DEBUG level
                auditLog.debug("AUDIT | method={} uri={} user={} ip={} status={} duration={}ms trace_id={}",
                            method, fullUri, user, clientIp, status, duration, traceId);
            }
        }
    }

    /**
     * Extract authenticated user from security context
     */
    private String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "ANONYMOUS";
    }

    /**
     * Extract client IP address (handles load balancers and proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        // Skip health checks and public endpoints
        return uri.contains("/actuator/health") || 
               uri.contains("/swagger-ui") ||
               uri.contains("/v3/api-docs");
    }
}
