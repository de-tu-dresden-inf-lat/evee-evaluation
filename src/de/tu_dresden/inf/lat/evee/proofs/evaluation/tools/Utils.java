/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.liveontologies.puli.DynamicProof;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.InferenceJustifiers;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetCollector;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerators;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.proofs.ElkOwlInference;
import org.semanticweb.elk.owlapi.proofs.ElkOwlProof;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException;

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import com.clarkparsia.owlapi.explanation.util.SilentExplanationProgressMonitor;

import de.tu_dresden.inf.lat.evee.proofs.data.Proof;
import de.tu_dresden.inf.lat.evee.general.data.exceptions.ParsingException;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.json.JsonProofParser;
import de.tu_dresden.inf.lat.evee.proofs.json.JsonProofWriter;
import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;
import de.tu_dresden.inf.lat.prettyPrinting.formatting.ParsableOWLFormatter;

/**
 * @author stefborg
 *
 */
public class Utils {

	private Utils() {
		// utilities class.
	}

	public static boolean EL_MODE = true;

	private final static ParsableOWLFormatter formatter = new ParsableOWLFormatter();
	private final static ReasonerFactory ALCHReasonerFactory = new ReasonerFactory();
	private final static ElkReasonerFactory ELHReasonerFactory = new ElkReasonerFactory();

	// no ABox!
	private final static List<AxiomType> ELHSupportedAxioms = Arrays.asList(
			new AxiomType[] { AxiomType.EQUIVALENT_CLASSES, AxiomType.SUBCLASS_OF, AxiomType.OBJECT_PROPERTY_DOMAIN,
					AxiomType.DISJOINT_CLASSES, AxiomType.SUB_OBJECT_PROPERTY, AxiomType.DECLARATION });
	private final static List<ClassExpressionType> ELHSupportedConcepts = Arrays
			.asList(ClassExpressionType.OWL_CLASS,
					ClassExpressionType.OBJECT_INTERSECTION_OF, ClassExpressionType.OBJECT_SOME_VALUES_FROM);

	private final static List<AxiomType> ALCHSupportedAxioms = Arrays
			.asList(AxiomType.EQUIVALENT_CLASSES, AxiomType.SUBCLASS_OF,
					AxiomType.OBJECT_PROPERTY_DOMAIN, AxiomType.OBJECT_PROPERTY_RANGE, AxiomType.DISJOINT_CLASSES,
					AxiomType.DISJOINT_UNION, AxiomType.SUB_OBJECT_PROPERTY, AxiomType.DECLARATION);
	private final static List<ClassExpressionType> ALCHSupportedConcepts = Arrays.asList(
			ClassExpressionType.OWL_CLASS, ClassExpressionType.OBJECT_INTERSECTION_OF,
					ClassExpressionType.OBJECT_UNION_OF, ClassExpressionType.OBJECT_COMPLEMENT_OF,
					ClassExpressionType.OBJECT_SOME_VALUES_FROM, ClassExpressionType.OBJECT_ALL_VALUES_FROM);

	private final static List<AxiomType> ALCSupportedAxioms = Arrays.asList(
			AxiomType.EQUIVALENT_CLASSES, AxiomType.SUBCLASS_OF, AxiomType.OBJECT_PROPERTY_DOMAIN,
					AxiomType.OBJECT_PROPERTY_RANGE, AxiomType.DISJOINT_CLASSES, AxiomType.DISJOINT_UNION);
	private final static List<ClassExpressionType> ALCSupportedConcepts = Arrays.asList(
			ClassExpressionType.OWL_CLASS, ClassExpressionType.OBJECT_INTERSECTION_OF,
					ClassExpressionType.OBJECT_UNION_OF, ClassExpressionType.OBJECT_COMPLEMENT_OF,
					ClassExpressionType.OBJECT_SOME_VALUES_FROM, ClassExpressionType.OBJECT_ALL_VALUES_FROM);

	private static List<ClassExpressionType> getSupportedConcepts() {
		return EL_MODE ? ELHSupportedConcepts : ALCHSupportedConcepts;
	}

	private static List<AxiomType> getSupportedAxioms() {
		return EL_MODE ? ELHSupportedAxioms : ALCHSupportedAxioms;
	}

	public static void filterOntology(OWLOntology ontology) {
		if (ontology
				.getOWLOntologyManager().removeAxioms(ontology, ontology.getLogicalAxioms().stream()
						.filter(ax -> !isAxiomSupported(ax)).collect(Collectors.toSet()))
				.equals(ChangeApplied.UNSUCCESSFULLY)) {
			throw new RuntimeException("Could not filter out unsupported axiom and concept types.");
		}
	}

	private static boolean isAxiomSupported(OWLAxiom ax) {
		return isAxiomSupported(ax, getSupportedAxioms(), getSupportedConcepts());
	}

	private static boolean isAxiomSupported(OWLAxiom ax, List<AxiomType> supportedAxioms,
			List<ClassExpressionType> supportedConcepts) {
		if (!supportedAxioms.contains(ax.getAxiomType())) {
			return false;
		}
		if (ax.getNestedClassExpressions().stream()
				.anyMatch(c -> !supportedConcepts.contains(c.getClassExpressionType()))) {
			return false;
		}
		return true;
	}

