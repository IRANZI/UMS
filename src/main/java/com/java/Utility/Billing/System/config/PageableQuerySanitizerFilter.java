package com.java.Utility.Billing.System.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Swagger UI sometimes sends sort as a JSON array (e.g. ["fullNames,desc"]).
 * Spring Data expects flat query params (sort=fullNames,desc). This filter normalizes that.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PageableQuerySanitizerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String sort = request.getParameter("sort");
        if (sort != null && (sort.startsWith("[") || sort.contains("\""))) {
            String sanitized = sort.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .trim();
            filterChain.doFilter(new SortParameterRequestWrapper(request, sanitized), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static class SortParameterRequestWrapper extends HttpServletRequestWrapper {

        private final String sanitizedSort;

        SortParameterRequestWrapper(HttpServletRequest request, String sanitizedSort) {
            super(request);
            this.sanitizedSort = sanitizedSort;
        }

        @Override
        public String getParameter(String name) {
            return "sort".equals(name) ? sanitizedSort : super.getParameter(name);
        }

        @Override
        public String[] getParameterValues(String name) {
            return "sort".equals(name) ? new String[]{sanitizedSort} : super.getParameterValues(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = new HashMap<>(super.getParameterMap());
            map.put("sort", new String[]{sanitizedSort});
            return Collections.unmodifiableMap(map);
        }
    }
}
