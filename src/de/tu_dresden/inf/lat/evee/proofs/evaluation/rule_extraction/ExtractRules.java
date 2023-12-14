/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLNaryClassAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import de.tu_dresden.inf.lat.evee.proofs.data.Inference;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.general.tools.ConceptNameGenerator;
import de.tu_dresden.inf.lat.evee.general.tools.IndividualNameGenerator;
import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;
import de.tu_dresden.inf.lat.evee.general.tools.RoleNameGenerator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C12AxiomPathsEvaluator;

/**
 * @author stefborg
 *
 */
public class ExtractRules {

	static InferenceCollection inferences = new InferenceCollection();

	public static void main(String[] args) {

		if (args.length != 3) {
			System.out.println("Arguments: inputFolder outputFolder EL/ALCH");
			return;
		}

		String inputFolder = args[0];
		String outputFolder = args[1];
		Utils.EL_MODE = args[2].equals("EL");

		inferences.load(outputFolder, "rule");
		System.out.println("Loaded inferences: " + inferences.size());

		Utils.processProofs(inputFolder, ExtractRules::processProof);
		System.out.println("Extracted inferences: " + inferences.size());

		inferences.filter(inf -> !ProofTools.isAsserted(inf));
		System.out.println("Non-asserted inferences: " + inferences.size());

		inferences.computeQuotient();
		System.out.println("Non-isomorphic non-asserted inferences: " + inferences.size());

		inferences.map(ExtractRules::anonymize);
		System.out.println("Inferences after anonymization: " + inferences.size());

		inferences.write(outputFolder, "rule");
	}

	private static void processProof(Path path) {
		Utils.loadProofAndThen(path, proof -> {
			for (IInference<OWLAxiom> inf : proof.getInferences()) {
				inferences.add(inf);
			}
		});
	}

	public static IInference<OWLAxiom> anonymize(IInference<OWLAxiom> inf) {
		List<List<OWLAxiom>> expressionPaths = new LinkedList<>(C12AxiomPathsEvaluator.getExpressionPaths(inf));
		expressionPaths.sort((l1, l2) -> -Integer.compare(l1.size(), l2.size()));
		List<OWLAxiom> orderedAxioms = new LinkedList<>();
		expressionPaths.forEach(path -> path.forEach(a -> {
			if (!orderedAxioms.contains(a))
				orderedAxioms.add(a);
		}));

		ConceptNameGenerator A = new ConceptNameGenerator("", true, null);
		RoleNameGenerator R = new RoleNameGenerator("", true, null);
		IndividualNameGenerator I = new IndividualNameGenerator("", true, null);
		Collection<OWLEntity> sig = new LinkedList<>();
		OWLObjectMap map = new OWLObjectMap();

		anonymizeAxiom(inf.getConclusion(), sig, A, R, I, map);
		for (OWLAxiom ax : orderedAxioms) {
			anonymizeAxiom(ax, sig, A, R, I, map);
		}
		IInference<OWLAxiom> result = new OWLAxiomReplacer(map)
				.visit(new Inference<>(inf.getConclusion(), "generic", orderedAxioms));

		if (!Utils.isCorrect(result)) {
			System.err.println("Invalid inference:\n" + inf);
//			throw new IllegalStateException("Inference is not valid.\n" + inf + "\n" + expressionPaths + "\n" + result);
		}

		return result;
	}

	private static void anonymizeAxiom(OWLAxiom ax, Collection<OWLEntity> sig, ConceptNameGenerator A,
			RoleNameGenerator R, IndividualNameGenerator I, OWLObjectMap map) {
		if (ax instanceof OWLNaryClassAxiom) {
			((OWLNaryClassAxiom) ax).getClassExpressions()
					.forEach(ex -> anonymizeEntities(ex.getSignature().stream(), sig, A, R, I, map));
		} else if (ax instanceof OWLSubClassOfAxiom) {
			anonymizeEntities(((OWLSubClassOfAxiom) ax).getSubClass().getSignature().stream(), sig, A, R, I, map);
			anonymizeEntities(((OWLSubClassOfAxiom) ax).getSuperClass().getSignature().stream(), sig, A, R, I, map);
		} else {
			anonymizeEntities(ax.getSignature().stream(), sig, A, R, I, map);
		}
	}

	private static void anonymizeEntities(Stream<OWLEntity> newEntities, Collection<OWLEntity> sig,
			ConceptNameGenerator A, RoleNameGenerator R, IndividualNameGenerator I, OWLObjectMap map) {
		newEntities.forEach(e -> {
			if (sig.contains(e)) {
				return;
			} else {
				sig.add(e);
			}
			if ((e instanceof OWLClass)) {
				if (!e.isBottomEntity() && !e.isTopEntity()) {
					map.add(e, A.next());
				}
				return;
			}
			if (e instanceof OWLObjectProperty) {
				if (!e.isBottomEntity() && !e.isTopEntity()) {
					map.add(e, R.next());
				}
				return;
			}
			if (e instanceof OWLIndividual) {
				map.add(e, I.next());
				return;
			}
			throw new IllegalStateException("Only OWLClasses, OWLObjectProperties, and OWLIndividuals are supported.");
		});
	}

	private static IInference<OWLAxiom> compress(IInference<OWLAxiom> inf) {
		inf = compress(inf, OWLObjectUnionOf.class, OWLTools.odf::getOWLObjectUnionOf, "U");
		inf = compress(inf, OWLObjectIntersectionOf.class, OWLTools.odf::getOWLObjectIntersectionOf, "I");
		return inf;
	}

	private static <T extends OWLNaryBooleanClassExpression> IInference<OWLAxiom> compress(IInference<OWLAxiom> inf,
			Class<T> c, Function<Set<OWLClass>, T> constructor, String prefix) {
		Set<Set<OWLClass>> sets = new OWLAxiomSetExtractor<>(c).visit(inf);
		boolean changed = true;
		while (changed) {
			changed = false;
			Entry<Set<OWLClass>, Set<OWLClass>> e = findOverlaps(sets);
			if (e != null) {
				Set<OWLClass> s1 = e.getKey();
				Set<OWLClass> s2 = e.getValue();
				sets.remove(s1);
				sets.remove(s2);

				Set<OWLClass> intersection = new HashSet<>(s1);
				intersection.retainAll(s2);
				s1.removeAll(intersection);
				s2.removeAll(intersection);
				sets.add(s1);
				sets.add(intersection);
				sets.add(s2);

				changed = true;
			}
		}

		OWLObjectMap map = new OWLObjectMap();
		ConceptNameGenerator A = new ConceptNameGenerator(prefix, false, null);

		for (Set<OWLClass> set : sets) {
			if (set.size() > 1) {
				map.addClasses(constructor.apply(set), A.next());
			}
		}

		return new OWLAxiomReplacer(map).visit(inf);
	}

	private static IInference<OWLAxiom> pullOutNegation(IInference<OWLAxiom> inf) {
		return new OWLAxiomNegationMerger().visit(inf);
	}

	private static Entry<Set<OWLClass>, Set<OWLClass>> findOverlaps(Set<Set<OWLClass>> sets) {
		for (Set<OWLClass> s1 : sets) {
			for (Set<OWLClass> s2 : sets) {
				if (!s1.equals(s2)) {
					if (s1.stream().anyMatch(s2::contains)) {
						return new SimpleEntry<>(s1, s2);
					}
				}
			}
		}
		return null;
	}

}
