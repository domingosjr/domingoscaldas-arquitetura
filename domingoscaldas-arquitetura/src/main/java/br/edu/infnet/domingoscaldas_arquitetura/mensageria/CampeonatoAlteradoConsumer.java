package br.edu.infnet.domingoscaldas_arquitetura.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.edu.infnet.domingoscaldas_arquitetura.conquista.ConquistaService;

/**
 * Consumidor: escuta a fila de campeonatos alterados e atualiza o nome e a data
 * do campeonato copiados nas conquistas (a cópia feita na Etapa 2). Se a
 * aplicação estiver fora do ar, as mensagens esperam na fila e são processadas
 * quando ela voltar.
 */
@Component
public class CampeonatoAlteradoConsumer {

	private static final Logger log = LoggerFactory.getLogger(CampeonatoAlteradoConsumer.class);

	private final ConquistaService conquistaService;

	public CampeonatoAlteradoConsumer(ConquistaService conquistaService) {
		this.conquistaService = conquistaService;
	}

	@RabbitListener(queues = MensageriaConfig.FILA_CAMPEONATOS_ALTERADOS)
	public void receber(CampeonatoAlteradoMessage mensagem) {
		int atualizadas = conquistaService.atualizarDadosDoCampeonato(mensagem.campeonatoId(), mensagem.nome(),
				mensagem.data());

		log.info("Mensagem recebida: {} -> {} conquista(s) atualizada(s)", mensagem, atualizadas);
	}
}
