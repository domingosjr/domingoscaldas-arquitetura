package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.Campeonato;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Conquista (título) de um aluno em um campeonato: medalha e categoria.
 */
@Entity
@Table(name = "conquistas")
public class Conquista {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "A categoria é obrigatória")
	@Size(max = 60, message = "A categoria deve possuir no máximo 60 caracteres")
	private String categoria;

	@NotNull(message = "A medalha é obrigatória")
	@Enumerated(EnumType.STRING)
	private Medalha medalha;

	@ManyToOne
	@JoinColumn(name = "campeonato_id")
	private Campeonato campeonato;

	@ManyToOne
	@JoinColumn(name = "aluno_id")
	private Aluno aluno;

	public Conquista() {
	}

	public Conquista(String categoria, Medalha medalha) {
		this.categoria = categoria;
		this.medalha = medalha;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public Medalha getMedalha() {
		return medalha;
	}

	public void setMedalha(Medalha medalha) {
		this.medalha = medalha;
	}

	public Campeonato getCampeonato() {
		return campeonato;
	}

	public void setCampeonato(Campeonato campeonato) {
		this.campeonato = campeonato;
	}

	public Aluno getAluno() {
		return aluno;
	}

	public void setAluno(Aluno aluno) {
		this.aluno = aluno;
	}
}
