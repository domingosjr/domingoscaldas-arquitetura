package br.edu.infnet.domingoscaldas_arquitetura.aluno;

/**
 * Faixas do Jiu-Jitsu (adulto), na ordem de progressão.
 * Cada faixa define a quantidade mínima de pontos (presenças + medalhas)
 * para receber um novo grau.
 */
public enum Faixa {

	BRANCA(40),
	AZUL(60),
	ROXA(80),
	MARROM(100),
	PRETA(150);

	private final int pontosMinimosPorGrau;

	Faixa(int pontosMinimosPorGrau) {
		this.pontosMinimosPorGrau = pontosMinimosPorGrau;
	}

	public int getPontosMinimosPorGrau() {
		return pontosMinimosPorGrau;
	}

	public Faixa proxima() {
		int posicao = ordinal();

		return posicao < values().length - 1 ? values()[posicao + 1] : this;
	}
}
