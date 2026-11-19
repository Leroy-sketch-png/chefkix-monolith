package com.chefkix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.chefkix.culinary.common.scheduled.PostDeadlineScheduler;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.service.ProfileProviderImpl;
import com.chefkix.identity.service.ProfileService;
import com.chefkix.social.api.PostProvider;
import com.chefkix.social.policy.BlockedContentPolicy;
import com.chefkix.social.post.service.PostService;
import com.chefkix.social.provider.PostProviderImpl;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

class PostProfileProviderCycleContextTest {

    @Test
    void actualProviderAndPostServiceGraphStartsWithLazyPostProviderEdge() {
        Map<String, Class<?>> actualBeans = new LinkedHashMap<>();
        actualBeans.put("postService", PostService.class);
        actualBeans.put("blockedContentPolicy", BlockedContentPolicy.class);
        actualBeans.put("profileProvider", ProfileProviderImpl.class);
        actualBeans.put("profileService", ProfileService.class);
        actualBeans.put("postProvider", PostProviderImpl.class);
        actualBeans.put("postDeadlineScheduler", PostDeadlineScheduler.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
            actualBeans.forEach((name, type) ->
                    beanFactory.registerBeanDefinition(name, new RootBeanDefinition(type)));
            registerUnrelatedCollaboratorMocks(beanFactory, actualBeans.values());

            context.refresh();

            assertThat(context.getBean(PostService.class)).isNotNull();
            assertThat(context.getBean(PostProvider.class)).isSameAs(context.getBean(PostProviderImpl.class));
            assertThat(context.getBean(ProfileProvider.class)).isSameAs(context.getBean(ProfileProviderImpl.class));
            assertThat(context.getBean(PostDeadlineScheduler.class)).isNotNull();
            Object lazyPostProvider = ReflectionTestUtils.getField(
                    context.getBean(ProfileService.class), "postProvider");
            assertThat(AopUtils.isAopProxy(lazyPostProvider)).isTrue();
        }
    }

    private static void registerUnrelatedCollaboratorMocks(
            DefaultListableBeanFactory beanFactory,
            Iterable<Class<?>> actualBeanTypes) {
        Set<Class<?>> actualTypes = new LinkedHashSet<>();
        actualBeanTypes.forEach(actualTypes::add);
        Map<Class<?>, Object> mocks = new LinkedHashMap<>();

        for (Class<?> actualType : actualTypes) {
            var constructors = actualType.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new IllegalStateException("Expected one injection constructor for " + actualType.getName());
            }
            Parameter[] parameters = constructors[0].getParameters();
            for (Parameter parameter : parameters) {
                Class<?> dependencyType = parameter.getType();
                if (actualTypes.stream().anyMatch(dependencyType::isAssignableFrom)
                        || mocks.values().stream().anyMatch(dependencyType::isInstance)) {
                    continue;
                }

                Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
                String beanName = qualifier == null
                        ? "testMock" + dependencyType.getSimpleName()
                        : qualifier.value();
                Object collaborator = mock(dependencyType);
                mocks.put(dependencyType, collaborator);
                beanFactory.registerSingleton(beanName, collaborator);
            }
        }
    }
}
