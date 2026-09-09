package br.edu.infnet.domingoscaldas_arquitetura.turma;

import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.turma.dto.AlunoResumoResponse;
import br.edu.infnet.domingoscaldas_arquitetura.turma.dto.TurmaResponse;

/**
 * Regras de negócio do módulo turma. Para falar com o módulo aluno usa o
 * {@link AlunoService} (ponto de colaboração) — nunca o AlunoRepository, que é
 * detalhe interno de persistência daquele módulo.
 */
@Service
public class TurmaService {

	private final TurmaRepository turmaRepository;
	private final AlunoService alunoService;

	public TurmaService(TurmaRepository turmaRepository, AlunoService alunoService) {
		this.turmaRepository = turmaRepository;
		this.alunoService = alunoService;
	}

	/**
	 * Matricula um aluno em uma turma de treino: valida a existência dos dois
	 * lados e recusa matrícula duplicada.
	 */
	public Turma matricularAluno(Long turmaId, Long alunoId) {
		Turma turma = obterPorId(turmaId);
		Aluno aluno = alunoService.obterPorId(alunoId);

		boolean jaMatriculado = turma.getAlunos().stream()
				.anyMatch(alunoMatriculado -> alunoMatriculado.getId().equals(alunoId));

		if (jaMatriculado) {
			throw new AlunoJaMatriculadoException(turmaId, alunoId);
		}

		turma.adicionarAluno(aluno);

		return turmaRepository.save(turma);
	}

	public TurmaResponse obterDetalhes(Long id) {
		Turma turma = obterPorId(id);

		return converterParaResponse(turma);
	}

	public Turma incluir(Turma turma) {
		return turmaRepository.save(turma);
	}

	public List<Turma> obterLista() {
		return turmaRepository.findAll();
	}

	public Turma obterPorId(Long id) {
		return turmaRepository.findById(id).orElseThrow(() -> new TurmaNaoEncontradaException(id));
	}

	public Turma alterar(Long id, Turma turma) {
		Turma existente = obterPorId(id);
		existente.setNome(turma.getNome());
		existente.setHorario(turma.getHorario());
		existente.setAtiva(turma.isAtiva());

		return turmaRepository.save(existente);
	}

	public void excluir(Long id) {
		Turma existente = obterPorId(id);

		turmaRepository.delete(existente);
	}

	public List<Turma> obterAtivas() {
		return turmaRepository.findByAtivaTrue();
	}

	private TurmaResponse converterParaResponse(Turma turma) {
		List<AlunoResumoResponse> alunos = turma.getAlunos().stream()
				.map(aluno -> new AlunoResumoResponse(aluno.getId(), aluno.getNome(), aluno.getEmail(),
						aluno.getFaixa()))
				.toList();

		return new TurmaResponse(turma.getId(), turma.getNome(), turma.getHorario(), turma.isAtiva(), alunos);
	}
}
