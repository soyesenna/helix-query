package com.soyesenna.helixquery.autoconfigure;

import com.soyesenna.helixquery.HelixQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HelixQueryAutoConfigurationTest {

    @Test
    void registersHelixQueryFactoryWhenMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HelixQueryAutoConfiguration.class))
                .withBean(EntityManager.class, () -> mock(EntityManager.class))
                .run(context -> assertThat(context).hasSingleBean(HelixQueryFactory.class));
    }

    @Test
    void backsOffWhenUserDefinesHelixQueryFactory() {
        HelixQueryFactory userFactory = new HelixQueryFactory(mock(EntityManager.class));

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HelixQueryAutoConfiguration.class))
                .withBean(EntityManager.class, () -> mock(EntityManager.class))
                .withBean(HelixQueryFactory.class, () -> userFactory)
                .run(context -> assertThat(context.getBean(HelixQueryFactory.class)).isSameAs(userFactory));
    }
}

