package br.edu.infnet.campeonato_service.campeonato;

import br.edu.infnet.campeonato_service.exception.RecursoNaoEncontradoException;

public class CampeonatoNaoEncontradoException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public CampeonatoNaoEncontradoException(Long id) {
		super("Campeonato não encontrado: " + id);
	}
}
