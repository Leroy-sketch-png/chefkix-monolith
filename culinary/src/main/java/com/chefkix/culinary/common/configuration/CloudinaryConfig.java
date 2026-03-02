package com.chefkix.culinary.common.configuration;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary configKey() {
        Map<String, String> config = new HashMap();
        config.put("cloud_name", "dc0umlqvf");
        config.put("api_key", "762755256531976");
        config.put("api_secret", "VRyusVXC7KObwoLjg1zmRtUt514");
        return new Cloudinary(config);
    }
}
