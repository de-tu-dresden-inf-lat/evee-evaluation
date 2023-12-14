/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

/**
 * @author stefborg
 *
 */
class OWLClassExpressionSetExtractor<T> implements OWLClassExpressionVisitorEx<Set<Set<OWLClass>>> {

	private Class<T> c;

	public OWLClassExpressionSetExtractor(Class<T> c) {
		this.c = c;
	}

	public Set<Set<OWLClass>> visit(Set<OWLClassExpression> exprs) {
		return exprs.stream().map(ax -> ax.accept(this)).reduce(new HashSet<>(), (s1, s2) -> {
			s1.addAll(s2);
			return s1;
		});
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLClass ce) {
		Set<Set<OWLClass>> ret = new HashSet<>();
		Set<OWLClass> s = new HashSet<>();
		s.add(ce);
		ret.add(s);
		return ret;
	}

	public Set<Set<OWLClass>> visitNary(OWLNaryBooleanClassExpression ce) {
		Set<Set<OWLClass>> ret = new HashSet<>();
		Set<OWLClass> s = new HashSet<>();
		ce.getOperands().forEach(op -> {
			if (c.isInstance(ce) && op instanceof OWLClass) {
				s.add((OWLClass) op);
			} else {
				ret.addAll(op.accept(this));
			}
		});
		if (!s.isEmpty()) {
			ret.add(s);
		}
		return ret;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectIntersectionOf ce) {
		return visitNary(ce);
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectUnionOf ce) {
		return visitNary(ce);
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectComplementOf ce) {
		return ce.getOperand().accept(this);
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectSomeValuesFrom ce) {
		return ce.getFiller().accept(this);
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectAllValuesFrom ce) {
		return ce.getFiller().accept(this);
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectOneOf ce) {
		return new HashSet<>();
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectHasValue ce) {
		return new HashSet<>();
	}

	// TODO support more constructors

//	@Override
//	public <T> Set<Set<OWLClass>> doDefault(T object) {
//		throw new UnsupportedOperationException("Unsupported class expression type: " + object);
//	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectMinCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLObjectHasSelf ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataSomeValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataAllValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataHasValue ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataMinCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Set<OWLClass>> visit(OWLDataMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

}
