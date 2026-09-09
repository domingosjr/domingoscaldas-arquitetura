package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

public class PresencaNaoEncontradaException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public PresencaNaoEncontradaException(Long id) {
		super("Presença não encontrada: " + id);
	}
}
