/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.amqp;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitTemplate}.
 * <p>
 * This configuration class is active only when the RabbitMQ and Spring AMQP client
 * libraries are on the classpath.
 * <P>
 * Registers the following beans:
 * <ul>
 * <li>{@link org.springframework.amqp.rabbit.core.RabbitTemplate RabbitTemplate} if there
 * is no other bean of the same type in the context.</li>
 * <li>{@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory
 * CachingConnectionFactory} instance if there is no other bean of the same type in the
 * context.</li>
 * <li>{@link org.springframework.amqp.core.AmqpAdmin } instance as long as
 * {@literal spring.rabbitmq.dynamic=true}.</li>
 * </ul>
 * <p>
 * The {@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory} honors
 * the following properties:
 * <ul>
 * <li>{@literal spring.rabbitmq.port} is used to specify the port to which the client
 * should connect, and defaults to 5672.</li>
 * <li>{@literal spring.rabbitmq.username} is used to specify the (optional) username.
 * </li>
 * <li>{@literal spring.rabbitmq.password} is used to specify the (optional) password.
 * </li>
 * <li>{@literal spring.rabbitmq.host} is used to specify the host, and defaults to
 * {@literal localhost}.</li>
 * <li>{@literal spring.rabbitmq.virtualHost} is used to specify the (optional) virtual
 * host to which the client should connect.</li>
 * </ul>
 * @author Greg Turnquist
 * @author Josh Long
 * @author Stephane Nicoll
 * @author Gary Russell
 */
@Configuration
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
@EnableConfigurationProperties(RabbitProperties.class)
@Import(RabbitAnnotationDrivenConfiguration.class)
public class RabbitAutoConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
	@ConditionalOnMissingBean(AmqpAdmin.class)
	public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private ObjectProvider<MessageConverter> messageConverter;

	@Bean
	@ConditionalOnMissingBean(RabbitTemplate.class)
	public RabbitTemplate rabbitTemplate() {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(this.connectionFactory);
		MessageConverter messageConverter = this.messageConverter.getIfUnique();
		if (messageConverter != null) {
			rabbitTemplate.setMessageConverter(messageConverter);
		}
		return rabbitTemplate;
	}

	@Configuration
	@ConditionalOnMissingBean(ConnectionFactory.class)
	protected static class RabbitConnectionFactoryCreator {

		@Bean
		public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties config)
				throws Exception {
			RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
			if (config.getHost() != null) {
				factory.setHost(config.getHost());
				factory.setPort(config.getPort());
			}
			if (config.getUsername() != null) {
				factory.setUsername(config.getUsername());
			}
			if (config.getPassword() != null) {
				factory.setPassword(config.getPassword());
			}
			if (config.getVirtualHost() != null) {
				factory.setVirtualHost(config.getVirtualHost());
			}
			if (config.getRequestedHeartbeat() != null) {
				factory.setRequestedHeartbeat(config.getRequestedHeartbeat());
			}
			RabbitProperties.Ssl ssl = config.getSsl();
			if (ssl.isEnabled()) {
				factory.setUseSSL(true);
				factory.setKeyStore(ssl.getKeyStore());
				factory.setKeyStorePassphrase(ssl.getKeyStorePassword());
				factory.setTrustStore(ssl.getTrustStore());
				factory.setTrustStorePassphrase(ssl.getTrustStorePassword());
			}
			factory.afterPropertiesSet();
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
					factory.getObject());
			connectionFactory.setAddresses(config.getAddresses());
			if (config.getCache().getChannel().getSize() != null) {
				connectionFactory
						.setChannelCacheSize(config.getCache().getChannel().getSize());
			}
			if (config.getCache().getConnection().getMode() != null) {
				connectionFactory
						.setCacheMode(config.getCache().getConnection().getMode());
			}
			if (config.getCache().getConnection().getSize() != null) {
				connectionFactory.setConnectionCacheSize(
						config.getCache().getConnection().getSize());
			}
			if (config.getCache().getChannel().getCheckoutTimeout() != null) {
				connectionFactory.setChannelCheckoutTimeout(
						config.getCache().getChannel().getCheckoutTimeout());
			}
			return connectionFactory;
		}

	}

	@ConditionalOnClass(RabbitMessagingTemplate.class)
	@ConditionalOnMissingBean(RabbitMessagingTemplate.class)
	protected static class MessagingTemplateConfiguration {

		@Bean
		public RabbitMessagingTemplate rabbitMessagingTemplate(
				RabbitTemplate rabbitTemplate) {
			return new RabbitMessagingTemplate(rabbitTemplate);
		}

	}

}
