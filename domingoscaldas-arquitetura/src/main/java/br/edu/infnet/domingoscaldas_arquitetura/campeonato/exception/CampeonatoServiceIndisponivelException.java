package br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception;

/**
 * O campeonato-service está fora do ar ou não respondeu a tempo.
 */
public class CampeonatoServiceIndisponivelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public static final String MENSAGEM = "O serviço de campeonatos está temporariamente indisponível. "
			+ "Tente novamente em instantes.";

	public CampeonatoServiceIndisponivelException() {
		super(MENSAGEM);
	}
}
