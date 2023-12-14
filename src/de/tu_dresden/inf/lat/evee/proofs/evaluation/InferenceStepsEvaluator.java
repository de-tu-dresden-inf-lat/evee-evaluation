/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;

/**
 * Counts the number of inference steps in a proof, ignoring "asserted" axioms.
 * If the proof contains an inference several times, it is only counted once.
 *
 * @author stefborg
 * 
 */
class InferenceStepsEvaluator<S> implements IProofEvaluator<S> {

	@Override
	public double evaluate(IProof<S> proof) {
		return (double) proof.getInferences().stream().distinct().filter(inf -> !ProofTools.isAsserted(inf)).count();
	}

	@Override
	public String getDescription() {
		return "Number of distinct inferences";
	}

}
