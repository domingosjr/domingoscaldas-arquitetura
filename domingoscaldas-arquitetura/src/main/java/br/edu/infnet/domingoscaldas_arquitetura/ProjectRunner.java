package br.edu.infnet.domingoscaldas_arquitetura;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.Faixa;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.Campeonato;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.CampeonatoService;
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
 */
@Component
public class ProjectRunner implements CommandLineRunner {

	private final AlunoService alunoService;
	private final InstrutorService instrutorService;
	private final TurmaService turmaService;
	private final CampeonatoService campeonatoService;
	private final ConquistaService conquistaService;
	private final GraduacaoService graduacaoService;
	private final boolean habilitado;

	public ProjectRunner(AlunoService alunoService, InstrutorService instrutorService, TurmaService turmaService,
			CampeonatoService campeonatoService, ConquistaService conquistaService,
			GraduacaoService graduacaoService, @Value("${app.runner.habilitado:true}") boolean habilitado) {
		this.alunoService = alunoService;
		this.instrutorService = instrutorService;
		this.turmaService = turmaService;
		this.campeonatoService = campeonatoService;
		this.conquistaService = conquistaService;
		this.graduacaoService = graduacaoService;
		this.habilitado = habilitado;
	}

	@Override
	public void run(String... args) {

		if (!habilitado) {
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

		Campeonato copaRio = campeonatoService.incluir(
				new Campeonato("Copa Rio de Jiu-Jitsu", "Rio de Janeiro", null, LocalDate.of(2026, 5, 17)));

		conquistaService.registrar(anderson.getId(), copaRio.getId(),
				new Conquista("Adulto Azul Pena", Medalha.OURO));
		conquistaService.registrar(beatriz.getId(), copaRio.getId(),
				new Conquista("Adulto Branca Leve", Medalha.PRATA));
		conquistaService.registrar(carlos.getId(), copaRio.getId(),
				new Conquista("Adulto Roxa Pesado", Medalha.BRONZE));

		graduacaoService.registrarGraduacao(beatriz.getId(),
				new Graduacao(Faixa.AZUL, 0, LocalDate.of(2026, 8, 10)));

		System.out.println("BJJ School: dados de demonstração carregados — alunos: "
				+ alunoService.obterLista().size() + ", turmas: " + turmaService.obterLista().size()
				+ ", presenças: " + graduacaoService.obterPresencas().size()
				+ ", conquistas: " + conquistaService.obterLista().size()
				+ ", aptos a graduar: " + graduacaoService.obterAptos().size());
	}

	private void registrarPresencas(Long alunoId, LocalDate dataInicial, int quantidade) {

		for (int i = 0; i < quantidade; i++) {
			graduacaoService.registrarPresenca(alunoId,
					new Presenca(dataInicial.plusDays(i), i % 2 == 0 ? "Gi" : "No-Gi"));
		}
	}
}
