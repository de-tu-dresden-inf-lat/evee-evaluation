package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import de.tu_dresden.inf.lat.evee.proofs.data.exceptions.ProofException;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;

public class MinimizationTimeEvaluator<S> implements IProofEvaluator<S> {

	private RuntimeEvaluator<S> runtimeEvaluator;

	public MinimizationTimeEvaluator(RuntimeEvaluator<S> runtimeEvaluator) {
		this.runtimeEvaluator = runtimeEvaluator;
	}

	@Override
	public double evaluate(IProof<S> proof) throws ProofException {
		return runtimeEvaluator.getMinimizationTime();
	}

	@Override
	public String getDescription() {
		return "Minimization time (ms)";
	}

}
