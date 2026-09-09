package br.edu.infnet.domingoscaldas_arquitetura.instrutor;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Faixa;
import br.edu.infnet.domingoscaldas_arquitetura.comum.Pessoa;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Instrutor responsável pelos treinos e pelas graduações (módulo instrutor).
 */
@Entity
@Table(name = "instrutores")
public class Instrutor extends Pessoa {

	@NotNull(message = "A faixa é obrigatória")
	@Enumerated(EnumType.STRING)
	private Faixa faixa;

	@PositiveOrZero(message = "Os graus não podem ser negativos")
	private int graus;

	private String registroFederacao;

	private boolean ativo;

	public Instrutor() {
	}

	public Instrutor(String nome, String email, String telefone, Faixa faixa, int graus, String registroFederacao,
			boolean ativo) {
		super(nome, email, telefone);
		this.faixa = faixa;
		this.graus = graus;
		this.registroFederacao = registroFederacao;
		this.ativo = ativo;
	}

	public Faixa getFaixa() {
		return faixa;
	}

	public void setFaixa(Faixa faixa) {
		this.faixa = faixa;
	}

	public int getGraus() {
		return graus;
	}

	public void setGraus(int graus) {
		this.graus = graus;
	}

	public String getRegistroFederacao() {
		return registroFederacao;
	}

	public void setRegistroFederacao(String registroFederacao) {
		this.registroFederacao = registroFederacao;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
}
