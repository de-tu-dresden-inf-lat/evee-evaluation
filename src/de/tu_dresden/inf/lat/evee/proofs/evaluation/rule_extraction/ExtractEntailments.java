/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.HermiT.graph.Graph;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import de.tu_dresden.inf.lat.evee.proofs.data.Inference;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.general.tools.ConceptNameGenerator;
import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;
import de.tu_dresden.inf.lat.evee.general.tools.RoleNameGenerator;

/**
 * @author stefborg
 *
 */
public class ExtractEntailments {

	private static boolean useUnionOfJustifications;

	public static String outputFolder;

	private static final InferenceCollection inferences = new InferenceCollection();

	private static OWLOntology ontology;
	private static String ontologyName;
	private static OWLReasoner reasoner;
	private static Set<OWLClass> visited;
	private static final InferenceComparator comp = new InferenceComparator(false);
	private static Graph<OWLClass> graph;

	public static void main(String[] args) {

		Utils.EL_MODE = false;

		if (args.length != 6) {
			System.out.println("Arguments:");
			System.out.println("ontologyPath outputFolder exceptionsFile processedFile [EL|ALCH] [UNIONS|SINGLE]");
			System.out.println("The last argument specifies whether for each entailment, the set of justifications");
			System.out.println(" or the union of justifications should be used.");
			System.out.println("[EL|ALCH] specifies whether only ELH or only ALCH tasks should be extracted.");
			return;
		}

		String ontologyPath = args[0];
		outputFolder = args[1] + File.pathSeparator;
		String exceptionsFile = args[2];
		String processedFile = args[3];
		Utils.EL_MODE = args[4].equals("EL");
		useUnionOfJustifications = args[5].equals("UNIONS");

		Path p = Paths.get(ontologyPath);
		ontologyName = p.getFileName().toString();
		System.out.println("##############  " + ontologyName);

		try {
			System.out.println("Ontology file size (bytes): " + Files.size(p));
			List<String> exceptions = Files.readAllLines(Paths.get(exceptionsFile));
			List<String> processed = Files.readAllLines(Paths.get(processedFile));
			if (exceptions.contains(ontologyName)) {
				System.out.println("Ontology is listed as an exception.");
				return;
			}
			if (processed.contains(ontologyName)) {
				System.out.println("Ontology has already been processed.");
				return;
			}
			inferences.load(outputFolder, "task");
			System.out.println(inferences.size() + " stored inferences loaded.");

			addInferences(p);

			inferences.map(ExtractRules::anonymize);

			// need to add them because we skip those in real ontologies (it is too
			// expensive to compute justifications each time)
			if (Utils.EL_MODE) {
				System.out.println("Adding simple inferences.");
				addSimpleTransitivityInferences();
				System.out.println(inferences.size() + " inferences collected so far.");
			}

		} catch (IOException e) {
			System.err.println("Could not read files: " + ontologyPath + " " + exceptionsFile + " " + processedFile);
			e.printStackTrace();
			return;
		} catch (Throwable e) {
			e.printStackTrace();
			try {
				Files.write(Paths.get(exceptionsFile),
						(ontologyName + "\n" + e + "\n"
								+ Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString)
										.collect(Collectors.joining(","))
								+ "\n").getBytes(),
						StandardOpenOption.APPEND);
			} catch (IOException e2) {
				System.out.println("Could not write to file: " + exceptionsFile);
				e.printStackTrace();
			}
			System.err.println("An error occurred. Stopping extraction of inferences ...");
			return;
		}

		inferences.write(outputFolder, "task");

