package br.edu.infnet.domingoscaldas_arquitetura.aluno;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Regras de negócio do módulo aluno. É o ponto de colaboração que os demais
 * módulos usam para falar com este módulo (nunca o AlunoRepository direto).
 */
@Service
public class AlunoService {

	private final AlunoRepository alunoRepository;

	public AlunoService(AlunoRepository alunoRepository) {
		this.alunoRepository = alunoRepository;
	}

	public Aluno incluir(Aluno aluno) {
		return alunoRepository.save(aluno);
	}

	public List<Aluno> obterLista() {
		return alunoRepository.findAll();
	}

	public Aluno obterPorId(Long id) {
		return alunoRepository.findById(id).orElseThrow(() -> new AlunoNaoEncontradoException(id));
	}

	public Aluno alterar(Long id, Aluno aluno) {
		Aluno existente = obterPorId(id);
		existente.setNome(aluno.getNome());
		existente.setEmail(aluno.getEmail());
		existente.setTelefone(aluno.getTelefone());
		existente.setDataNascimento(aluno.getDataNascimento());
		existente.setDataMatricula(aluno.getDataMatricula());
		existente.setPeso(aluno.getPeso());
		existente.setAtivo(aluno.isAtivo());
		existente.setFaixa(aluno.getFaixa());
		existente.setGraus(aluno.getGraus());

		return alunoRepository.save(existente);
	}

	public void excluir(Long id) {
		Aluno existente = obterPorId(id);

		alunoRepository.delete(existente);
	}

	/**
	 * Operação oferecida ao módulo graduação: atualiza faixa/graus do aluno
	 * quando uma graduação é registrada.
	 */
	public Aluno graduar(Long id, Faixa novaFaixa, int novosGraus) {
		Aluno existente = obterPorId(id);
		existente.graduar(novaFaixa, novosGraus);

		return alunoRepository.save(existente);
	}

	public List<Aluno> obterAtivos() {
		return alunoRepository.findByAtivoTrue();
	}

	public List<Aluno> obterPorNome(String nome) {
		return alunoRepository.findByNomeContainingIgnoreCase(nome);
	}

	public List<Aluno> obterPorFaixa(Faixa faixa) {
		return alunoRepository.findByFaixa(faixa);
	}

	public List<Aluno> obterOrdenadosPorNome() {
		return alunoRepository.findAllByOrderByNomeAsc();
	}
}
