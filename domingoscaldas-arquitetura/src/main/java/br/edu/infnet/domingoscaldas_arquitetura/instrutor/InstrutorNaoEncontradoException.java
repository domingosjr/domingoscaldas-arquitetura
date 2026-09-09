package br.edu.infnet.domingoscaldas_arquitetura.instrutor;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

public class InstrutorNaoEncontradoException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public InstrutorNaoEncontradoException(Long id) {
		super("Instrutor não encontrado: " + id);
	}
}
