package com.payment.service.util;

import com.payment.service.config.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimsExtractor {

    public AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto da requisição atual.");
        }

        if (!(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException(
                    "Principal inesperado no SecurityContext: " + auth.getPrincipal().getClass().getName());
        }

        return user;
    }

    public Long currentUserId() {
        return currentUser().userId();
    }

    public String currentUserEmail() {
        return currentUser().email();
    }
}
