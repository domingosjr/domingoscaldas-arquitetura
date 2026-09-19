package br.edu.infnet.domingoscaldas_arquitetura;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.Faixa;
import br.edu.infnet.domingoscaldas_arquitetura.conquista.Conquista;
import br.edu.infnet.domingoscaldas_arquitetura.conquista.ConquistaService;
import br.edu.infnet.domingoscaldas_arquitetura.conquista.Medalha;
import br.edu.infnet.domingoscaldas_arquitetura.graduacao.Graduacao;
import br.edu.infnet.domingoscaldas_arquitetura.graduacao.GraduacaoService;
import br.edu.infnet.domingoscaldas_arquitetura.graduacao.Presenca;
import br.edu.infnet.domingoscaldas_arquitetura.instrutor.Instrutor;
import br.edu.infnet.domingoscaldas_arquitetura.instrutor.InstrutorService;
import br.edu.infnet.domingoscaldas_arquitetura.turma.Turma;
import br.edu.infnet.domingoscaldas_arquitetura.turma.TurmaService;

/**
 * Carga de dados de demonstração no H2 (desative com app.runner.habilitado=false).
 * Usa apenas os services públicos de cada módulo — a aplicação também é
 * "cliente" das fronteiras dos módulos.
 *
 * Desde a Etapa 2 os campeonatos são semeados pelo campeonato-service. Aqui
 * nenhuma chamada remota acontece: as conquistas de demonstração já trazem os
 * dados da Copa Rio (campeonato de id 1 no serviço), e a aplicação sobe mesmo
 * com o serviço desligado.
 */
@Component
public class ProjectRunner implements CommandLineRunner {

	private static final Long COPA_RIO_ID = 1L;
	private static final String COPA_RIO_NOME = "Copa Rio de Jiu-Jitsu";
	private static final LocalDate COPA_RIO_DATA = LocalDate.of(2026, 5, 17);

	private final AlunoService alunoService;
	private final InstrutorService instrutorService;
	private final TurmaService turmaService;
	private final ConquistaService conquistaService;
	private final GraduacaoService graduacaoService;
	private final boolean habilitado;

	public ProjectRunner(AlunoService alunoService, InstrutorService instrutorService, TurmaService turmaService,
			ConquistaService conquistaService, GraduacaoService graduacaoService,
			@Value("${app.runner.habilitado:true}") boolean habilitado) {
		this.alunoService = alunoService;
		this.instrutorService = instrutorService;
		this.turmaService = turmaService;
		this.conquistaService = conquistaService;
		this.graduacaoService = graduacaoService;
		this.habilitado = habilitado;
	}

	@Override
	public void run(String... args) {

		if (!habilitado || !alunoService.obterLista().isEmpty()) {
			return;
		}

		instrutorService.incluir(new Instrutor("Domingos Caldas", "domingojr@bjjschool.com.br", "(21) 99999-0001",
				Faixa.PRETA, 3, "CBJJ-484817", true));

		Aluno anderson = alunoService.incluir(new Aluno("Anderson Souza", "anderson@gmail.com", "(21) 98888-0002",
				LocalDate.of(1995, 3, 10), LocalDate.of(2024, 2, 1), 82.5, true, Faixa.AZUL, 1));
		Aluno beatriz = alunoService.incluir(new Aluno("Beatriz Lima", "beatriz@gmail.com", "(21) 97777-0003",
				LocalDate.of(2000, 11, 25), LocalDate.of(2025, 6, 15), 61.0, true, Faixa.BRANCA, 2));
		Aluno carlos = alunoService.incluir(new Aluno("Carlos Pereira", "carlos@gmail.com", "(21) 96666-0004",
				LocalDate.of(1988, 7, 2), LocalDate.of(2023, 9, 10), 94.3, false, Faixa.ROXA, 4));

		Turma competicao = turmaService.incluir(new Turma("Adulto Competição", "Seg/Qua/Sex 20h"));
		competicao.setAtiva(true);
		turmaService.alterar(competicao.getId(), competicao);
		turmaService.matricularAluno(competicao.getId(), anderson.getId());
		turmaService.matricularAluno(competicao.getId(), beatriz.getId());

		Turma kids = turmaService.incluir(new Turma("Kids", "Ter/Qui 18h"));
		kids.setAtiva(true);
		turmaService.alterar(kids.getId(), kids);

		registrarPresencas(anderson.getId(), LocalDate.of(2026, 1, 5), 58);
		registrarPresencas(beatriz.getId(), LocalDate.of(2026, 8, 11), 5);

		conquistaService.incluir(anderson.getId(), conquistaNaCopaRio("Adulto Azul Pena", Medalha.OURO));
		conquistaService.incluir(beatriz.getId(), conquistaNaCopaRio("Adulto Branca Leve", Medalha.PRATA));
		conquistaService.incluir(carlos.getId(), conquistaNaCopaRio("Adulto Roxa Pesado", Medalha.BRONZE));

		graduacaoService.registrarGraduacao(beatriz.getId(),
				new Graduacao(Faixa.AZUL, 0, LocalDate.of(2026, 8, 10)));

		System.out.println("BJJ School: dados de demonstração carregados — alunos: "
				+ alunoService.obterLista().size() + ", turmas: " + turmaService.obterLista().size()
				+ ", presenças: " + graduacaoService.obterPresencas().size()
				+ ", conquistas: " + conquistaService.obterLista().size()
				+ ", aptos a graduar: " + graduacaoService.obterAptos().size()
				+ " (campeonatos: campeonato-service)");
	}

	private Conquista conquistaNaCopaRio(String categoria, Medalha medalha) {
		Conquista conquista = new Conquista(categoria, medalha);
		conquista.definirCampeonato(COPA_RIO_ID, COPA_RIO_NOME, COPA_RIO_DATA);

		return conquista;
	}

	private void registrarPresencas(Long alunoId, LocalDate dataInicial, int quantidade) {

		for (int i = 0; i < quantidade; i++) {
			graduacaoService.registrarPresenca(alunoId,
					new Presenca(dataInicial.plusDays(i), i % 2 == 0 ? "Gi" : "No-Gi"));
		}
	}
}
