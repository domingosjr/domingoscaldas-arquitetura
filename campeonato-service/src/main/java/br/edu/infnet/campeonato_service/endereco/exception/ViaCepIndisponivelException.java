package br.edu.infnet.campeonato_service.endereco.exception;

/**
 * A API ViaCEP está fora do ar ou não respondeu a tempo.
 */
public class ViaCepIndisponivelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ViaCepIndisponivelException() {
		super("A consulta de CEP está temporariamente indisponível. Tente novamente em instantes.");
	}
}
