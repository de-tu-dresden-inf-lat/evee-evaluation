/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
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

import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;

/**
 * @author stefborg
 *
 */
class OWLClassExpressionNegationMerger implements OWLClassExpressionVisitorEx<OWLClassExpression> {

	public Set<OWLClassExpression> visit(Collection<OWLClassExpression> exprs) {
		return exprs.stream().map(c -> c.accept(this)).collect(Collectors.toSet());
	}

	@Override
	public OWLClassExpression visit(OWLClass ce) {
		return ce;
	}

	public OWLClassExpression visitNary(OWLNaryBooleanClassExpression ce,
			Function<Set<? extends OWLClassExpression>, OWLClassExpression> constructor,
			Function<Set<? extends OWLClassExpression>, OWLClassExpression> dualConstructor) {
		Set<OWLClass> negatedClasses = new HashSet<>();
		Set<OWLClassExpression> rest = new HashSet<>();
		for (OWLClassExpression expr : ce.getOperands()) {
			OWLClass c = getNegatedClass(expr);
			if (c != null) {
				negatedClasses.add(c);
			} else {
				rest.add(expr.accept(this));
			}
		}

		OWLClassExpression negation = null;
		if (negatedClasses.size() > 1) {
			negation = OWLTools.odf.getOWLObjectComplementOf(dualConstructor.apply(negatedClasses));
		} else if (negatedClasses.size() == 1) {
			negation = OWLTools.odf.getOWLObjectComplementOf(negatedClasses.iterator().next());
		}

		if (rest.size() > 0) {
			if (negation != null) {
				rest.add(negation);
			}
			return constructor.apply(rest);
		} else {
			return negation;
		}
	}

	private OWLClass getNegatedClass(OWLClassExpression ce) {
		if (ce instanceof OWLObjectComplementOf) {
			OWLClassExpression operand = ((OWLObjectComplementOf) ce).getOperand();
			if (operand instanceof OWLClass) {
				return (OWLClass) operand;
			}
		}
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectIntersectionOf ce) {
		return visitNary(ce, OWLTools.odf::getOWLObjectIntersectionOf, OWLTools.odf::getOWLObjectUnionOf);
	}

	@Override
	public OWLClassExpression visit(OWLObjectUnionOf ce) {
		return visitNary(ce, OWLTools.odf::getOWLObjectUnionOf, OWLTools.odf::getOWLObjectIntersectionOf);
	}

	@Override
	public OWLClassExpression visit(OWLObjectComplementOf ce) {
		OWLClassExpression operand = ce.getOperand().accept(this);
		if (operand instanceof OWLObjectComplementOf) {
			return ((OWLObjectComplementOf) operand).getOperand();
		} else {
			return OWLTools.odf.getOWLObjectComplementOf(operand);
		}
	}

	@Override
	public OWLClassExpression visit(OWLObjectSomeValuesFrom ce) {
		return OWLTools.odf.getOWLObjectSomeValuesFrom(ce.getProperty(), ce.getFiller().accept(this));
	}

	@Override
	public OWLClassExpression visit(OWLObjectAllValuesFrom ce) {
		return OWLTools.odf.getOWLObjectAllValuesFrom(ce.getProperty(), ce.getFiller().accept(this));
	}

	@Override
	public OWLClassExpression visit(OWLObjectOneOf ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasValue ce) {
		return ce;
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
