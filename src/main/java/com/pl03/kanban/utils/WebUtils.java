package com.pl03.kanban.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
@Getter
@Component
public class WebUtils {
//    public static String getBaseUrl() {
//        ServletRequestAttributes attributes =
//                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//
//        if (attributes == null) {
//            throw new IllegalStateException("No request context available");
//        }
//
//        HttpServletRequest request = attributes.getRequest();
//        return request.getScheme() + "://" + request.getServerName() +
//                (request.getServerPort() == 80 || request.getServerPort() == 443 || request.getServerPort() == 8080 //for dev
//                        ? "" : ":" + request.getServerPort());
//    }

    private final String baseUrl;

    @Autowired
    public WebUtils(@Value("${app.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
