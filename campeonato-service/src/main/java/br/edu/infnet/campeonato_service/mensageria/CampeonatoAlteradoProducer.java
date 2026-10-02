package br.edu.infnet.campeonato_service.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Produtor: publica na fila que um campeonato foi alterado. Não espera nenhum
 * consumidor — a alteração termina aqui, e quem guarda cópia dos dados do
 * campeonato se atualiza quando puder.
 */
@Component
public class CampeonatoAlteradoProducer {

	private static final Logger log = LoggerFactory.getLogger(CampeonatoAlteradoProducer.class);

	private final RabbitTemplate rabbitTemplate;

	public CampeonatoAlteradoProducer(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	/**
	 * Se o broker estiver fora do ar, a alteração do campeonato continua valendo
	 * e a falha fica registrada no log (a mensagem não é publicada).
	 */
	public void enviar(CampeonatoAlteradoMessage mensagem) {
		try {
			rabbitTemplate.convertAndSend(MensageriaConfig.FILA_CAMPEONATOS_ALTERADOS, mensagem);
			log.info("Mensagem enviada para a fila {}: {}", MensageriaConfig.FILA_CAMPEONATOS_ALTERADOS, mensagem);
		} catch (AmqpException e) {
			log.warn("Mensagem NAO publicada (broker indisponivel): {} - {}", mensagem, e.getMessage());
		}
	}
}
