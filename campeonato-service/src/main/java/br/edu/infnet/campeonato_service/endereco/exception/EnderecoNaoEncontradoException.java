package br.edu.infnet.campeonato_service.endereco.exception;

import br.edu.infnet.campeonato_service.exception.RecursoNaoEncontradoException;

public class EnderecoNaoEncontradoException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public EnderecoNaoEncontradoException(String cep) {
		super("Nenhum endereço encontrado para o CEP: " + cep);
	}
}
