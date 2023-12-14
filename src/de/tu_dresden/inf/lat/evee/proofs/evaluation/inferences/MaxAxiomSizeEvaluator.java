/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;
import de.tu_dresden.inf.lat.evee.proofs.tools.measures.OWLAxiomSizeMeasurer;

public class MaxAxiomSizeEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		OWLAxiomSizeMeasurer m = new OWLAxiomSizeMeasurer();
		return ProofTools.getSentences(inf).stream().mapToDouble(ax -> ax.accept(m)).max().orElse(0d);
	}

	@Override
	public String getDescription() {
		return "Maximum axiom size";
	}

}
