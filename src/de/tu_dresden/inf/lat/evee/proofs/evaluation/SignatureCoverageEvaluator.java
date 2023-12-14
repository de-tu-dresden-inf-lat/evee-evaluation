package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;

public class SignatureCoverageEvaluator implements IProofEvaluator<OWLAxiom> {

	private List<String> signature = null;

	public void setSignature(List<String> signature) {
		this.signature = signature;
	}

	@Override
	public double evaluate(IProof<OWLAxiom> proof) {
		if (signature == null) {
			return 0d;
		}
		Set<OWLEntity> proofSignature = ProofTools.getSignature(proof);
		long overlap = proofSignature.stream().map(OWLEntity::getIRI).map(IRI::toString).filter(signature::contains)
				.count();
		return 100d * ((double) overlap) / ((double) proofSignature.size());
	}

	@Override
	public String getDescription() {
		return "Signature coverage (%)";
	}

}
