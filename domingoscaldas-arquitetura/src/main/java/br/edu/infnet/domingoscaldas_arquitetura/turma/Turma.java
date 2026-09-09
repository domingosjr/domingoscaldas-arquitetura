package br.edu.infnet.domingoscaldas_arquitetura.turma;

import java.util.ArrayList;
import java.util.List;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Turma de treino da escola de Jiu-Jitsu (ex.: Kids, Adulto Iniciante,
 * Competição). Uma turma tem muitos alunos e um aluno pode treinar em muitas
 * turmas — relacionamento muitos-para-muitos unidirecional a partir da turma.
 */
@Entity
@Table(name = "turmas")
public class Turma {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "O nome é obrigatório")
	private String nome;

	@NotBlank(message = "O horário é obrigatório")
	private String horario;

	private boolean ativa;

	@ManyToMany(fetch = FetchType.EAGER)
	private List<Aluno> alunos = new ArrayList<>();

	public Turma() {
	}

	public Turma(String nome, String horario) {
		this.nome = nome;
		this.horario = horario;
	}

	/** Comportamento de domínio: a coleção não é exposta para escrita externa. */
	public void adicionarAluno(Aluno aluno) {
		this.alunos.add(aluno);
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

	public String getHorario() {
		return horario;
	}

	public void setHorario(String horario) {
		this.horario = horario;
	}

	public boolean isAtiva() {
		return ativa;
	}

	public void setAtiva(boolean ativa) {
		this.ativa = ativa;
	}

	public List<Aluno> getAlunos() {
		return alunos;
	}
}
