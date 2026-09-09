package br.edu.infnet.domingoscaldas_arquitetura.endereco;

import org.springframework.stereotype.Service;

import feign.FeignException;

/**
 * Consulta de endereço por CEP (módulo endereço). Não depende de nenhuma
 * entidade nem do banco da aplicação — segundo candidato a serviço independente.
 */
@Service
public class EnderecoService {

	private final ViaCepClient viaCepClient;

	public EnderecoService(ViaCepClient viaCepClient) {
		this.viaCepClient = viaCepClient;
	}

	public Endereco consultarPorCep(String cep) {

		if (cep == null || !cep.matches("\\d{8}")) {
			throw new IllegalArgumentException("O CEP deve possuir exatamente 8 dígitos numéricos");
		}

		Endereco endereco;

		try {
			endereco = viaCepClient.consultarCep(cep);
		} catch (FeignException e) {
			throw new IllegalArgumentException("Não foi possível consultar o CEP " + cep + " no ViaCEP");
		}

		if (endereco == null || endereco.isCepInexistente()) {
			throw new EnderecoNaoEncontradoException(cep);
		}

		return endereco;
	}
}
