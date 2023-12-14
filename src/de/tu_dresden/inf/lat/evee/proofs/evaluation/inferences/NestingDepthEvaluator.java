package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;

public class NestingDepthEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		return new OWLAxiomDepthMeasurer().visit(inf);
	}

	@Override
	public String getDescription() {
		return "Nesting depth of class expressions";
	}

}
