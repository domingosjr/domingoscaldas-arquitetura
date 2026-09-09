package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

public class ConquistaNaoEncontradaException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public ConquistaNaoEncontradaException(Long id) {
		super("Conquista não encontrada: " + id);
	}
}
