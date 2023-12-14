package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import java.util.Collection;

import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;
import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;

public class TaskSignatureSizeEvaluator implements IProofEvaluator<OWLAxiom> {

	Collection<? extends OWLAxiom> ontology;
	OWLAxiom goalAxiom;

	public void setTask(IInference<OWLAxiom> task) {
		this.ontology = task.getPremises();
		this.goalAxiom = task.getConclusion();
	}

	public void setOntology(Collection<? extends OWLAxiom> ontology) {
		this.ontology = ontology;
	}

	public void setGoalAxiom(OWLAxiom goalAxiom) {
		this.goalAxiom = goalAxiom;
	}

	@Override
	public double evaluate(IProof<OWLAxiom> proof) {
		return (double) OWLTools.getSignature(ontology).size();
	}

	@Override
	public String getDescription() {
		return "Signature size of the task";
	}

}
