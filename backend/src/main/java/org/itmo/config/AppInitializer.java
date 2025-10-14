package org.itmo.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;


public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{JpaConfig.class};   // корневая конфигурация (БД, JPA)
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{WebConfig.class};   // настройка MVC и контроллеров
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};              // все URL идут через DispatcherServlet
    }
}