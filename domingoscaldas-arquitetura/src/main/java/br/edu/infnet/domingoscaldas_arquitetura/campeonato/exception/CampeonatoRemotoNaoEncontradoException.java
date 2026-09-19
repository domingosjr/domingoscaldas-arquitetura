package br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception;

import br.edu.infnet.domingoscaldas_arquitetura.exception.RecursoNaoEncontradoException;

/**
 * O campeonato-service respondeu 404: o campeonato informado não existe lá.
 */
public class CampeonatoRemotoNaoEncontradoException extends RecursoNaoEncontradoException {

	private static final long serialVersionUID = 1L;

	public CampeonatoRemotoNaoEncontradoException(Long id) {
		super("O campeonato de ID " + id + " não foi encontrado no campeonato-service.");
	}
}
