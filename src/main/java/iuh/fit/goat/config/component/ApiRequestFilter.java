package iuh.fit.goat.config.component;

import iuh.fit.goat.common.SoftDeleteFilter;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApiRequestFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException
    {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        log.info("➡️ API called: {} {}{}", method, uri, query != null ? "?" + query : "");

        Session session = this.entityManager.unwrap(Session.class);
        this.enableAllFilters(session);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            this.disableAllFilters(session);

            long duration = System.currentTimeMillis() - start;
            log.info("⬅️ API finished: {} {} ({} ms)", method, uri, duration);
        }
    }

    private void enableAllFilters(Session session) {
        if (SecurityUtil.hasRole("SUPER_ADMIN")) {
            return;
        }

        for (SoftDeleteFilter filter : SoftDeleteFilter.values()) {
            session.enableFilter(filter.getValue());
        }
    }

    private void disableAllFilters(Session session) {
        for (SoftDeleteFilter filter : SoftDeleteFilter.values()) {
            try {
                session.disableFilter(filter.getValue());
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}