	public static boolean isELHAxiom(OWLAxiom axiom) {
		return isAxiomSupported(axiom, ELHSupportedAxioms, ELHSupportedConcepts);
	}

	public static boolean isELHInference(IInference<OWLAxiom> inf) {
		return ProofTools.getSentences(inf).stream()
				.allMatch(ax -> isAxiomSupported(ax, ELHSupportedAxioms, ELHSupportedConcepts));
	}

	public static boolean isALCInference(IInference<OWLAxiom> inf) {
		return ProofTools.getSentences(inf).stream()
				.allMatch(ax -> isAxiomSupported(ax, ALCSupportedAxioms, ALCSupportedConcepts));
	}

	public static IProof<OWLAxiom> loadProof(Path path) throws IOException {
		if (Files.size(path) == 0) {
			System.err.println("File is empty: " + path);
			return null;
		}
		String proofString = String.join("\n", Files.readAllLines(path));
		if (proofString.contains("Timeout")) {
			System.err.println("Timeout: " + path);
			return null;
		}
		try {
			return JsonProofParser.getInstance().parseProof(proofString);
		} catch (ParsingException e) {
			throw new IOException("Could not parse proof:\n" + proofString, e);
		}
	}

	public static void loadProofAndThen(Path path, Consumer<IProof<OWLAxiom>> andThen) {
		loadProofAndThenOrElse(path, andThen, () -> {
		});
	}

	public static void loadProofAndThenOrElse(Path path, Consumer<IProof<OWLAxiom>> andThen, Runnable orElse) {
		IProof<OWLAxiom> proof;
		try {
			proof = Utils.loadProof(path);
		} catch (IOException e) {
			System.err.println("Failed to load proof: " + path);
			e.printStackTrace();
			return;
		}
		if (proof != null) {
			andThen.accept(proof);
		} else {
			orElse.run();
		}
	}

	public static void processProofs(String inputFolder, Consumer<Path> proc) {
		Path path = Paths.get(inputFolder);
		try (Stream<Path> walk = Files.list(path)) {

			walk.filter(Files::isRegularFile).filter(f -> !f.getFileName().toString().startsWith("."))
					.filter(f -> f.getFileName().toString().endsWith(".json"))
					.sorted(Comparator.comparing(f -> f.getFileName().toString())).forEachOrdered(proc);

		} catch (IOException e) {
			System.err.println("Could not read .json files from " + inputFolder);
			e.printStackTrace();
		}
	}

	public static void writeLines(byte[] lines, String filename) {
		try {
			Files.write(Paths.get(filename), lines);
		} catch (IOException e) {
			System.err.println("Could not write to " + filename);
			e.printStackTrace();
		}
	}

	public static void writeProof(IInference<OWLAxiom> inf, String filename) {
		writeProof(singletonProof(inf), filename);
	}

	public static void writeProof(IProof<OWLAxiom> proof, String filename) {
		try {
			JsonProofWriter.<OWLAxiom>getInstance().writeToFile(proof, filename);
		} catch (IOException e) {
			System.err.println("Could not write proof to " + filename);
			e.printStackTrace();
		}
	}

