package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import java.util.List;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.data.Inference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.JustificationComplexityEvaluator;

/**
 * Computes the Manchester complexity measure of the justification underlying
 * the proof, i.e. viewing the proof as a single step from the leaves to the
 * conclusion.
 * 
 * @author stefborg
 *
 */
public class JustificationComplexityProofEvaluator implements IProofEvaluator<OWLAxiom> {

	private IInferenceEvaluator<OWLAxiom> justificationComplexityEvaluator = new JustificationComplexityEvaluator();

	@Override
	public double evaluate(IProof<OWLAxiom> proof) {
		List<OWLAxiom> justification = proof.getInferences().stream().filter(ProofTools::isAsserted)
				.map(IInference::getConclusion).distinct().collect(Collectors.toList());
		IInference<OWLAxiom> justificationInference = new Inference<>(proof.getFinalConclusion(), "", justification);
		Double res = justificationComplexityEvaluator.evaluate(justificationInference);
		return res;
	}

	@Override
	public String getDescription() {
		return "Justification complexity";
	}

}
