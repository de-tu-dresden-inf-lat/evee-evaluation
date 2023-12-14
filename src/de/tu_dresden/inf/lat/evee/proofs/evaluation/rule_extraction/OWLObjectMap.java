/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;

/**
 * @author stefborg
 *
 */
public class OWLObjectMap {

	private Map<OWLObject, OWLObject> map = new HashMap<>();
	protected Collection<OWLObject> domain = new LinkedList<>();
	protected Collection<OWLObject> range = new LinkedList<>();
	protected Collection<OWLClassExpression> domainSubconcepts = new LinkedList<>();
	protected Collection<OWLClassExpression> rangeSubconcepts = new LinkedList<>();
	protected Collection<OWLObjectProperty> domainSubroles = new LinkedList<>();
	protected Collection<OWLObjectProperty> rangeSubroles = new LinkedList<>();

	public boolean contains(OWLObject e1) {
		return domain.contains(e1);
	}

	public boolean containsSubset(OWLClassExpression expr) {
		if (expr instanceof OWLObjectUnionOf) {
			return getDomain().stream().filter(o -> o instanceof OWLObjectUnionOf).anyMatch(
					o -> subsetOf(((OWLObjectUnionOf) o).getOperands(), ((OWLObjectUnionOf) expr).getOperands()));
		}
		if (expr instanceof OWLObjectIntersectionOf) {
			return getDomain().stream().filter(o -> o instanceof OWLObjectIntersectionOf)
					.anyMatch(o -> subsetOf(((OWLObjectIntersectionOf) o).getOperands(),
							((OWLObjectIntersectionOf) expr).getOperands()));
		}
		return false;
	}

	private boolean subsetOf(Set<OWLClassExpression> s1, Set<OWLClassExpression> s2) {
		return s2.containsAll(s1);
	}

	public boolean add(OWLObject o1, OWLObject o2) {
		if ((o1 == null) || (o2 == null)) {
			throw new IllegalArgumentException("Cannot map 'null': " + o1 + " -> " + o2);
		}
		if ((o1 instanceof OWLClassExpression) && (o2 instanceof OWLClassExpression)) {
			return addClasses((OWLClassExpression) o1, (OWLClassExpression) o2);
		}
		if ((o1 instanceof OWLObjectPropertyExpression) && (o2 instanceof OWLObjectPropertyExpression)) {
			return addProperties((OWLObjectPropertyExpression) o1, (OWLObjectPropertyExpression) o2);
		}
		if ((o1 instanceof OWLIndividual) && (o2 instanceof OWLIndividual)) {
			return addIndividuals((OWLIndividual) o1, (OWLIndividual) o2);
		}
		throw new IllegalArgumentException("Cannot map between different types of OWL objects:\n" + o1 + "\n" + o2);
	}

	public boolean addClasses(OWLClassExpression c1, OWLClassExpression c2) {
		// Make sure that c1 does not share a subexpression with any element of the
		// domain of this map, and similarly for c2. Also, top and bottom should never
		// be mapped.
		if (!c1.getNestedClassExpressions().stream().anyMatch(domainSubconcepts::contains)
				&& !c2.getNestedClassExpressions().stream().anyMatch(rangeSubconcepts::contains)
				&& !c1.getObjectPropertiesInSignature().stream().anyMatch(domainSubroles::contains)
				&& !c2.getObjectPropertiesInSignature().stream().anyMatch(rangeSubroles::contains)
				&& !Utils.areTopBottom(c1, c2)) {
//		if (!domain.contains(c1) && !range.contains(c2) && !Utils.areTopBottom(c1, c2)) {
			map.put(c1, c2);
			domain.add(c1);
			range.add(c2);
			c1.getNestedClassExpressions().forEach(domainSubconcepts::add);
			c2.getNestedClassExpressions().forEach(rangeSubconcepts::add);
			c1.getObjectPropertiesInSignature().forEach(domainSubroles::add);
			c2.getObjectPropertiesInSignature().forEach(rangeSubroles::add);
			return true;
		}
		return false;
	}

	public boolean addProperties(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2) {
		if (!p1.getObjectPropertiesInSignature().stream().anyMatch(domainSubroles::contains)
				&& !p2.getObjectPropertiesInSignature().stream().anyMatch(rangeSubroles::contains)
				&& !Utils.areTopBottom(p1, p2)) {
//		if (!domain.contains(p1) && !range.contains(p2) && !Utils.areTopBottom(p1, p2)) {
			map.put(p1, p2);
			domain.add(p1);
			range.add(p2);
			p1.getObjectPropertiesInSignature().forEach(domainSubroles::add);
			p2.getObjectPropertiesInSignature().forEach(rangeSubroles::add);
			return true;
		}
		return false;
	}

	public boolean addIndividuals(OWLIndividual i1, OWLIndividual i2) {
		if (!domain.contains(i1) && !range.contains(i2)) {
			map.put(i1, i2);
			domain.add(i1);
			range.add(i2);
			return true;
		}
		return false;
	}

	public OWLObject remove(OWLObject o) {
		OWLObject o2 = map.remove(o);
		if (o2 != null) {
			domain.remove(o);
			range.remove(o2);
		}
		return o2;
	}

	public OWLObject apply(OWLObject o) {
		return map.get(o);
	}

