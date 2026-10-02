package br.edu.infnet.domingoscaldas_arquitetura.mensageria;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da mensageria (RabbitMQ).
 */
@Configuration
public class MensageriaConfig {

	/** Fila de onde o consumidor lê os campeonatos alterados. */
	public static final String FILA_CAMPEONATOS_ALTERADOS = "campeonatos.alterados";

	/** Fila durável (as mensagens sobrevivem a um reinício do broker). */
	@Bean
	Queue campeonatosAlteradosQueue() {
		return new Queue(FILA_CAMPEONATOS_ALTERADOS, true);
	}

	/** Converte o JSON da fila no record do parâmetro do consumidor. */
	@Bean
	MessageConverter mensagemJsonConverter() {
		return new JacksonJsonMessageConverter();
	}
}
