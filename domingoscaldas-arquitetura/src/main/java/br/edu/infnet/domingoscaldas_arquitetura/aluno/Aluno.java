package br.edu.infnet.domingoscaldas_arquitetura.aluno;

import java.time.LocalDate;

import br.edu.infnet.domingoscaldas_arquitetura.comum.Pessoa;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Aluno da escola de Jiu-Jitsu (módulo aluno). Mantém a faixa e os graus
 * atuais; presenças, conquistas e graduações pertencem aos módulos
 * graduação e campeonato, que referenciam o aluno.
 */
@Entity
@Table(name = "alunos")
public class Aluno extends Pessoa {

	@NotNull(message = "A data de nascimento é obrigatória")
	@Past(message = "A data de nascimento deve estar no passado")
	private LocalDate dataNascimento;

	private LocalDate dataMatricula;

	@PositiveOrZero(message = "O peso não pode ser negativo")
	private double peso;

	private boolean ativo;

	@NotNull(message = "A faixa é obrigatória")
	@Enumerated(EnumType.STRING)
	private Faixa faixa;

	@PositiveOrZero(message = "Os graus não podem ser negativos")
	private int graus;

	public Aluno() {
	}

	public Aluno(String nome, String email, String telefone, LocalDate dataNascimento, LocalDate dataMatricula,
			double peso, boolean ativo, Faixa faixa, int graus) {
		super(nome, email, telefone);
		this.dataNascimento = dataNascimento;
		this.dataMatricula = dataMatricula;
		this.peso = peso;
		this.ativo = ativo;
		this.faixa = faixa;
		this.graus = graus;
	}

	/** Comportamento de domínio: aplica uma nova graduação (faixa/grau) ao aluno. */
	public void graduar(Faixa novaFaixa, int novosGraus) {
		this.faixa = novaFaixa;
		this.graus = novosGraus;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public LocalDate getDataMatricula() {
		return dataMatricula;
	}

	public void setDataMatricula(LocalDate dataMatricula) {
		this.dataMatricula = dataMatricula;
	}

	public double getPeso() {
		return peso;
	}

	public void setPeso(double peso) {
		this.peso = peso;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
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
}