		try {
			Files.write(Paths.get(processedFile), (ontologyName + "\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.out.println("Could not write to file: " + processedFile);
			e.printStackTrace();
		}
	}

	private static void addSimpleTransitivityInferences() {
		List<OWLClass> classes = new LinkedList<>();
		ConceptNameGenerator A = new ConceptNameGenerator("", true, null);
		RoleNameGenerator R = new RoleNameGenerator("", true, null);
		for (int i = 0; i < 8; i++) {
			classes.add(A.next());
		}
		List<OWLAxiom> axioms = new LinkedList<>();
		List<OWLAxiom> altAxioms = new LinkedList<>();
		for (int i = 0; i < 7; i++) {
			axioms.add(OWLTools.odf.getOWLSubClassOfAxiom(classes.get(i), classes.get(i + 1)));
			altAxioms.add(OWLTools.odf.getOWLEquivalentClassesAxiom(classes.get(i), OWLTools.odf.getOWLObjectIntersectionOf(
					classes.get(i + 1), OWLTools.odf.getOWLObjectSomeValuesFrom(R.next(), A.next()))));
		}
		for (int i = 1; i < 7; i++) {
			OWLAxiom conclusion = OWLTools.odf.getOWLSubClassOfAxiom(classes.get(0), classes.get(i));
			addModuloIsomorphisms(new Inference<>(conclusion, "generic", axioms.subList(0, i)), 0L);
			addModuloIsomorphisms(new Inference<>(conclusion, "generic", altAxioms.subList(0, i)), 0L);
		}
	}

	private static void addInferences(Path p) {
		ontology = Utils.loadOntology(p);
		if (ontology == null) {
			return;
		}
		Utils.filterOntology(ontology);

		if (Utils.EL_MODE || !ontology.getLogicalAxioms().stream().allMatch(Utils::isELHAxiom)) {
			buildGraph();
			reasoner = Utils.createReasoner(ontology);
			if (!reasoner.isConsistent()) {
				System.out.println("Ontology is inconsistent. No inferences can be extracted.");
				return;
			}
			visited = new HashSet<>();
			System.out.println("Computing inferences");
			addAxioms(reasoner.getTopClassNode());
			System.out.println(inferences.size() + " inferences collected so far.");
			reasoner.dispose();
		} else {
			System.out.println("Restriction of the ontology to ALCH is already in ELH.");
		}

		OWLTools.manager.removeOntology(ontology);
	}

	private static void addAxioms(Node<OWLClass> n) {
		n.forEach(c -> visited.add(c));
		Set<Node<OWLClass>> subs = reasoner.getSubClasses(n.getRepresentativeElement(), true).getNodes();
//		System.out.print(subs.size());
		for (OWLClass c1 : n) {
			for (OWLClass c2 : n) {
				if (!c1.equals(c2)) {
					addAxiom(OWLTools.odf.getOWLEquivalentClassesAxiom(c1, c2));
				}
			}
			for (Node<OWLClass> sub : subs) {
				for (OWLClass c2 : sub) {
					if (!reachable(c2, c1)) {
						addAxiom(OWLTools.odf.getOWLSubClassOfAxiom(c2, c1));
					}
				}
			}
		}
//		System.out.println(",");
		for (Node<OWLClass> sub : subs) {
			if (!visited.contains(sub.getRepresentativeElement())) {
				addAxioms(sub);
			}
		}
	}

	private static void addAxiom(OWLAxiom ax) {
//		System.out.print(".");

		System.out.println(ontologyName);
		System.out.println(ax);

		if(useUnionOfJustifications) {
			Optional<Set<? extends OWLAxiom>> axiomsOptional =
			Utils.computeUnionofJustifications(reasoner,ax);
			if(!axiomsOptional.isPresent())
				System.out.println("Nothing found for selected DL.");
			else
				processInference(axiomsOptional.get(),ax);
		} else {
			Set<? extends Set<? extends OWLAxiom>> just = Utils.computeJustifications(reasoner, ax);
			System.out.println(just.size() + " justifications generated.");

			int k = 0;
			for (Set<? extends OWLAxiom> j : just) {
				k++;
				System.out.print("." + k);
				processInference(j,ax);
			}
		}

		System.out.println();
		System.out.println(inferences.size() + " inferences collected so far.");
	}

	private static void processInference(Set<? extends OWLAxiom> axioms, OWLAxiom conclusion){
		IInference<OWLAxiom> inf = new Inference<>(conclusion, "generic", new LinkedList<>(axioms));
		try {
			addModuloIsomorphisms(inf, 1L);
		} catch (RuntimeException e) {
			throw new RuntimeException(axioms.toString(), e);
		}
	}

	private static void addModuloIsomorphisms(IInference<OWLAxiom> inf, Long num) {
		if (Utils.EL_MODE || !Utils.isELHInference(inf)) {
			inferences.addModuloIsomorphisms(inf, num, comp);
		}
	}

	private static void buildGraph() {
		graph = new Graph<>();
		ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES).forEach(ExtractEntailments::addEdges);
		ontology.getAxioms(AxiomType.SUBCLASS_OF).forEach(ExtractEntailments::addEdges);
		ontology.getClassesInSignature().forEach(c -> addEdge(c, OWLTools.odf.getOWLThing()));
		ontology.getClassesInSignature().forEach(c -> addEdge(OWLTools.odf.getOWLNothing(), c));
	}

	private static void addEdges(OWLEquivalentClassesAxiom ax) {
		ax.getClassExpressions().stream().filter(ex -> !ex.isAnonymous()).forEach(c -> ax.getClassExpressions().stream()
				.filter(ex -> !ex.equals(c)).forEach(ex -> addEdge(c.asOWLClass(), ex)));
	}

	private static void addEdges(OWLSubClassOfAxiom ax) {
		if (!ax.getSubClass().isAnonymous()) {
			addEdge(ax.getSubClass().asOWLClass(), ax.getSuperClass());
		}
	}

	private static void addEdge(OWLClass c, OWLClassExpression ex) {
		ex.asConjunctSet().stream().filter(subex -> !subex.isAnonymous())
				.forEach(subex -> addEdge(c, subex.asOWLClass()));
	}

	private static void addEdge(OWLClass c1, OWLClass c2) {
		graph.addEdge(c1, c2);
	}

	private static boolean reachable(OWLClass from, OWLClass to) {
		return graph.getReachableSuccessors(from).contains(to);
	}

}
