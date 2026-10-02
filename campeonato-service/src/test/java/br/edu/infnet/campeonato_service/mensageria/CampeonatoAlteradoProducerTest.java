package br.edu.infnet.campeonato_service.mensageria;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * O produtor publica na fila certa e não derruba a operação quando o broker
 * está fora do ar.
 */
class CampeonatoAlteradoProducerTest {

	private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
	private final CampeonatoAlteradoProducer producer = new CampeonatoAlteradoProducer(rabbitTemplate);

	private final CampeonatoAlteradoMessage mensagem =
			new CampeonatoAlteradoMessage(1L, "Copa Rio 2026", LocalDate.of(2026, 5, 18));

	@Test
	void publicaNaFilaDeCampeonatosAlterados() {
		producer.enviar(mensagem);

		verify(rabbitTemplate).convertAndSend("campeonatos.alterados", (Object) mensagem);
	}

	@Test
	void brokerForaDoArNaoPropagaErro() {
		doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
				.when(rabbitTemplate).convertAndSend(eq("campeonatos.alterados"), any(Object.class));

		assertThatCode(() -> producer.enviar(mensagem)).doesNotThrowAnyException();
	}
}
