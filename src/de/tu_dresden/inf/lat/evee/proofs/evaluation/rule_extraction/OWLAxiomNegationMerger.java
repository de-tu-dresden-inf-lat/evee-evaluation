package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

import de.tu_dresden.inf.lat.evee.proofs.data.Inference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;

class OWLAxiomNegationMerger implements OWLAxiomVisitorEx<OWLAxiom> {

	private OWLClassExpressionNegationMerger cenm;

	public OWLAxiomNegationMerger() {
		this.cenm = new OWLClassExpressionNegationMerger();
	}

	public List<OWLAxiom> visit(Collection<? extends OWLAxiom> axioms) {
		return axioms.stream().map(ax -> ax.accept(this)).collect(Collectors.toList());
	}

	public IInference<OWLAxiom> visit(IInference<OWLAxiom> inf) {
		return new Inference<>(inf.getConclusion().accept(this), "generic", visit(inf.getPremises()));
	}

	@Override
	public OWLAxiom visit(OWLSubClassOfAxiom axiom) {
		return OWLTools.odf.getOWLSubClassOfAxiom(axiom.getSubClass().accept(cenm), axiom.getSuperClass().accept(cenm));
	}

	@Override
	public OWLAxiom visit(OWLDisjointClassesAxiom axiom) {
		return OWLTools.odf.getOWLDisjointClassesAxiom(cenm.visit(axiom.getClassExpressions()));
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyDomainAxiom axiom) {
		return OWLTools.odf.getOWLObjectPropertyDomainAxiom(axiom.getProperty(), axiom.getDomain().accept(cenm));
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyRangeAxiom axiom) {
		return OWLTools.odf.getOWLObjectPropertyRangeAxiom(axiom.getProperty(), axiom.getRange().accept(cenm));
	}

	@Override
	public OWLAxiom visit(OWLEquivalentClassesAxiom axiom) {
		return OWLTools.odf.getOWLEquivalentClassesAxiom(cenm.visit(axiom.getClassExpressions()));
	}

	@Override
	public OWLAxiom visit(OWLSubObjectPropertyOfAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLSubPropertyChainOfAxiom axiom) {
		return axiom;
	}

	// TODO support more axioms

//	@Override
//	public <T> OWLAxiom doDefault(T object) {
//		throw new UnsupportedOperationException("Unsupported axiom type: " + object);
//	}

	@Override
	public OWLAxiom visit(OWLAnnotationAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSubAnnotationPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLAnnotationPropertyDomainAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLAnnotationPropertyRangeAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLReflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyDomainAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDifferentIndividualsAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDisjointDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDisjointObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDisjointUnionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyRangeAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLFunctionalDataPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLEquivalentDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLClassAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLTransitiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSubDataPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSameIndividualAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLInverseObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLHasKeyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(SWRLRule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDeclarationAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDatatypeDefinitionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

}
