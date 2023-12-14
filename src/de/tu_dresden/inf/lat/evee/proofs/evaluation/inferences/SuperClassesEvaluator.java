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

public class SuperClassesEvaluator implements IInferenceEvaluator<OWLAxiom> {

	@Override
	public Double evaluate(IInference<OWLAxiom> inf) {
		return (double) ProofTools.getSentences(inf).stream().flatMap(this::getSuperClasses).distinct().count();
	}

	public Stream<OWLClassExpression> getSuperClasses(OWLAxiom ax) {
		if (ax instanceof OWLSubClassOfAxiom) {
			return Stream.of(((OWLSubClassOfAxiom) ax).getSuperClass());
		}
		if (ax instanceof OWLNaryClassAxiom) {
			return ((OWLNaryClassAxiom) ax).getClassExpressions().stream();
		}
		if (ax instanceof OWLSubClassOfAxiomShortCut) {
			return getSuperClasses(((OWLSubClassOfAxiomShortCut) ax).asOWLSubClassOfAxiom());
		}
		return Stream.empty();
	}

	@Override
	public String getDescription() {
		return "Number of different right-hand sides";
	}

}
