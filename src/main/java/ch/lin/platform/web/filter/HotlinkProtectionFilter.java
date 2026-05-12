/*=============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *===========================================================================*/
package ch.lin.platform.web.filter;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.lin.platform.api.ApiError;
import ch.lin.platform.api.ApiResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to prevent hotlinking of specific resources (e.g., images). It
 * validates the 'Referer' or 'Origin' headers against allowed CORS patterns.
 * <p>
 * This filter is intentionally NOT annotated with @Component to avoid global
 * registration by Spring Boot. It should be registered via a
 * FilterRegistrationBean.
 */
public class HotlinkProtectionFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(HotlinkProtectionFilter.class);

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * A list of origin patterns that are permitted to access the resources.
     */
    private final List<String> allowedOrigins;

    /**
     * If true, requests without a Referer or Origin header will be blocked.
     */
    private final boolean strictMode;

    /**
     * Constructs a new {@code HotlinkProtectionFilter} with the specified
     * settings.
     *
     * @param allowedOrigins A list of origin patterns that are permitted to
     * access the resources. Supports wildcards.
     * @param strictMode If true, requests without a Referer or Origin header
     * will be blocked.
     */
    public HotlinkProtectionFilter(List<String> allowedOrigins, boolean strictMode) {
        this.allowedOrigins = allowedOrigins;
        this.strictMode = strictMode;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method inspects the 'Origin' or 'Referer' header of the incoming
     * request. If the source matches the configured allowed origins, the
     * request is allowed to proceed down the filter chain. Otherwise, it is
     * blocked with an HTTP 403 Forbidden status. The behavior for requests
     * without a source header is controlled by the 'strictMode' setting.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String referer = req.getHeader("Referer");
        String origin = req.getHeader("Origin");

        // Prefer Origin, fallback to Referer
        String source = StringUtils.hasText(origin) ? origin : referer;

        if (!StringUtils.hasText(source)) {
            if (strictMode) {
                String clientIp = getClientIp(req);
                String userAgent = req.getHeader("User-Agent");
                logger.warn("AUDIT [Hotlink Blocked] - Reason: Missing Referer/Origin (Strict Mode). URI: [{}], IP: [{}], User-Agent: [{}]",
                        req.getRequestURI(), clientIp, userAgent);
                sendForbiddenResponse(res, "Hotlinking is not allowed");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        for (String allowed : allowedOrigins) {
            if (allowed.equals("*") || PatternMatchUtils.simpleMatch(allowed + "*", source)) {
                chain.doFilter(request, response);
                return;
            }
        }

        String clientIp = getClientIp(req);
        String userAgent = req.getHeader("User-Agent");
        logger.warn("AUDIT [Hotlink Blocked] - Reason: Disallowed Source. URI: [{}], Source: [{}], IP: [{}], User-Agent: [{}]",
                req.getRequestURI(), source, clientIp, userAgent);
        sendForbiddenResponse(res, "Hotlinking is not allowed");
    }

    /**
     * Helper method to write a standardized JSON error response.
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json; charset=UTF-8");

        ApiError apiError = new ApiError("FORBIDDEN", message);
        ApiResponse<ApiError> apiResponse = ApiResponse.failure(apiError);

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    /**
     * Helper method to extract the real client IP, considering reverse proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // The first IP in the list is the original client IP
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
