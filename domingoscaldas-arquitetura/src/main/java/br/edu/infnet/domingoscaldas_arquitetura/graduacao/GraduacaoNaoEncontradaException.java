package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

public class GraduacaoNaoEncontradaException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public GraduacaoNaoEncontradaException(Long id) {
		super("Graduação não encontrada: " + id);
	}
}
