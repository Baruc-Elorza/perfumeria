package com.example.demo.config;

import com.example.demo.repository.UsuarioRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Comprueba revocación en cada petición, incluso tras desbloquear una cuenta. */
public class UsuarioActivoFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    public UsuarioActivoFilter(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var usuario = usuarios.findByEmail(auth.getName()).orElse(null);
            var session = request.getSession(false);
            Object version = session == null ? null : session.getAttribute("versionSesion");
            if (usuario == null || !usuario.getActivo()
                    || !(version instanceof Long) || ((Long) version).longValue() != usuario.getVersionSesion()) {
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Sesión revocada o cuenta bloqueada. Inicia sesión nuevamente si tu cuenta está activa.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