	public OWLObject applySubset(OWLObject o) {
		if (o instanceof OWLObjectUnionOf) {
			return OWLTools.odf.getOWLObjectUnionOf(applyNAry((OWLObjectUnionOf) o));
		}
		if (o instanceof OWLObjectIntersectionOf) {
			return OWLTools.odf.getOWLObjectIntersectionOf(applyNAry((OWLObjectIntersectionOf) o));
		}
		return null;
	}

	private <T extends OWLNaryBooleanClassExpression> Set<OWLClassExpression> applyNAry(T o) {
		Set<OWLClassExpression> operands = o.getOperands();
		for (Entry<OWLObject, OWLObject> e : map.entrySet()) {
			if (o.getClass().isInstance(e.getKey())) {
				Set<OWLClassExpression> sub = o.getClass().cast(e.getKey()).getOperands();
				if (operands.containsAll(sub)) {
					operands.removeAll(sub);
					operands.add((OWLClassExpression) e.getValue());
				}
			}
		}
		return operands;
	}

	public OWLClassExpression applyClass(OWLClassExpression c) {
		return (OWLClassExpression) map.get(c);
	}

	public OWLObjectPropertyExpression applyProperty(OWLObjectPropertyExpression p) {
		return (OWLObjectPropertyExpression) map.get(p);
	}

	public OWLIndividual applyIndividual(OWLIndividual i) {
		return (OWLIndividual) map.get(i);
	}

//	public final IInference apply(IInference inf) {
//		return new Inference(apply(inf.getConclusion()), "generic", apply(inf.getPremises()));
//	}
//
//	public final List<OWLAxiom> apply(List<? extends OWLAxiom> axioms) {
//		return axioms.stream().map(this::apply).collect(Collectors.toList());
//	}
//
//	public final OWLAxiom apply(OWLAxiom ax) {
//		if (ax instanceof OWLSubClassOfAxiom) {
//			OWLSubClassOfAxiom sco = (OWLSubClassOfAxiom) ax;
//			return Main.odf.getOWLSubClassOfAxiom(apply(sco.getSubClass()), apply(sco.getSuperClass()));
//		}
//		if (ax instanceof OWLEquivalentClassesAxiom) {
//			return Main.odf.getOWLEquivalentClassesAxiom(apply(((OWLEquivalentClassesAxiom) ax).classExpressions()));
//		}
//		if (ax instanceof OWLDisjointClassesAxiom) {
//			return Main.odf.getOWLDisjointClassesAxiom(apply(((OWLDisjointClassesAxiom) ax).classExpressions()));
//		}
//		if (ax instanceof OWLObjectPropertyDomainAxiom) {
//			OWLObjectPropertyDomainAxiom d = (OWLObjectPropertyDomainAxiom) ax;
//			return Main.odf.getOWLObjectPropertyDomainAxiom(apply(d.getProperty()), apply(d.getDomain()));
//		}
//		if (ax instanceof OWLObjectPropertyRangeAxiom) {
//			OWLObjectPropertyRangeAxiom r = (OWLObjectPropertyRangeAxiom) ax;
//			return Main.odf.getOWLObjectPropertyRangeAxiom(apply(r.getProperty()), apply(r.getRange()));
//		}
//		throw new UnsupportedOperationException("Unsupported axiom type:\n" + Main.toString(ax));
//	}
//
//	public final OWLObjectPropertyExpression apply(OWLObjectPropertyExpression role) {
//		if (role instanceof OWLObjectInverseOf) {
//			return Main.odf.getOWLObjectInverseOf((OWLObjectProperty) applyEntity(role.getNamedProperty()));
//		}
//		if (role instanceof OWLObjectProperty) {
//			return (OWLObjectProperty) applyEntity((OWLObjectProperty) role);
//		}
//		return null;
//	}
//
//	public final Stream<OWLClassExpression> apply(Stream<OWLClassExpression> exprs) {
//		return exprs.map(this::apply);
//	}
//
//	public final OWLClassExpression apply(OWLClassExpression c) {
//		if (c instanceof OWLObjectIntersectionOf) {
//			return Main.odf.getOWLObjectIntersectionOf(apply(((OWLObjectIntersectionOf) c).operands()));
//		}
//		if (c instanceof OWLObjectUnionOf) {
//			return Main.odf.getOWLObjectUnionOf(apply(((OWLObjectUnionOf) c).operands()));
//		}
//		if (c instanceof OWLObjectComplementOf) {
//			return Main.odf.getOWLObjectComplementOf(apply(((OWLObjectComplementOf) c).getOperand()));
//		}
//		if (c instanceof OWLObjectSomeValuesFrom) {
//			OWLObjectSomeValuesFrom e = (OWLObjectSomeValuesFrom) c;
//			return Main.odf.getOWLObjectSomeValuesFrom(apply(e.getProperty()), apply(e.getFiller()));
//		}
//		if (c instanceof OWLObjectAllValuesFrom) {
//			OWLObjectAllValuesFrom a = (OWLObjectAllValuesFrom) c;
//			return Main.odf.getOWLObjectAllValuesFrom(apply(a.getProperty()), apply(a.getFiller()));
//		}
//		if (c instanceof OWLClass) {
//			if (c.isOWLThing() || c.isOWLNothing()) {
//				return c;
//			}
//			return (OWLClass) applyEntity((OWLClass) c);
//		}
//		throw new UnsupportedOperationException("Unsupported class expression type:\n" + Main.toString(c));
//	}

	public Collection<OWLObject> getDomain() {
		return domain;
	}

	public Collection<OWLObject> getRange() {
		return range;
	}

	@Override
	public String toString() {
		return map.toString();
	}

}
