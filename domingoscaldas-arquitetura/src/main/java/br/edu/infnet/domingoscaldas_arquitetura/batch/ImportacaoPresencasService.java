package br.edu.infnet.domingoscaldas_arquitetura.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Service;

/**
 * Dispara o Job de importação de presenças e resume o resultado.
 */
@Service
public class ImportacaoPresencasService {

	private final JobOperator jobOperator;
	private final Job importarPresencasJob;

	public ImportacaoPresencasService(JobOperator jobOperator, Job importarPresencasJob) {
		this.jobOperator = jobOperator;
		this.importarPresencasJob = importarPresencasJob;
	}

	/** Executa uma nova instância do Job (o RunIdIncrementer gera parâmetros novos a cada chamada). */
	public ImportacaoResponse importar() {
		JobExecution execucao = jobOperator.startNextInstance(importarPresencasJob);

		StepExecution importacao = execucao.getStepExecutions().stream()
				.filter(passo -> BatchConfig.STEP_IMPORTACAO.equals(passo.getStepName()))
				.findFirst()
				.orElseThrow();

		return new ImportacaoResponse(execucao.getId(), execucao.getStatus().name(), importacao.getReadCount(),
				importacao.getFilterCount(), importacao.getWriteCount());
	}
}
