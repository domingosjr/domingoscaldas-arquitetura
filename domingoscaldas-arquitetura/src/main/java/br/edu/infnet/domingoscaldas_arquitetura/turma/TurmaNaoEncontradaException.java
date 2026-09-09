package br.edu.infnet.domingoscaldas_arquitetura.turma;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

/**
 * Exceção de domínio do módulo turma.
 */
public class TurmaNaoEncontradaException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public TurmaNaoEncontradaException(Long id) {
		super("Turma não encontrada: " + id);
	}
}
