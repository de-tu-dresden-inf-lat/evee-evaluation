package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import java.util.List;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;

public class RuntimeEvaluator<S> implements IProofEvaluator<S> {

	public static final long TIMEOUT = 300000;
	private List<String> log;
	private long generationTime, minimizationTime;

	public void setLog(List<String> log) {
		this.log = log;
	}

	public double getGenerationTime() {
		return (double) generationTime;
	}

	public double getMinimizationTime() {
		return (double) minimizationTime;
	}

	public double evaluate(IProof<S> proof) {
		generationTime = 0;
		minimizationTime = 0;
		long totalTime = 0;
		boolean error = true;

		for (String line : log) {
			if (line.contains(" ms -- ")) {
				String[] parts = line.split(" ms -- ");
				long time = Long.parseLong(parts[0]);
				if (parts[1].equals("Finished")) {
					generationTime = time;
					minimizationTime = 0;
					totalTime = time;
					error = false;
				}
				if (parts[1].equals("Generated")) {
					generationTime = time;
				}
				if (parts[1].equals("Minimized")) {
					minimizationTime = time - generationTime;
					totalTime = time;
					error = false;
				}
			}
			if (line.equals("Timeout")) {
				totalTime = TIMEOUT;
				error = false;
			}
		}

		if (error) {
			generationTime = 0;
			minimizationTime = 0;
			totalTime = TIMEOUT;
		}

		return (double) totalTime;
	}

	@Override
	public String getDescription() {
		return "Total time (ms)";
	}

}
