package com.develokit.maeum_ieum.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .components(new Components())
                .info(apiInfo());
    }
    public Info apiInfo(){
        return new Info()
                .title("Maeum-Ieum API Document")
                .description("기능 요청을 위한 API 명세")
                .version("1.0");
    }


}
