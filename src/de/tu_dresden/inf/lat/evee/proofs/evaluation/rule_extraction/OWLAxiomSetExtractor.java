/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClass;
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

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;

/**
 * @author stefborg
 *
 */
class OWLAxiomSetExtractor<T> implements OWLAxiomVisitorEx<Set<Set<OWLClass>>> {

	private OWLClassExpressionSetExtractor<T> cese;

	public OWLAxiomSetExtractor(Class<T> c) {
		cese = new OWLClassExpressionSetExtractor<T>(c);
	}

	public Set<Set<OWLClass>> visit(List<? extends OWLAxiom> axioms) {
		return axioms.stream().map(ax -> ax.accept(this)).reduce(new HashSet<>(), (s1, s2) -> {
			s1.addAll(s2);
			return s1;
		});
	}

	public Set<Set<OWLClass>> visit(IInference<OWLAxiom> inf) {
		Set<Set<OWLClass>> ret = inf.getConclusion().accept(this);
		ret.addAll(visit(inf.getPremises()));
		return ret;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLSubClassOfAxiom axiom) {
		Set<Set<OWLClass>> ret = axiom.getSubClass().accept(cese);
		ret.addAll(axiom.getSuperClass().accept(cese));
		return ret;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDisjointClassesAxiom axiom) {
		return cese.visit(axiom.getClassExpressions());
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectPropertyDomainAxiom axiom) {
		return axiom.getDomain().accept(cese);
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectPropertyRangeAxiom axiom) {
		return axiom.getRange().accept(cese);
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLEquivalentClassesAxiom axiom) {
		return cese.visit(axiom.getClassExpressions());
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLSubObjectPropertyOfAxiom axiom) {
		return new HashSet<>();
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLSubPropertyChainOfAxiom axiom) {
		return new HashSet<>();
	}

	// TODO support more axioms

//	@Override
//	public <X> Set<Set<OWLClass>> doDefault(X object) {
//		throw new UnsupportedOperationException("Unsupported axiom type: " + object);
//	}

	@Override
	public Set<Set<OWLClass>> visit(OWLAnnotationAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLSubAnnotationPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLAnnotationPropertyDomainAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLAnnotationPropertyRangeAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLReflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataPropertyDomainAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDifferentIndividualsAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDisjointDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDisjointObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDisjointUnionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLSymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataPropertyRangeAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLFunctionalDataPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLEquivalentDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLClassAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLTransitiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLSubDataPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLSameIndividualAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLInverseObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLHasKeyAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(SWRLRule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDeclarationAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDatatypeDefinitionAxiom axiom) {
		// TODO Auto-generated method stub
		return null;
	}

}
