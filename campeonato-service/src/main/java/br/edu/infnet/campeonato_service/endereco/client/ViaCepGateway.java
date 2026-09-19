package br.edu.infnet.campeonato_service.endereco.client;

import org.springframework.stereotype.Component;

import br.edu.infnet.campeonato_service.endereco.Endereco;
import br.edu.infnet.campeonato_service.endereco.exception.ViaCepIndisponivelException;
import feign.RetryableException;

/**
 * Único lugar que trata as falhas de comunicação com o ViaCEP: API fora do ar
 * ou lenta (timeout) vira uma exceção do domínio, traduzida para 503.
 */
@Component
public class ViaCepGateway {

	private final ViaCepClient viaCepClient;

	public ViaCepGateway(ViaCepClient viaCepClient) {
		this.viaCepClient = viaCepClient;
	}

	public Endereco consultarCep(String cep) {
		try {
			return viaCepClient.consultarCep(cep);
		} catch (RetryableException e) {
			throw new ViaCepIndisponivelException();
		}
	}
}
