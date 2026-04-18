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

    // This bean is used when annotating with @Async("taskExecutor")
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-worker-");

        // MOST IMPORTANT: Attach Decorator here
        // It copies both SecurityContext AND RequestAttributes to the new thread
        executor.setTaskDecorator(new ContextCopyingDecorator());

        executor.initialize();
        return executor;
    }

    // Inner class Decorator (keeps the same logic)
    static class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 1. Capture context from PARENT thread (Main Thread)
            var securityContext = SecurityContextHolder.getContext();
            var requestAttributes = RequestContextHolder.getRequestAttributes();

            return () -> {
                try {
                    // 2. Inject context into CHILD thread (Async Thread)
                    SecurityContextHolder.setContext(securityContext);
                    RequestContextHolder.setRequestAttributes(requestAttributes);

                    // 3. Run the logic
                    runnable.run();
                } finally {
                    // 4. Cleanup
                    SecurityContextHolder.clearContext();
                    RequestContextHolder.resetRequestAttributes();
                }
            };
        }
    }
}