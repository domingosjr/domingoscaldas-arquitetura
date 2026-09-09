package br.edu.infnet.domingoscaldas_arquitetura.campeonato;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

public class CampeonatoNaoEncontradoException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public CampeonatoNaoEncontradoException(Long id) {
		super("Campeonato não encontrado: " + id);
	}
}
