package com.sme.be_sme.shared;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CorsFilter runs before controllers to ensure CORS headers are set.
 * Needed when deployed behind proxy (DigitalOcean, etc.) where WebMvcConfigurer may not apply early enough.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://fe-sme.vercel.app,https://*.vercel.app}")
    private String allowedOrigins;

    private List<String> patterns;

    @Override
    public void init(FilterConfig filterConfig) {
        patterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (patterns.isEmpty()) {
            patterns = List.of("*");
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        String allowOrigin = resolveAllowOrigin(origin);
        if (allowOrigin == null) allowOrigin = "*";
        response.setHeader("Access-Control-Allow-Origin", allowOrigin);
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-Request-Id");
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        response.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    private String resolveAllowOrigin(String origin) {
        if (patterns.contains("*")) return "*";
        if (!StringUtils.hasText(origin)) return null;
        if (patterns.contains(origin)) return origin;
        // Pattern match: https://*.vercel.app
        for (String p : patterns) {
            if (matchPattern(p, origin)) return origin;
        }
        return null;
    }

    private static boolean matchPattern(String pattern, String origin) {
        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return origin.matches(regex);
        }
        return pattern.equals(origin);
    }
}
