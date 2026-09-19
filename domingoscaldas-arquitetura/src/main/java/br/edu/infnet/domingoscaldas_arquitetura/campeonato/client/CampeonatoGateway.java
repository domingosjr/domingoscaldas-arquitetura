package br.edu.infnet.domingoscaldas_arquitetura.campeonato.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception.CampeonatoRemotoNaoEncontradoException;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception.CampeonatoServiceIndisponivelException;
import feign.FeignException;
import feign.RetryableException;

/**
 * Único lugar da aplicação que conhece o OpenFeign do campeonato-service.
 * Traduz as falhas da comunicação em exceções do domínio:
 * 404 remoto → campeonato não encontrado; serviço fora do ar ou lento
 * (conexão recusada, timeout) → serviço indisponível. A causa técnica fica só
 * no log, nunca na resposta ao cliente.
 */
@Component
public class CampeonatoGateway {

	private static final Logger log = LoggerFactory.getLogger(CampeonatoGateway.class);

	private final CampeonatoClient campeonatoClient;

	public CampeonatoGateway(CampeonatoClient campeonatoClient) {
		this.campeonatoClient = campeonatoClient;
	}

	public CampeonatoResponse obterPorId(Long id) {
		try {
			return campeonatoClient.obterPorId(id);
		} catch (FeignException.NotFound e) {
			throw new CampeonatoRemotoNaoEncontradoException(id);
		} catch (RetryableException e) {
			log.warn("campeonato-service indisponível: {}", e.getMessage());
			throw new CampeonatoServiceIndisponivelException();
		}
	}
}
