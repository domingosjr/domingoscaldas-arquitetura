package br.edu.infnet.domingoscaldas_arquitetura.aluno;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

/**
 * Exceção de domínio do módulo aluno.
 */
public class AlunoNaoEncontradoException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public AlunoNaoEncontradoException(Long id) {
		super("Aluno não encontrado: " + id);
	}
}
