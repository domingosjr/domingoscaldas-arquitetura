package br.edu.infnet.domingoscaldas_arquitetura.turma;

import br.edu.infnet.domingoscaldas_arquitetura.exception.ConflitoException;

/**
 * Exceção de domínio do módulo turma: a matrícula conflita com o estado atual
 * da turma (o aluno já está nela).
 */
public class AlunoJaMatriculadoException extends ConflitoException {

	private static final long serialVersionUID = 1L;

	public AlunoJaMatriculadoException(Long turmaId, Long alunoId) {
		super("O aluno " + alunoId + " já está matriculado na turma " + turmaId);
	}
}
