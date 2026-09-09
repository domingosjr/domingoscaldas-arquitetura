package br.edu.infnet.domingoscaldas_arquitetura.endereco;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

public class EnderecoNaoEncontradoException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public EnderecoNaoEncontradoException(String cep) {
		super("Nenhum endereço encontrado para o CEP: " + cep);
	}
}
