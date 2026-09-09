package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import java.time.LocalDate;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.Faixa;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Registro histórico de uma graduação do aluno: nova faixa ou novo grau na
 * faixa atual (grau 0 representa a troca de faixa).
 */
@Entity
@Table(name = "graduacoes")
public class Graduacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "A faixa é obrigatória")
	@Enumerated(EnumType.STRING)
	private Faixa faixa;

	@PositiveOrZero(message = "O grau não pode ser negativo")
	private int grau;

	@NotNull(message = "A data da graduação é obrigatória")
	private LocalDate data;

	@ManyToOne
	@JoinColumn(name = "aluno_id")
	private Aluno aluno;

	public Graduacao() {
	}

	public Graduacao(Faixa faixa, int grau, LocalDate data) {
		this.faixa = faixa;
		this.grau = grau;
		this.data = data;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Faixa getFaixa() {
		return faixa;
	}

	public void setFaixa(Faixa faixa) {
		this.faixa = faixa;
	}

	public int getGrau() {
		return grau;
	}

	public void setGrau(int grau) {
		this.grau = grau;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public Aluno getAluno() {
		return aluno;
	}

	public void setAluno(Aluno aluno) {
		this.aluno = aluno;
	}
}
