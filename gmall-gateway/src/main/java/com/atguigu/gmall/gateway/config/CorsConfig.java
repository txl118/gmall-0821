package com.atguigu.gmall.gateway.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //允许跨域的域名可以写*,代表所有域名,但是不能携带cookie
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");
        //允许跨域请求的方法
        corsConfiguration.addAllowedMethod("*");
        //允许携带的头信息
        corsConfiguration.addAllowedHeader("*");
        //允许携带cookie
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(configurationSource);

    }


}
