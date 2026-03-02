package com.chefkix.culinary.common.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // Bean này được gọi khi dùng @Async("taskExecutor")
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-worker-");

        // QUAN TRỌNG NHẤT: Gắn Decorator vào đây
        // Nó sẽ copy cả SecurityContext VÀ RequestAttributes sang luồng mới
        executor.setTaskDecorator(new ContextCopyingDecorator());

        executor.initialize();
        return executor;
    }

    // Inner class Decorator (Bạn viết đúng rồi, giữ nguyên logic này)
    static class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 1. Lấy context ở luồng CHA (Main Thread)
            var securityContext = SecurityContextHolder.getContext();
            var requestAttributes = RequestContextHolder.getRequestAttributes();

            return () -> {
                try {
                    // 2. Bơm context vào luồng CON (Async Thread)
                    SecurityContextHolder.setContext(securityContext);
                    RequestContextHolder.setRequestAttributes(requestAttributes);

                    // 3. Chạy logic
                    runnable.run();
                } finally {
                    // 4. Dọn dẹp
                    SecurityContextHolder.clearContext();
                    RequestContextHolder.resetRequestAttributes();
                }
            };
        }
    }
}