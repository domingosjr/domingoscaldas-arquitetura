package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import java.time.LocalDate;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import jakarta.persistence.Column;
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
 *
 * Desde a Etapa 2 o campeonato pertence ao campeonato-service: aqui fica só
 * o identificador externo ({@code campeonatoId}, sem FK) e uma cópia do nome e
 * da data do evento, feita no momento do registro. A data é usada pela regra
 * de pontos da graduação, que assim não depende do serviço estar no ar.
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

	@Column(name = "campeonato_id")
	private Long campeonatoId;

	@Column(name = "campeonato_nome", length = 120)
	private String campeonatoNome;

	@Column(name = "campeonato_data")
	private LocalDate campeonatoData;

	@ManyToOne
	@JoinColumn(name = "aluno_id")
	private Aluno aluno;

	public Conquista() {
	}

	public Conquista(String categoria, Medalha medalha) {
		this.categoria = categoria;
		this.medalha = medalha;
	}

	/** Associa a conquista a um campeonato do campeonato-service. */
	public void definirCampeonato(Long campeonatoId, String campeonatoNome, LocalDate campeonatoData) {
		this.campeonatoId = campeonatoId;
		this.campeonatoNome = campeonatoNome;
		this.campeonatoData = campeonatoData;
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

	public Long getCampeonatoId() {
		return campeonatoId;
	}

	public String getCampeonatoNome() {
		return campeonatoNome;
	}

	public LocalDate getCampeonatoData() {
		return campeonatoData;
	}

	public Aluno getAluno() {
		return aluno;
	}

	public void setAluno(Aluno aluno) {
		this.aluno = aluno;
	}
}
