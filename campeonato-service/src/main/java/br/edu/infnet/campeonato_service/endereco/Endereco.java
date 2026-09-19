package br.edu.infnet.campeonato_service.endereco;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Endereço devolvido pela consulta de CEP (resposta do ViaCEP).
 * O campo "erro" só vem preenchido quando o CEP não existe.
 */
public class Endereco {

	private String cep;
	private String logradouro;
	private String bairro;
	private String localidade;
	private String uf;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Boolean erro;

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getLogradouro() {
		return logradouro;
	}

	public void setLogradouro(String logradouro) {
		this.logradouro = logradouro;
	}

	public String getBairro() {
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getLocalidade() {
		return localidade;
	}

	public void setLocalidade(String localidade) {
		this.localidade = localidade;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public Boolean getErro() {
		return erro;
	}

	public void setErro(Boolean erro) {
		this.erro = erro;
	}

	@JsonIgnore
	public boolean isCepInexistente() {
		return Boolean.TRUE.equals(erro);
	}
}