	public static void cleanFolder(String folder) {
		Path p = Paths.get(folder);
		if (!Files.exists(p)) {
			try {
				Files.createDirectory(p);
			} catch (IOException e) {
				System.err.println("Could not create directory " + p);
				e.printStackTrace();
			}
		}
		try (Stream<Path> walk = Files.walk(p).filter(Files::isRegularFile)) {
			for (Path f : (Iterable<Path>) walk::iterator) {
				try {
					Files.delete(f);
				} catch (IOException e) {
					System.err.println("Could not delete file: " + f);
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.err.println("Could not access output folder: " + folder);
		}
	}

	public static boolean areTopBottom(OWLObject o1, OWLObject o2) {
		return o1.isTopEntity() || o1.isBottomEntity() || o2.isTopEntity() || o2.isBottomEntity();
	}

	public static IProof<OWLAxiom> singletonProof(IInference<OWLAxiom> inf) {
		return new Proof<>(inf.getConclusion(), Collections.singletonList(inf));
	}

	public static String toString(OWLClassExpression c) {
		return formatter.converter().convert(c).toString();
	}

	public static String toString(OWLAxiom ax) { return formatter.format(ax); }

	public static String toString(OWLObjectPropertyExpression p) {
		return formatter.converter().convert(p).toString();
	}

	public static OWLReasoner createReasoner(OWLOntology o) {
		if (EL_MODE) {
			return ELHReasonerFactory.createReasoner(o);
		} else {
			return ALCHReasonerFactory.createReasoner(o);
		}
	}

	public static OWLOntology loadOntology(Path p) {
		try {
			return OWLTools.manager.loadOntologyFromOntologyDocument(p.toFile());
		} catch (OWLOntologyCreationException e) {
			System.err.println("Could not load ontology:" + p);
			e.printStackTrace();
		}
		return null;
	}

	public static Stream<?> getAxiomTypes(Stream<? extends OWLAxiom> axioms) {
		return axioms.map(OWLAxiom::getAxiomType).distinct();
	}

	public static boolean entails(Stream<? extends OWLAxiom> axioms, OWLAxiom conclusion) {
		OWLOntology ontology = OWLTools.createOntology(axioms);
		boolean supported = ontology.getLogicalAxioms().stream().allMatch(Utils::isAxiomSupported)
				&& isAxiomSupported(conclusion);
		OWLReasoner reasoner = supported ? Utils.createReasoner(ontology)
				: ALCHReasonerFactory.createReasoner(ontology);
		boolean result;
		try {
			result = reasoner.isEntailed(conclusion);
		} catch (UnsupportedEntailmentTypeException e) {
			if (!EL_MODE) {
				throw new RuntimeException(e);
			}
			OWLReasoner ALCHReasoner = ALCHReasonerFactory.createReasoner(ontology);
			result = ALCHReasoner.isEntailed(conclusion);
			ALCHReasoner.dispose();
		}
		reasoner.dispose();
		OWLTools.manager.removeOntology(ontology);
		return result;
	}

	public static boolean isCorrect(IInference<OWLAxiom> inf) {
		return entails(inf.getPremises().stream(), inf.getConclusion());
	}

	public static Set<? extends Set<? extends OWLAxiom>> computeJustifications(OWLReasoner reasoner,
			OWLAxiom conclusion) {
		if (EL_MODE) {
			try {
				return computeELHJustifications(reasoner, conclusion);
			} catch (UnsupportedEntailmentTypeException ex) {
				return Collections.singleton(reasoner.getRootOntology().getAxioms());
			}
		} else {
			return computeALCHJustifications(reasoner, conclusion);
		}
	}

	public static Optional<Set<? extends OWLAxiom>> computeUnionofJustifications(OWLReasoner reasoner,
			OWLAxiom conclusion) {
		if (EL_MODE) {
			Set<? extends Set<? extends OWLAxiom>> just = Utils.computeJustifications(reasoner, conclusion);
			System.out.println(just.size() + " justifications generated.");
			Set<OWLAxiom> union = new HashSet<>();
			just.forEach(set -> {
				if (set.stream().allMatch(Utils::isELHAxiom))
					// if in EL mode, we should only add EL justifications
					union.addAll(set);
			});
			if (union.isEmpty())
				return Optional.empty();
			else
				return Optional.of(union);
		} else {

			/*
			 * OWLOntology ontology = reasoner.getRootOntology();
			 * SyntacticLocalityModuleExtractor moduleExtractor = new
			 * SyntacticLocalityModuleExtractor(ontology.getOWLOntologyManager(), ontology,
			 * ModuleType.STAR);
			 * 
			 * Set<OWLAxiom> module = moduleExtractor.extract(conclusion.getSignature());
			 * 
			 * if(module.stream().allMatch(Utils::isELHAxiom)) {
			 * System.out.println("module is purely ELH"); return Optional.empty(); // there
			 * won't be any justification not in ELH } else { OWLAxiom no =
			 * module.stream().filter(x -> !Utils.isELHAxiom(x)).findFirst().get();
			 * System.out.println("module contains: "+no); }
			 */
			// <-- optimiziation to check the module before computing any justifications. An
			// initial test with two
			// ontologies were justification extraction took longer showed no effect, as
			// modules always contained ALC
			// even if the respective justifications don't.

			Set<? extends Set<? extends OWLAxiom>> just = Utils.computeJustifications(reasoner, conclusion);
			System.out.println(just.size() + " justifications generated.");
			if (just.isEmpty())
				return Optional.empty();
			Set<OWLAxiom> union = new HashSet<>();
			just.forEach(union::addAll);
			if (union.stream().allMatch(Utils::isELHAxiom))
				return Optional.empty();
			else
				return Optional.of(union);
		}
	}

	protected static Set<? extends Set<? extends OWLAxiom>> computeELHJustifications(OWLReasoner reasoner,
			OWLAxiom conclusion) {
		DynamicProof<ElkOwlInference> proof = ElkOwlProof.create((ElkReasoner) reasoner, conclusion);
		final InferenceJustifier<org.liveontologies.puli.Inference<OWLAxiom>, ? extends Set<? extends OWLAxiom>> justifier = InferenceJustifiers
				.justifyAssertedInferences();
		final Set<Set<? extends OWLAxiom>> just = new HashSet<>();
		MinimalSubsetEnumerators.enumerateJustifications(conclusion, proof, justifier, InterruptMonitor.DUMMY,
				new MinimalSubsetCollector<>(just));
		return just;
	}

	protected static Set<Set<OWLAxiom>> computeALCHJustifications(OWLReasoner reasoner, OWLAxiom conclusion) {
		DefaultExplanationGenerator explainer = new DefaultExplanationGenerator(OWLTools.manager,
				Utils.ALCHReasonerFactory, reasoner.getRootOntology(), reasoner,
				new SilentExplanationProgressMonitor());
		return explainer.getExplanations(conclusion);
	}

}