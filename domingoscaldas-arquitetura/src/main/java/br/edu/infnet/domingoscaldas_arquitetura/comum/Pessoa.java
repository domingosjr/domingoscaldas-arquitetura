package br.edu.infnet.domingoscaldas_arquitetura.comum;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados comuns às pessoas da escola (aluno, instrutor). Superclasse mapeada:
 * cada módulo mantém a sua própria tabela, sem tabela compartilhada.
 */
@MappedSuperclass
public abstract class Pessoa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "O nome é obrigatório")
	@Size(max = 100, message = "O nome deve possuir no máximo 100 caracteres")
	private String nome;

	@NotBlank(message = "O e-mail é obrigatório")
	@Email(message = "O e-mail deve ser válido")
	private String email;

	@Size(max = 20, message = "O telefone deve possuir no máximo 20 caracteres")
	private String telefone;

	protected Pessoa() {
	}

	protected Pessoa(String nome, String email, String telefone) {
		this.nome = nome;
		this.email = email;
		this.telefone = telefone;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}
}
