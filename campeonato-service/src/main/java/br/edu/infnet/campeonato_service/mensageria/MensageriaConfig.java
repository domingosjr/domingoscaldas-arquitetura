package br.edu.infnet.campeonato_service.mensageria;

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

	/** Fila onde os campeonatos alterados ficam esperando o consumidor. */
	public static final String FILA_CAMPEONATOS_ALTERADOS = "campeonatos.alterados";

	/**
	 * Fila durável: declarada também pelo produtor, para existir mesmo que o
	 * consumidor nunca tenha subido — senão a mensagem publicada se perderia.
	 */
	@Bean
	Queue campeonatosAlteradosQueue() {
		return new Queue(FILA_CAMPEONATOS_ALTERADOS, true);
	}

	/** A mensagem vai para a fila como JSON (e não como objeto Java serializado). */
	@Bean
	MessageConverter mensagemJsonConverter() {
		return new JacksonJsonMessageConverter();
	}
}
