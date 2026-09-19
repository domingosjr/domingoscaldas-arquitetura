package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.client.CampeonatoGateway;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.client.CampeonatoResponse;
import br.edu.infnet.domingoscaldas_arquitetura.conquista.dto.ConquistaDetalheResponse;

/**
 * Regras de negócio do módulo conquista. Depende do módulo aluno (pelo
 * service) e do campeonato-service (pelo CampeonatoGateway, via OpenFeign)
 * para validar a conquista registrada.
 */
@Service
public class ConquistaService {

	private final ConquistaRepository conquistaRepository;
	private final CampeonatoGateway campeonatoGateway;
	private final AlunoService alunoService;

	public ConquistaService(ConquistaRepository conquistaRepository, CampeonatoGateway campeonatoGateway,
			AlunoService alunoService) {
		this.conquistaRepository = conquistaRepository;
		this.campeonatoGateway = campeonatoGateway;
		this.alunoService = alunoService;
	}

	/**
	 * Registra a conquista de um aluno em um campeonato. O aluno é validado
	 * aqui; o campeonato é consultado no campeonato-service, e o nome e a data
	 * do evento são copiados para a conquista.
	 */
	public Conquista registrar(Long alunoId, Long campeonatoId, Conquista conquista) {
		Aluno aluno = alunoService.obterPorId(alunoId);
		CampeonatoResponse campeonato = campeonatoGateway.obterPorId(campeonatoId);

		conquista.setAluno(aluno);
		conquista.definirCampeonato(campeonato.id(), campeonato.nome(), campeonato.data());

		return conquistaRepository.save(conquista);
	}

	/**
	 * Inclui uma conquista cujo campeonato já está definido (carga de
	 * demonstração). Não chama o campeonato-service, para a aplicação subir
	 * mesmo com o serviço desligado.
	 */
	public Conquista incluir(Long alunoId, Conquista conquista) {
		conquista.setAluno(alunoService.obterPorId(alunoId));

		return conquistaRepository.save(conquista);
	}

	public List<Conquista> obterLista() {
		return conquistaRepository.findAll();
	}

	public Conquista obterPorId(Long id) {
		return conquistaRepository.findById(id).orElseThrow(() -> new ConquistaNaoEncontradaException(id));
	}

	/** Conquista com os dados atuais do campeonato, consultados no campeonato-service. */
	public ConquistaDetalheResponse obterDetalhes(Long id) {
		Conquista conquista = obterPorId(id);
		CampeonatoResponse campeonato = campeonatoGateway.obterPorId(conquista.getCampeonatoId());

		return converterParaResponse(conquista, campeonato);
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

	private ConquistaDetalheResponse converterParaResponse(Conquista conquista, CampeonatoResponse campeonato) {
		return new ConquistaDetalheResponse(conquista.getId(), conquista.getCategoria(), conquista.getMedalha(),
				conquista.getAluno().getId(), conquista.getAluno().getNome(), campeonato.id(), campeonato.nome(),
				campeonato.cidade(), campeonato.data());
	}
}
