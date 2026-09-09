package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.Campeonato;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.CampeonatoService;

/**
 * Regras de negócio do módulo conquista. Depende dos módulos aluno e
 * campeonato (pelos services) para validar a conquista registrada.
 */
@Service
public class ConquistaService {

	private final ConquistaRepository conquistaRepository;
	private final CampeonatoService campeonatoService;
	private final AlunoService alunoService;

	public ConquistaService(ConquistaRepository conquistaRepository, CampeonatoService campeonatoService,
			AlunoService alunoService) {
		this.conquistaRepository = conquistaRepository;
		this.campeonatoService = campeonatoService;
		this.alunoService = alunoService;
	}

	/** Registra a conquista de um aluno em um campeonato existente. */
	public Conquista registrar(Long alunoId, Long campeonatoId, Conquista conquista) {
		Aluno aluno = alunoService.obterPorId(alunoId);
		Campeonato campeonato = campeonatoService.obterPorId(campeonatoId);

		conquista.setAluno(aluno);
		conquista.setCampeonato(campeonato);

		return conquistaRepository.save(conquista);
	}

	public List<Conquista> obterLista() {
		return conquistaRepository.findAll();
	}

	public Conquista obterPorId(Long id) {
		return conquistaRepository.findById(id).orElseThrow(() -> new ConquistaNaoEncontradaException(id));
	}

	public Conquista alterar(Long id, Conquista conquista) {
		Conquista existente = obterPorId(id);
		existente.setCategoria(conquista.getCategoria());
		existente.setMedalha(conquista.getMedalha());

		return conquistaRepository.save(existente);
	}

	public void excluir(Long id) {
		Conquista existente = obterPorId(id);

		conquistaRepository.delete(existente);
	}

	/** Operação oferecida ao módulo graduação. */
	public List<Conquista> obterPorAluno(Long alunoId) {
		alunoService.obterPorId(alunoId);

		return conquistaRepository.findByAlunoId(alunoId);
	}

	public List<Conquista> obterPorMedalha(Medalha medalha) {
		return conquistaRepository.findByMedalha(medalha);
	}

	/** Quadro de medalhas da escola: total de conquistas por tipo de medalha. */
	public Map<Medalha, Long> obterQuadroDeMedalhas() {
		return conquistaRepository.findAll().stream()
				.collect(Collectors.groupingBy(Conquista::getMedalha, Collectors.counting()));
	}
}
