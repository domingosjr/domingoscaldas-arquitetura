package br.edu.infnet.campeonato_service.campeonato;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Campeonato de Jiu-Jitsu em que os alunos competem. A validação dos dados de
 * entrada fica no DTO {@code CampeonatoRequest}; a entidade só mapeia a tabela.
 */
@Entity
@Table(name = "campeonatos")
public class Campeonato {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(length = 80)
	private String cidade;

	@Column(length = 8)
	private String cep;

	@Column(nullable = false)
	private LocalDate data;

	public Campeonato() {
	}

	public Campeonato(String nome, String cidade, String cep, LocalDate data) {
		this.nome = nome;
		this.cidade = cidade;
		this.cep = cep;
		this.data = data;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}
}
