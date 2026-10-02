package br.edu.infnet.domingoscaldas_arquitetura.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import br.edu.infnet.domingoscaldas_arquitetura.graduacao.GraduacaoService;
import br.edu.infnet.domingoscaldas_arquitetura.graduacao.Presenca;

/**
 * Etapa 4 — processamento em lote: importação da lista de presença (CSV).
 * Fonte (CSV) → ItemReader → ItemProcessor → ItemWriter → destino (tabela
 * presencas da aplicação principal), em chunks de 5 linhas.
 */
@Configuration
public class BatchConfig {

	private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);

	static final String STEP_IMPORTACAO = "importarPresencasStep";

	/** Leitor: uma linha do CSV por vez (pula o cabeçalho; campos separados por ponto e vírgula). */
	@Bean
	FlatFileItemReader<PresencaBatch> presencaReader() {
		return new FlatFileItemReaderBuilder<PresencaBatch>()
				.name("presencaReader")
				.resource(new ClassPathResource("batch/presencas.csv"))
				.linesToSkip(1)
				.delimited()
				.delimiter(";")
				.names("alunoId", "data", "tipoTreino")
				.targetType(PresencaBatch.class)
				.build();
	}

	/** Escritor: grava as presenças do chunk pelo serviço do módulo graduação (o banco da própria aplicação). */
	@Bean
	ItemWriter<PresencaImportada> presencaWriter(GraduacaoService graduacaoService) {
		return chunk -> {
			for (PresencaImportada presenca : chunk.getItems()) {
				graduacaoService.registrarPresenca(presenca.alunoId(),
						new Presenca(presenca.data(), presenca.tipoTreino()));
			}
			log.info("Chunk gravado: {} presença(s)", chunk.getItems().size());
		};
	}

	/** Passo 1: ler → processar → gravar, confirmando a transação a cada 5 linhas (chunk). */
	@Bean
	Step importarPresencasStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FlatFileItemReader<PresencaBatch> presencaReader, PresencaProcessor presencaProcessor,
			ItemWriter<PresencaImportada> presencaWriter) {
		return new StepBuilder(STEP_IMPORTACAO, jobRepository)
				.<PresencaBatch, PresencaImportada>chunk(5)
				.transactionManager(transactionManager)
				.reader(presencaReader)
				.processor(presencaProcessor)
				.writer(presencaWriter)
				.build();
	}

	/** Passo 2: tarefa única que registra o resumo da importação. */
	@Bean
	Step resumirImportacaoStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("resumirImportacaoStep", jobRepository)
				.tasklet((contribution, chunkContext) -> {
					StepExecution importacao = chunkContext.getStepContext().getStepExecution().getJobExecution()
							.getStepExecutions().stream()
							.filter(passo -> STEP_IMPORTACAO.equals(passo.getStepName()))
							.findFirst()
							.orElseThrow();

					log.info("Importação de presenças concluída: {} linha(s) lida(s), {} ignorada(s), {} gravada(s)",
							importacao.getReadCount(), importacao.getFilterCount(), importacao.getWriteCount());

					return RepeatStatus.FINISHED;
				}, transactionManager)
				.build();
	}

	/** O Job: primeiro importa, depois resume. O RunIdIncrementer permite executar de novo. */
	@Bean
	Job importarPresencasJob(JobRepository jobRepository, Step importarPresencasStep, Step resumirImportacaoStep) {
		return new JobBuilder("importarPresencasJob", jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(importarPresencasStep)
				.next(resumirImportacaoStep)
				.build();
	}
}
