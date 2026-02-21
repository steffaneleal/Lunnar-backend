package com.steffaneleal.lunnar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapeia a URL /uploads/** para a pasta 'uploads' na raiz do projeto.
        // O 'file:' é crucial para indicar que é um caminho no sistema de arquivos.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
