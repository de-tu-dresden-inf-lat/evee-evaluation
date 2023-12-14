/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;

/**
 * @author stefborg
 *
 */
class OWLClassExpressionReplacer implements OWLClassExpressionVisitorEx<OWLClassExpression> {

	private OWLObjectMap map;

	public OWLClassExpressionReplacer(OWLObjectMap map) {
		this.map = map;
	}

	public Set<OWLClassExpression> visit(Collection<OWLClassExpression> s) {
		return s.stream().map(c -> c.accept(this)).collect(Collectors.toSet());
	}

	private <T extends OWLClassExpression> OWLClassExpression replaceIfContained(T ce, Supplier<OWLClassExpression> f) {
		if (map.contains(ce)) {
			return map.applyClass(ce);
		} else if (map.containsSubset(ce)) {
			return (OWLClassExpression) map.applySubset(ce);
		} else {
			return f.get();
		}
	}

	public List<OWLObjectPropertyExpression> visit(List<OWLObjectPropertyExpression> l) {
		return l.stream().map(this::visit).collect(Collectors.toList());
	}

	public OWLObjectPropertyExpression visit(OWLObjectPropertyExpression p) {
		if (map.contains(p)) {
			return map.applyProperty(p);
		} else {
			if (p instanceof OWLObjectProperty) {
				return p;
			}
			if (p instanceof OWLObjectInverseOf) {
				return OWLTools.odf
						.getOWLObjectInverseOf((OWLObjectProperty) visit(((OWLObjectInverseOf) p).getInverse()));
			}
			return null;
		}
	}

	public Set<OWLIndividual> visitIndividuals(Set<OWLIndividual> s) {
		return s.stream().map(this::visit).collect(Collectors.toSet());
	}

	public OWLIndividual visit(OWLIndividual i) {
		if (map.contains(i)) {
			return map.applyIndividual(i);
		} else {
			return i;
		}
	}

	@Override
	public OWLClassExpression visit(OWLClass ce) {
		return replaceIfContained(ce, () -> ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectIntersectionOf ce) {
		return replaceIfContained(ce, () -> OWLTools.odf.getOWLObjectIntersectionOf(visit(ce.getOperands())));
	}

	@Override
	public OWLClassExpression visit(OWLObjectUnionOf ce) {
		return replaceIfContained(ce, () -> OWLTools.odf.getOWLObjectUnionOf(visit(ce.getOperands())));
	}

	@Override
	public OWLClassExpression visit(OWLObjectComplementOf ce) {
		return replaceIfContained(ce, () -> OWLTools.odf.getOWLObjectComplementOf(ce.getOperand().accept(this)));
	}

	@Override
	public OWLClassExpression visit(OWLObjectSomeValuesFrom ce) {
		return replaceIfContained(ce,
				() -> OWLTools.odf.getOWLObjectSomeValuesFrom(visit(ce.getProperty()), ce.getFiller().accept(this)));
	}

	@Override
	public OWLClassExpression visit(OWLObjectAllValuesFrom ce) {
		return replaceIfContained(ce,
				() -> OWLTools.odf.getOWLObjectAllValuesFrom(visit(ce.getProperty()), ce.getFiller().accept(this)));
	}

	@Override
	public OWLClassExpression visit(OWLObjectOneOf ce) {
		return replaceIfContained(ce, () -> OWLTools.odf.getOWLObjectOneOf(visitIndividuals(ce.getIndividuals())));
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasValue ce) {
		return replaceIfContained(ce, () -> OWLTools.odf.getOWLObjectSomeValuesFrom(visit(ce.getProperty()),
				OWLTools.odf.getOWLObjectOneOf(ce.getFiller()).accept(this)));
	}

	// TODO support more constructors

//	@Override
//	public <T> OWLClassExpression doDefault(T object) {
//		throw new UnsupportedOperationException("Unsupported class expression type: " + object);
//	}

	@Override
	public OWLClassExpression visit(OWLObjectMinCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasSelf ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataSomeValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataAllValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataHasValue ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataMinCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

}
