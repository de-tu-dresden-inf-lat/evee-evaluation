package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;
import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;

public class ClassExpressionTypesEvaluator implements IInferenceEvaluator<OWLAxiom> {

	private Collection<ClassExpressionType> types;

	public ClassExpressionTypesEvaluator(ClassExpressionType... types) {
		this.types = Arrays.asList(types);
	}

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		return OWLTools.getClassExpressionTypes(ProofTools.getSentences(inf)).containsAll(types) ? 1d : 0d;
	}

	@Override
	public String getDescription() {
		return "Contains " + types.stream().map(ClassExpressionType::toString).collect(Collectors.joining(" and "));
	}

}
