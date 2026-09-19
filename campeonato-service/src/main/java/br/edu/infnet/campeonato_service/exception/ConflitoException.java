package br.edu.infnet.campeonato_service.exception;

/**
 * Base das exceções de conflito com o estado atual do sistema (ex.: aluno já
 * matriculado). Cada módulo define a sua; o handler global traduz para 409.
 */
public abstract class ConflitoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	protected ConflitoException(String mensagem) {
		super(mensagem);
	}
}
