/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;

/**
 * @author stefborg
 *
 */
public class SignatureEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		return (double) ProofTools.getSignature(inf).size();
	}

	@Override
	public String getDescription() {
		return "Signature size";
	}

}
