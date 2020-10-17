package com.example.appanddb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.DenyAll;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;

@SpringBootApplication
@RestController
@EnableRetry
public class AppAndDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppAndDbApplication.class, args);
	}

	@Bean
	BeanPostProcessor dataSourceWrapper() {
		return new RetryableDataSourceBeanPostProcessor();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> applicationListener(
		CustomerRepository customerRepository) {
		return event -> {
			customerRepository.save(new Customer(null, "Bean"));
			customerRepository.save(new Customer(null, "Kai"));
			customerRepository.findAll().forEach(System.out::println);
		};
	}
}


@Order(Ordered.HIGHEST_PRECEDENCE)
class RetryableDataSourceBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
		throws BeansException {
		if (bean instanceof DataSource) {
			bean = new RetryableDataSource((DataSource) bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
		throws BeansException {
		return bean;
	}
}

interface CustomerRepository extends JpaRepository<Customer, Integer> {
}


@Log4j2
class RetryableDataSource extends AbstractDataSource {

	private final DataSource delegate;

	public RetryableDataSource(DataSource delegate) {
		this.delegate =	 delegate;
	}

	@Override
	@Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2.3, maxDelay = 30000))
	public Connection getConnection() throws SQLException {
		log.info("trying getConnection()");
		return delegate.getConnection();
	}

	@Override
	@Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2.3, maxDelay = 30000))
	public Connection getConnection(String username, String password)
		throws SQLException {
		log.info("trying getConnection(username, password)");
		return delegate.getConnection(username, password);
	}

}


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
class Customer {

	@Id
	@GeneratedValue
	private Integer id;

	private String name;
}