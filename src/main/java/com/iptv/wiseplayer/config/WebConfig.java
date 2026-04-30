package com.iptv.wiseplayer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/support-tickets}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String location = "file:" + Paths.get(path).toAbsolutePath().toString() + "/";

        registry.addResourceHandler("/uploads/support-tickets/**")
                .addResourceLocations(location);
    }
}
