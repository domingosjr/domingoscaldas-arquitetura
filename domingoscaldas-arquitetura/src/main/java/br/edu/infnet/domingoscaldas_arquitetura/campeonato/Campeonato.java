package br.edu.infnet.domingoscaldas_arquitetura.campeonato;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Campeonato de Jiu-Jitsu em que os alunos competem (módulo campeonato).
 */
@Entity
@Table(name = "campeonatos")
public class Campeonato {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "O nome é obrigatório")
	@Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres")
	private String nome;

	@Size(max = 80, message = "A cidade deve possuir no máximo 80 caracteres")
	private String cidade;

	@Pattern(regexp = "\\d{8}", message = "O CEP deve possuir exatamente 8 dígitos numéricos")
	private String cep;

	@NotNull(message = "A data é obrigatória")
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
