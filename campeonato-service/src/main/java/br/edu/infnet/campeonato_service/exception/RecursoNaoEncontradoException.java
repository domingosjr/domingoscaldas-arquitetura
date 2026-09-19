package br.edu.infnet.campeonato_service.exception;

/**
 * Base das exceções "não encontrado" de cada módulo (AlunoNaoEncontrado,
 * TurmaNaoEncontrada...). Cada módulo define a sua; o handler global traduz
 * todas para 404 num único ponto.
 */
public abstract class RecursoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	protected RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}
}
