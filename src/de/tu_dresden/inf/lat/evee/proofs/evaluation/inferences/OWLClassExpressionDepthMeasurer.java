package de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences;

import java.util.Collection;
import java.util.List;

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

public class OWLClassExpressionDepthMeasurer implements OWLClassExpressionVisitorEx<Double> {

	public Double visit(Collection<OWLClassExpression> exprs) {
		return exprs.stream().mapToDouble(c -> c.accept(this)).max().orElse(0d);
	}

	public Double visit(OWLObjectPropertyExpression p) {
		if (p instanceof OWLObjectProperty) {
			return 0d;
		}
		if (p instanceof OWLObjectInverseOf) {
			return 1d + visit(p.getInverseProperty());
		}
		return 0d;
	}

	public Double visit(List<OWLObjectPropertyExpression> l) {
		return l.stream().mapToDouble(this::visit).max().orElse(0d);
	}

	public Double visitIndividuals(Collection<OWLIndividual> s) {
		return 0d;
	}

	private Double visit(OWLIndividual ind) {
		return 0d;
	}

	@Override
	public Double visit(OWLClass ce) {
		return 0d;
	}

	@Override
	public Double visit(OWLObjectIntersectionOf ce) {
		return 1d + visit(ce.getOperands());
	}

	@Override
	public Double visit(OWLObjectUnionOf ce) {
		return 1d + visit(ce.getOperands());
	}

	@Override
	public Double visit(OWLObjectComplementOf ce) {
		return 1d + ce.getOperand().accept(this);
	}

	@Override
	public Double visit(OWLObjectSomeValuesFrom ce) {
		return 1d + Double.max(visit(ce.getProperty()), ce.getFiller().accept(this));
	}

	@Override
	public Double visit(OWLObjectAllValuesFrom ce) {
		return 1d + Double.max(visit(ce.getProperty()), ce.getFiller().accept(this));
	}

	@Override
	public Double visit(OWLObjectHasValue ce) {
		return 1d + visit(ce.getProperty()) + visit(ce.getFiller());
	}

	@Override
	public Double visit(OWLObjectOneOf ce) {
		return 1d + visitIndividuals(ce.getIndividuals());
	}

	// TODO support more constructors


	@Override
	public Double visit(OWLObjectMinCardinality ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLObjectExactCardinality ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLObjectMaxCardinality ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLObjectHasSelf ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLDataSomeValuesFrom ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLDataAllValuesFrom ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLDataHasValue ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLDataMinCardinality ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLDataExactCardinality ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

	@Override
	public Double visit(OWLDataMaxCardinality ce) {
		throw new UnsupportedOperationException("Unsupported class expression type: " + ce);
	}

}
