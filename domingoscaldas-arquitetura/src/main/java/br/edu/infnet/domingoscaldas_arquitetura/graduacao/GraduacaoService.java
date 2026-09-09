package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.conquista.ConquistaService;

/**
 * Regra central do BJJ School (módulo graduação): frequência + conquistas em
 * campeonatos definem quando o aluno está apto a um novo grau ou faixa.
 * Colabora com os módulos aluno (AlunoService) e conquista (ConquistaService)
 * sempre pelos services — nunca pelos repositories deles.
 * Candidato a serviço independente na Etapa 2.
 */
@Service
public class GraduacaoService {

	private final PresencaRepository presencaRepository;
	private final GraduacaoRepository graduacaoRepository;
	private final AlunoService alunoService;
	private final ConquistaService conquistaService;

	public GraduacaoService(PresencaRepository presencaRepository, GraduacaoRepository graduacaoRepository,
			AlunoService alunoService, ConquistaService conquistaService) {
		this.presencaRepository = presencaRepository;
		this.graduacaoRepository = graduacaoRepository;
		this.alunoService = alunoService;
		this.conquistaService = conquistaService;
	}

	// ----- presenças -----

	public Presenca registrarPresenca(Long alunoId, Presenca presenca) {
		Aluno aluno = alunoService.obterPorId(alunoId);
		presenca.setAluno(aluno);

		return presencaRepository.save(presenca);
	}

	public List<Presenca> obterPresencas() {
		return presencaRepository.findAll();
	}

	public Presenca obterPresencaPorId(Long id) {
		return presencaRepository.findById(id).orElseThrow(() -> new PresencaNaoEncontradaException(id));
	}

	public List<Presenca> obterPresencasDoAluno(Long alunoId) {
		alunoService.obterPorId(alunoId);

		return presencaRepository.findByAlunoId(alunoId);
	}

	public List<Presenca> obterPresencasPorPeriodo(LocalDate inicio, LocalDate fim) {

		if (inicio == null || fim == null || fim.isBefore(inicio)) {
			throw new IllegalArgumentException("O período informado é inválido");
		}

		return presencaRepository.findByDataBetween(inicio, fim);
	}

	public Presenca alterarPresenca(Long id, Presenca presenca) {
		Presenca existente = obterPresencaPorId(id);
		existente.setData(presenca.getData());
		existente.setTipoTreino(presenca.getTipoTreino());

		return presencaRepository.save(existente);
	}

	public void excluirPresenca(Long id) {
		Presenca existente = obterPresencaPorId(id);

		presencaRepository.delete(existente);
	}

	// ----- graduações -----

	/**
	 * Registra a graduação no histórico e pede ao módulo aluno que atualize a
	 * faixa/graus atuais.
	 */
	public Graduacao registrarGraduacao(Long alunoId, Graduacao graduacao) {
		Aluno aluno = alunoService.obterPorId(alunoId);
		graduacao.setAluno(aluno);

		Graduacao registrada = graduacaoRepository.save(graduacao);
		alunoService.graduar(alunoId, graduacao.getFaixa(), graduacao.getGrau());

		return registrada;
	}

	public List<Graduacao> obterGraduacoes() {
		return graduacaoRepository.findAll();
	}

	public Graduacao obterGraduacaoPorId(Long id) {
		return graduacaoRepository.findById(id).orElseThrow(() -> new GraduacaoNaoEncontradaException(id));
	}

	public List<Graduacao> obterGraduacoesDoAluno(Long alunoId) {
		alunoService.obterPorId(alunoId);

		return graduacaoRepository.findByAlunoIdOrderByDataDesc(alunoId);
	}

	public Graduacao alterarGraduacao(Long id, Graduacao graduacao) {
		Graduacao existente = obterGraduacaoPorId(id);
		existente.setFaixa(graduacao.getFaixa());
		existente.setGrau(graduacao.getGrau());
		existente.setData(graduacao.getData());

		return graduacaoRepository.save(existente);
	}

	public void excluirGraduacao(Long id) {
		Graduacao existente = obterGraduacaoPorId(id);

		graduacaoRepository.delete(existente);
	}

	// ----- regra de pontos -----

	/**
	 * Pontos acumulados desde a última graduação: cada presença vale 1 ponto e
	 * cada medalha vale os pontos definidos em {@code Medalha}. Contam apenas
	 * presenças e conquistas posteriores à última graduação.
	 */
	public long calcularPontos(Long alunoId) {
		alunoService.obterPorId(alunoId);

		LocalDate ultimaGraduacao = graduacaoRepository.findTopByAlunoIdOrderByDataDesc(alunoId)
				.map(Graduacao::getData)
				.orElse(null);

		long pontosPresencas = ultimaGraduacao == null
				? presencaRepository.countByAlunoId(alunoId)
				: presencaRepository.countByAlunoIdAndDataAfter(alunoId, ultimaGraduacao);

		long pontosConquistas = conquistaService.obterPorAluno(alunoId).stream()
				.filter(conquista -> conquista.getCampeonato() != null
						&& (ultimaGraduacao == null || conquista.getCampeonato().getData().isAfter(ultimaGraduacao)))
				.mapToLong(conquista -> conquista.getMedalha().getPontosGraduacao())
				.sum();

		return pontosPresencas + pontosConquistas;
	}

	/** Alunos ativos cujos pontos atingem o mínimo exigido pela faixa atual. */
	public List<Aluno> obterAptos() {
		return alunoService.obterAtivos().stream()
				.filter(aluno -> calcularPontos(aluno.getId()) >= aluno.getFaixa().getPontosMinimosPorGrau())
				.toList();
	}
}
