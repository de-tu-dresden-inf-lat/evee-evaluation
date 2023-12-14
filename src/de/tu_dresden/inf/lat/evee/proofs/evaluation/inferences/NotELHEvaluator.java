package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;

public class NotELHEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		return Utils.isELHInference(inf) ? 0d : 1d;
	}

	@Override
	public String getDescription() {
		return "Is not in EL";
	}

}
