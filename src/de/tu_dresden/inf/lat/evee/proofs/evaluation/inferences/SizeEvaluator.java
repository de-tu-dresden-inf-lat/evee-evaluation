package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.measures.OWLAxiomSizeMeasurer;

public class SizeEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		return new OWLAxiomSizeMeasurer().visit(inf);
	}

	@Override
	public String getDescription() {
		return "Expression size";
	}

}
