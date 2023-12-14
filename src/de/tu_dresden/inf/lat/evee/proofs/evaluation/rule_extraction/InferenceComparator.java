/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNaryClassAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;

/**
 * @author stefborg
 *
 *         Compares two inferences by checking whether they are isomorphic.
 *         Treats the premises as sets instead of lists.
 *
 */
class InferenceComparator {

	private boolean allowToSimplify;

	public InferenceComparator(boolean allowToSimplify) {
		this.allowToSimplify = allowToSimplify;
	}

	public OWLObjectMap isomorphic(IInference<OWLAxiom> inf1, IInference<OWLAxiom> inf2) {

		if (equals(inf1, inf2)) {
			return new OWLObjectMap();
		}

		if (!allowToSimplify) {
			if (inf1.getPremises().size() != inf2.getPremises().size()) {
				return null;
			}
			Set<OWLEntity> sig1 = ProofTools.getSignature(inf1);
			Set<OWLEntity> sig2 = ProofTools.getSignature(inf2);
			if (sig1.size() != sig2.size()) {
				return null;
			}
		}

		// TODO: remove duplicate maps at intermediate steps?
		Set<OWLObjectMap> maps = new HashSet<>();
		maps.add(new OWLObjectMap());

		try {
			maps = check(inf1.getConclusion(), inf2.getConclusion(), maps);
			maps = checkSet(new HashSet<>(inf1.getPremises()), new HashSet<>(inf2.getPremises()), maps);
		} catch (RuntimeException e) {
			System.err.println(inf1);
			System.err.println(inf2);
			throw new RuntimeException(e);
		}

		for (OWLObjectMap map : maps) {
			try {
				if (equals(new OWLAxiomReplacer(map).visit(inf1), inf2)) {
					if (!hasOverlaps(map)) {
						return map;
					}
				}
			} catch (RuntimeException e) {
				System.err.println(inf1);
				System.err.println(inf2);
				System.err.println(map);
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private boolean hasOverlaps(OWLObjectMap map) {
		Collection<OWLObject> domain = map.getDomain();
		for (OWLObject o1 : domain) {
			if (o1.getNestedClassExpressions().stream().filter(c -> !c.equals(o1)).anyMatch(domain::contains)) {
				return true;
			}
		}
		return false;
	}

	private boolean equals(IInference<OWLAxiom> inf1, IInference<OWLAxiom> inf2) {
		return inf1.getConclusion().equals(inf2.getConclusion()) && inf1.getPremises().containsAll(inf2.getPremises())
				&& inf2.getPremises().containsAll(inf1.getPremises());
	}

	public Set<OWLObjectMap> checkList(List<? extends OWLObject> l1, List<? extends OWLObject> l2,
			Set<OWLObjectMap> origMaps) {
		if (l1.size() != l2.size()) {
			return new HashSet<>();
		}
		Set<OWLObjectMap> newMaps = origMaps;
		Iterator<? extends OWLObject> i1 = l1.iterator();
		Iterator<? extends OWLObject> i2 = l2.iterator();
		while (i1.hasNext()) {
			newMaps = check(i1.next(), i2.next(), origMaps);
		}
		return newMaps;
	}

	// TODO: extend to maps between subsets?? (only for OWLClassExpressions)
	public Set<OWLObjectMap> checkSet(Set<? extends OWLObject> s1, Set<? extends OWLObject> s2,
			Set<OWLObjectMap> origMaps) {
		Set<OWLObjectMap> newMaps = origMaps;
		for (OWLObject c1 : s1) {
			newMaps = checkSet(c1, s2, newMaps);
		}
		for (OWLObject c2 : s2) {
			newMaps = checkSet(s1, c2, newMaps);
		}
		return newMaps;
	}

	public Set<OWLObjectMap> checkSet(OWLObject c1, Set<? extends OWLObject> s2, Set<OWLObjectMap> origMaps) {
		Set<OWLObjectMap> newMaps = new HashSet<OWLObjectMap>();
		for (OWLObject c2 : s2) {
			newMaps.addAll(check(c1, c2, origMaps));
		}
		return newMaps;
	}

	public Set<OWLObjectMap> checkSet(Set<? extends OWLObject> s1, OWLObject c2, Set<OWLObjectMap> origMaps) {
		Set<OWLObjectMap> newMaps = new HashSet<OWLObjectMap>();
		for (OWLObject c1 : s1) {
			newMaps.addAll(check(c1, c2, origMaps));
		}
		return newMaps;
	}

	public Set<OWLObjectMap> check(OWLObject o1, OWLObject o2, Set<OWLObjectMap> origMaps) {
		if ((o1 instanceof OWLAxiom) && (o2 instanceof OWLAxiom)) {
			return check((OWLAxiom) o1, (OWLAxiom) o2, origMaps);
		}
		if ((o1 instanceof OWLClassExpression) && (o2 instanceof OWLClassExpression)) {
			return check((OWLClassExpression) o1, (OWLClassExpression) o2, origMaps);
		}
		if ((o1 instanceof OWLObjectPropertyExpression) && (o2 instanceof OWLObjectPropertyExpression)) {
			return check((OWLObjectPropertyExpression) o1, (OWLObjectPropertyExpression) o2, origMaps);
		}
		if ((o1 instanceof OWLIndividual) && (o2 instanceof OWLIndividual)) {
			return check((OWLIndividual) o1, (OWLIndividual) o2, origMaps);
		}
		throw new IllegalArgumentException("Cannot compare OWL objects of different types:\n" + o1 + "\n" + o2);
	}

	public Set<OWLObjectMap> check(OWLAxiom ax1, OWLAxiom ax2, Set<OWLObjectMap> origMaps) {
		if (ax1.getAxiomType().equals(ax2.getAxiomType())) {
			if (ax1 instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom sco1 = (OWLSubClassOfAxiom) ax1;
				OWLSubClassOfAxiom sco2 = (OWLSubClassOfAxiom) ax2;
				return check(sco1.getSuperClass(), sco2.getSuperClass(),
						check(sco1.getSubClass(), sco2.getSubClass(), origMaps));
			}
			if (ax1 instanceof OWLNaryClassAxiom) {
				return checkSet(((OWLNaryClassAxiom) ax1).getClassExpressions(),
						((OWLNaryClassAxiom) ax2).getClassExpressions(), origMaps);
			}
			if (ax1 instanceof OWLObjectPropertyDomainAxiom) {
				OWLObjectPropertyDomainAxiom d1 = (OWLObjectPropertyDomainAxiom) ax1;
				OWLObjectPropertyDomainAxiom d2 = (OWLObjectPropertyDomainAxiom) ax2;
				return check(d1.getDomain(), d2.getDomain(), check(d1.getProperty(), d2.getProperty(), origMaps));
			}
			if (ax1 instanceof OWLObjectPropertyRangeAxiom) {
				OWLObjectPropertyRangeAxiom r1 = (OWLObjectPropertyRangeAxiom) ax1;
				OWLObjectPropertyRangeAxiom r2 = (OWLObjectPropertyRangeAxiom) ax2;
				return check(r1.getRange(), r2.getRange(), check(r1.getProperty(), r2.getProperty(), origMaps));
			}
			if (ax1 instanceof OWLSubObjectPropertyOfAxiom) {
				OWLSubObjectPropertyOfAxiom sopo1 = (OWLSubObjectPropertyOfAxiom) ax1;
				OWLSubObjectPropertyOfAxiom sopo2 = (OWLSubObjectPropertyOfAxiom) ax2;
				return check(sopo1.getSuperProperty(), sopo2.getSuperProperty(),
						check(sopo1.getSubProperty(), sopo2.getSubProperty(), origMaps));
			}
			if (ax1 instanceof OWLSubPropertyChainOfAxiom) {
				OWLSubPropertyChainOfAxiom spco1 = (OWLSubPropertyChainOfAxiom) ax1;
				OWLSubPropertyChainOfAxiom spco2 = (OWLSubPropertyChainOfAxiom) ax2;
				return checkList(spco1.getPropertyChain(), spco2.getPropertyChain(),
						check(spco1.getSuperProperty(), spco2.getSuperProperty(), origMaps));
			}
			// TODO support more axioms
			throw new UnsupportedOperationException(
					"Unsupported axiom types:\n" + Utils.toString(ax1) + "\n" + Utils.toString(ax2));
		}
		return new HashSet<>();
	}

	public Set<OWLObjectMap> check(OWLClassExpression c1, OWLClassExpression c2, Set<OWLObjectMap> origMaps) {
		Set<OWLObjectMap> newMaps = new HashSet<OWLObjectMap>();

		if (allowToSimplify) {
			// allow to map concept names to complex concepts
			if ((c1 instanceof OWLClass) || (c2 instanceof OWLClass)) {
				newMaps.addAll(tryToAdd(c1, c2, origMaps));
			}
		}

		if ((c1 instanceof OWLObjectIntersectionOf) || (c2 instanceof OWLObjectIntersectionOf)) {
			// at least one side should be decomposed here
			newMaps.addAll(checkSet(c1.asConjunctSet(), c2.asConjunctSet(), origMaps));
			return newMaps;
		}
		if ((c1 instanceof OWLObjectUnionOf) || (c2 instanceof OWLObjectUnionOf)) {
			// at least one side should be decomposed here
			newMaps.addAll(checkSet(c1.asDisjunctSet(), c2.asDisjunctSet(), origMaps));
			return newMaps;
		}

		if (c1.getClassExpressionType().equals(c2.getClassExpressionType())) {
			if (c1 instanceof OWLClass) {
				if (Utils.areTopBottom(c1, c2)) {
					if (c1.equals(c2)) {
						newMaps.addAll(origMaps);
					}
				} else {
					if (!allowToSimplify) {
						// only allow to map concept names to concept names
						newMaps.addAll(tryToAdd(c1, c2, origMaps));
					}
				}
				return newMaps;
			}
			if (c1 instanceof OWLObjectComplementOf) {
				newMaps.addAll(check(((OWLObjectComplementOf) c1).getOperand(),
						((OWLObjectComplementOf) c2).getOperand(), origMaps));
				return newMaps;
			}
			if (c1 instanceof OWLQuantifiedObjectRestriction) {
				OWLQuantifiedObjectRestriction r1 = (OWLQuantifiedObjectRestriction) c1;
				OWLQuantifiedObjectRestriction r2 = (OWLQuantifiedObjectRestriction) c2;
				newMaps.addAll(
						check(r1.getFiller(), r2.getFiller(), check(r1.getProperty(), r2.getProperty(), origMaps)));
				return newMaps;
			}
			if (c1 instanceof OWLObjectOneOf) {
				newMaps.addAll(checkSet(((OWLObjectOneOf) c1).getIndividuals(),
						((OWLObjectOneOf) c2).getIndividuals(), origMaps));
				return newMaps;
			}
			if (c1 instanceof OWLObjectHasValue) {
				OWLObjectHasValue ohv1 = (OWLObjectHasValue) c1;
				OWLObjectHasValue ohv2 = (OWLObjectHasValue) c2;
				newMaps.addAll(check(ohv1.getFiller(), ohv2.getFiller(),
						check(ohv1.getProperty(), ohv2.getProperty(), origMaps)));
				return newMaps;
			}
			// TODO support more constructors
			throw new UnsupportedOperationException("Unsupported class expression types:\n" + Utils.toString(c1) + " ("
					+ c1.getClassExpressionType().getName() + ")\n" + Utils.toString(c2) + " ("
					+ c2.getClassExpressionType().getName() + ")");
		}

		return newMaps;
	}

	public Set<OWLObjectMap> check(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2,
			Set<OWLObjectMap> origMaps) {
		if ((p1 instanceof OWLObjectInverseOf) && (p2 instanceof OWLObjectInverseOf)) {
			return check(((OWLObjectInverseOf) p1).getInverse(), ((OWLObjectInverseOf) p2).getInverse(), origMaps);
		}
		if ((p1 instanceof OWLObjectProperty) && (p2 instanceof OWLObjectProperty)) {
			if (Utils.areTopBottom(p1, p2)) {
				if (p1.equals(p2)) {
					return origMaps;
				} else {
					return new HashSet<>();
				}
			}
			return tryToAdd((OWLObjectProperty) p1, (OWLObjectProperty) p2, origMaps);
		}
		return new HashSet<>();
	}

	public Set<OWLObjectMap> check(OWLIndividual i1, OWLIndividual i2, Set<OWLObjectMap> origMaps) {
		return tryToAdd(i1, i2, origMaps);
	}

	public Set<OWLObjectMap> tryToAdd(OWLObject o1, OWLObject o2, Set<OWLObjectMap> origMaps) {
		Set<OWLObjectMap> newMaps = new HashSet<>();
		for (OWLObjectMap m : origMaps) {
			if (!m.contains(o1)) {
				OWLObjectMap cm = new CompositeOWLObjectMap(m);
				if (cm.add(o1, o2)) {
					newMaps.add(cm);
				}
			} else if (m.apply(o1).equals(o2)) {
				newMaps.add(m);
			}
		}
		return newMaps;
	}

}
