/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

/**
 * @author stefborg
 *
 */
class CompositeOWLObjectMap extends OWLObjectMap {

	private OWLObjectMap origMap;

	public CompositeOWLObjectMap(OWLObjectMap origMap) {
		this.origMap = origMap;
		this.domain.addAll(origMap.domain);
		this.range.addAll(origMap.range);
		this.domainSubconcepts.addAll(origMap.domainSubconcepts);
		this.rangeSubconcepts.addAll(origMap.rangeSubconcepts);
		this.domainSubroles.addAll(origMap.domainSubroles);
		this.rangeSubroles.addAll(origMap.rangeSubroles);
	}

	@Override
	public OWLObject apply(OWLObject o) {
		OWLObject ret = origMap.apply(o);
		if (ret == null) {
			ret = super.apply(o);
		}
		return ret;
	}

	@Override
	public OWLObject applySubset(OWLObject o) {
		return super.apply(origMap.applySubset(o));
	}

	@Override
	public OWLClassExpression applyClass(OWLClassExpression c) {
		OWLClassExpression ret = origMap.applyClass(c);
		if (ret == null) {
			ret = super.applyClass(c);
		}
		return ret;
	}

	@Override
	public OWLObjectPropertyExpression applyProperty(OWLObjectPropertyExpression p) {
		OWLObjectPropertyExpression ret = origMap.applyProperty(p);
		if (ret == null) {
			ret = super.applyProperty(p);
		}
		return ret;
	}

	@Override
	public OWLIndividual applyIndividual(OWLIndividual i) {
		OWLIndividual ret = origMap.applyIndividual(i);
		if (ret == null) {
			ret = super.applyIndividual(i);
		}
		return ret;
	}

	@Override
	public OWLObject remove(OWLObject o) {
		OWLObject o2 = origMap.remove(o);
		if (o2 != null) {
			domain.remove(o);
			range.remove(o2);
			return o2;
		}
		return super.remove(o);
	}

	@Override
	public String toString() {
		String ret = origMap.toString();
		ret += "\n";
		ret += super.toString();
		return ret;
	}

}
