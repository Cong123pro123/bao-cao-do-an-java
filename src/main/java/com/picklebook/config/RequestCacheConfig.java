package com.picklebook.config;

import com.picklebook.security.StaticResourceRequestCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.savedrequest.RequestCache;

@Configuration
public class RequestCacheConfig {

    @Bean
    public RequestCache requestCache() {
        return new StaticResourceRequestCache();
    }
}
