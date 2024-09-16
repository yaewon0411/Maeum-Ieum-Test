package com.develokit.maeum_ieum.config.openAI.header;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignHeaderConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Logger log = LoggerFactory.getLogger(FeignHeaderConfig.class);

    @Value("${openai.key}")
    private String OPENAI_API_KEY;

    @Bean
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                requestTemplate.header(AUTHORIZATION_HEADER, "Bearer "+OPENAI_API_KEY);
                requestTemplate.header("OpenAI-Beta", "assistants=v2");
g            }
        };
    }


}
