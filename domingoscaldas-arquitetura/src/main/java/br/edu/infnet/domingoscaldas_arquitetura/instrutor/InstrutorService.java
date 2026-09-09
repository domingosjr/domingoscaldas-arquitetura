package br.edu.infnet.domingoscaldas_arquitetura.instrutor;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Regras de negócio do módulo instrutor.
 */
@Service
public class InstrutorService {

	private final InstrutorRepository instrutorRepository;

	public InstrutorService(InstrutorRepository instrutorRepository) {
		this.instrutorRepository = instrutorRepository;
	}

	public Instrutor incluir(Instrutor instrutor) {
		return instrutorRepository.save(instrutor);
	}

	public List<Instrutor> obterLista() {
		return instrutorRepository.findAll();
	}

	public Instrutor obterPorId(Long id) {
		return instrutorRepository.findById(id).orElseThrow(() -> new InstrutorNaoEncontradoException(id));
	}

	public Instrutor alterar(Long id, Instrutor instrutor) {
		Instrutor existente = obterPorId(id);
		existente.setNome(instrutor.getNome());
		existente.setEmail(instrutor.getEmail());
		existente.setTelefone(instrutor.getTelefone());
		existente.setFaixa(instrutor.getFaixa());
		existente.setGraus(instrutor.getGraus());
		existente.setRegistroFederacao(instrutor.getRegistroFederacao());
		existente.setAtivo(instrutor.isAtivo());

		return instrutorRepository.save(existente);
	}

	public void excluir(Long id) {
		Instrutor existente = obterPorId(id);

		instrutorRepository.delete(existente);
	}

	public List<Instrutor> obterAtivos() {
		return instrutorRepository.findByAtivoTrue();
	}
}
