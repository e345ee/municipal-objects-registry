package org.itmo.config;

import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = "org.itmo")
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mapper.setDateFormat(new com.fasterxml.jackson.databind.util.StdDateFormat().withColonInTimeZone(true));

        converters.add(0, new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper));
    }
}
