package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;

public class DifferentClassesEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		Set<OWLClassExpression> expressions = new HashSet<OWLClassExpression>();
		for (OWLAxiom axiom : ProofTools.getSentences(inf)) {
			expressions.addAll(axiom.getNestedClassExpressions());
		}
		return (double) expressions.size();
	}

	@Override
	public String getDescription() {
		return "Number of distinct subexpressions";
	}

}
