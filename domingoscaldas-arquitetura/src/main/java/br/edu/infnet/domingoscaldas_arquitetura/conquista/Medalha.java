package br.edu.infnet.domingoscaldas_arquitetura.conquista;

/**
 * Medalhas possíveis em uma conquista. Cada medalha vale pontos para a
 * graduação do aluno (regra consumida pelo módulo graduação).
 */
public enum Medalha {

	OURO(10),
	PRATA(5),
	BRONZE(3);

	private final int pontosGraduacao;

	Medalha(int pontosGraduacao) {
		this.pontosGraduacao = pontosGraduacao;
	}

	public int getPontosGraduacao() {
		return pontosGraduacao;
	}
}
