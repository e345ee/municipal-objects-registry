package org.itmo.config;

import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = "org.itmo")
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new com.fasterxml.jackson.databind.util.StdDateFormat().withColonInTimeZone(true));

        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                converters.set(i, new MappingJackson2HttpMessageConverter(mapper));
                return;
            }
        }
        converters.add(new MappingJackson2HttpMessageConverter(mapper));
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer c) {
        c.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
