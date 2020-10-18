package com.example.appanddb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.ThreadWaitSleeper;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.Collections;

@Log4j2
@EnableRetry
@RestController
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication
public class AppAndDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppAndDbApplication.class, args);
	}

	@Bean
	RetryTemplate retryTemplate() {
		var retryTemplate = new RetryTemplate();

		var retryPolicy = new SimpleRetryPolicy(10, Collections.singletonMap(Exception.class, true), true);
		retryTemplate.setRetryPolicy(retryPolicy);

		var backOffPolicy = new ExponentialRandomBackOffPolicy();
		backOffPolicy.setInitialInterval(1000 * 10);
		backOffPolicy.setMultiplier(2.7);
		backOffPolicy.setSleeper( new ThreadWaitSleeper());
		backOffPolicy.setMaxInterval(1000 * 60 * 3);

		retryTemplate.setBackOffPolicy(backOffPolicy);

		return retryTemplate;
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> listener(JdbcTemplate template) {
		return event -> template
			.query("select * from customer", (resultSet, i) -> new Customer(resultSet.getInt("id"), resultSet.getString("name")))
			.forEach(System.out::println);
	}


	@Log4j2
	@Component
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@RequiredArgsConstructor
	public static class RetryableDataSourceBeanPostProcessor implements BeanPostProcessor {

		private final RetryTemplate retryTemplate;

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof DataSource) {
				return buildRetryableDataSource(retryTemplate, beanName, (DataSource) bean);
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
			return bean;
		}
	}

	static DataSource buildRetryableDataSource(
		RetryTemplate template, String beanName, DataSource dataSource) {
		log.info("wrapping beanName " + beanName + " for class " + dataSource.getClass().getCanonicalName() + '.');
		var pfb = new ProxyFactoryBean();
		pfb.setTarget(dataSource);
		pfb.setAutodetectInterfaces(true);
		pfb.setTargetClass(dataSource.getClass());
		pfb.addAdvice((MethodInterceptor) mi ->
			template
				.execute((RetryCallback<Object, Throwable>) retryContext -> {
					var method = mi.getMethod();
					log.info("invoke the method [" + mi.getMethod().getName() +
						"] retryCount [" + retryContext.getRetryCount() + ']');
					return method.invoke(dataSource, mi.getArguments());
				}));
		return (DataSource) pfb.getObject();
	}

}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
	private Integer id;
	private String name;
}



	/*@Log4j2
	@Deprecated
	public static class RetryableDataSource extends AbstractDataSource {

		private final DataSource delegate;

		private final String beanName;

		private RetryableDataSource(String beanName, DataSource delegate) {
			this.delegate = delegate;
			this.beanName = beanName;
			log.info("wrapping beanName '" + this.beanName + "'");
		}

		@Override
		@Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2.3, maxDelay = 60_000))
		public Connection getConnection() throws SQLException {
			log.info("trying getConnection(): ");
			return delegate.getConnection();
		}

		@Override
		@Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2.3, maxDelay = 60_000))
		public Connection getConnection(String username, String password)
			throws SQLException {
			log.info("trying getConnection(" + username + ", " + password + ")");
			return delegate.getConnection(username, password);
		}

	}*/
