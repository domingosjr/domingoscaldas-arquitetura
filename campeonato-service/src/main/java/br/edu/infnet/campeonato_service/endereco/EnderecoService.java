package br.edu.infnet.campeonato_service.endereco;

import org.springframework.stereotype.Service;

import br.edu.infnet.campeonato_service.endereco.client.ViaCepGateway;
import br.edu.infnet.campeonato_service.endereco.exception.EnderecoNaoEncontradoException;

/**
 * Consulta de endereço por CEP. A comunicação com o ViaCEP fica no
 * ViaCepGateway; aqui ficam as regras: formato do CEP e CEP inexistente.
 */
@Service
public class EnderecoService {

	private final ViaCepGateway viaCepGateway;

	public EnderecoService(ViaCepGateway viaCepGateway) {
		this.viaCepGateway = viaCepGateway;
	}

	public Endereco consultarPorCep(String cep) {

		if (cep == null || !cep.matches("\\d{8}")) {
			throw new IllegalArgumentException("O CEP deve possuir exatamente 8 dígitos numéricos");
		}

		Endereco endereco = viaCepGateway.consultarCep(cep);

		if (endereco == null || endereco.isCepInexistente()) {
			throw new EnderecoNaoEncontradoException(cep);
		}

		return endereco;
	}
}
