package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import java.time.LocalDate;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import jakarta.persistence.Entity;
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
 * Presença de um aluno em um treino (módulo graduação). A frequência é o
 * principal critério para a graduação de graus e faixas.
 */
@Entity
@Table(name = "presencas")
public class Presenca {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "A data da presença é obrigatória")
	private LocalDate data;

	@NotBlank(message = "O tipo de treino é obrigatório")
	@Size(max = 30, message = "O tipo de treino deve possuir no máximo 30 caracteres")
	private String tipoTreino;

	@ManyToOne
	@JoinColumn(name = "aluno_id")
	private Aluno aluno;

	public Presenca() {
	}

	public Presenca(LocalDate data, String tipoTreino) {
		this.data = data;
		this.tipoTreino = tipoTreino;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public String getTipoTreino() {
		return tipoTreino;
	}

	public void setTipoTreino(String tipoTreino) {
		this.tipoTreino = tipoTreino;
	}

	public Aluno getAluno() {
		return aluno;
	}

	public void setAluno(Aluno aluno) {
		this.aluno = aluno;
	}
}
