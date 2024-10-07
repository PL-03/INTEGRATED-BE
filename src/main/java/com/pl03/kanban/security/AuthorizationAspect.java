package com.pl03.kanban.security;

import com.pl03.kanban.controllers.BaseController;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuthorizationAspect {

    @Autowired
    private BaseController baseController;

    @Around("@annotation(requiresAuth)")
    public Object authorizeRequest(ProceedingJoinPoint joinPoint, RequiresAuth requiresAuth) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String authHeader = null;
        String boardId = null;

        for (int i = 0; i < signature.getParameterNames().length; i++) {
            if (signature.getParameterNames()[i].equals("authHeader")) {
                authHeader = (String) joinPoint.getArgs()[i];
            } else if (signature.getParameterNames()[i].equals("boardId")) {
                boardId = (String) joinPoint.getArgs()[i];
            }
        }

        if (authHeader == null || boardId == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }

        String userId = baseController.validateTokenAndGetUserId(authHeader);
        baseController.validateBoardOwnership(boardId, userId, requiresAuth.allowPublicAccess());

        return joinPoint.proceed();
    }
}