package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNaryClassAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiomShortCut;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInferenceEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;

public class SubClassesEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		return (double) ProofTools.getSentences(inf).stream().flatMap(this::getSubClasses).distinct().count();
	}

	public Stream<OWLClassExpression> getSubClasses(OWLAxiom ax) {
		if (ax instanceof OWLSubClassOfAxiom) {
			return Stream.of(((OWLSubClassOfAxiom) ax).getSubClass());
		}
		if (ax instanceof OWLNaryClassAxiom) {
			return ((OWLNaryClassAxiom) ax).getClassExpressions().stream();
		}
		if (ax instanceof OWLSubClassOfAxiomShortCut) {
			return getSubClasses(((OWLSubClassOfAxiomShortCut) ax).asOWLSubClassOfAxiom());
		}
		return Stream.empty();
	}

	@Override
	public String getDescription() {
		return "Number of different left-hand sides";
	}

}
