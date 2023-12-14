/**
 * 
 */
package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.data.exceptions.ProofException;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.ClassExpressionTypesEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.DifferentClassesEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.MaxAxiomSizeEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.NestingDepthEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.SignatureEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.SizeEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.SubClassesEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.inferences.SuperClassesEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.RecursiveProofEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.AggregateProofEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.CorrectnessEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.HypergraphSizeEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.JustificationSizeEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.RedundancyEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C10ClassConstructorDiffEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C11LaconicGCICountEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C12AxiomPathsEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C1AxiomTypesEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C2ClassConstructorsEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C3UniversalImplicationEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C4SynonymOfThingEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C5SynonymOfNothingEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C6DomainAndNoExistentialEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C8SignatureDifferenceEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.C9AxiomTypeDiffEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.evaluators.inferences.JustificationComplexityEvaluator;
import de.tu_dresden.inf.lat.evee.proofs.tools.measures.DepthMeasure;
import de.tu_dresden.inf.lat.evee.proofs.tools.measures.TreeSizeMeasure;

/**
 * @author stefborg
 *
 */
public class Stats {

	private static String inputFolder;

	private static String tasksFolder;

	private static final String LOG_SUFFIX = ".output";

//	public static boolean WEIGHT_PROOFS = false;

	private static final CorrectnessEvaluator correctnessEvaluator = new CorrectnessEvaluator();
	private static final SignatureCoverageEvaluator signatureCoverageEvaluator = new SignatureCoverageEvaluator();
	private static final RuntimeEvaluator<OWLAxiom> runtimeEvaluator = new RuntimeEvaluator<>();
	private static final TaskSignatureSizeEvaluator signatureEvaluator = new TaskSignatureSizeEvaluator();

	private static final List<IProofEvaluator<OWLAxiom>> proofEvaluators = Arrays.asList(
			new RecursiveProofEvaluator<>(new TreeSizeMeasure<>()),
			new HypergraphSizeEvaluator<>(), new InferenceStepsEvaluator<>(),
			new RecursiveProofEvaluator<>(new DepthMeasure<>()), new RedundancyEvaluator<>(),
			correctnessEvaluator, new AggregateProofEvaluator<>(new JustificationComplexityEvaluator()),
			new AggregateProofEvaluator<>(new JustificationComplexityEvaluator(), DoubleSummaryStatistics::getMax,
					"max"),
			new AggregateProofEvaluator<>(new JustificationComplexityEvaluator(), DoubleSummaryStatistics::getAverage,
					"avg"),
			new JustificationSizeEvaluator<>(), new JustificationComplexityProofEvaluator(),
			signatureCoverageEvaluator, runtimeEvaluator, new GenerationTimeEvaluator<>(runtimeEvaluator),
			new MinimizationTimeEvaluator<>(runtimeEvaluator), signatureEvaluator);

	private static boolean rules = true;

	// TODO: more evaluators: custom weighted sum
	public static List<IProofEvaluator<OWLAxiom>> ruleEvaluators = Stream.of(
			// new NotELHEvaluator(), // correlated with ClassExpressionTypesEvaluators
			new SizeEvaluator(), // not correlated with the others?
			new MaxAxiomSizeEvaluator(), // weak correlation with total size
			new SignatureEvaluator(), //
			new NestingDepthEvaluator(), // weak correlation with size
			new ClassExpressionTypesEvaluator(ClassExpressionType.OBJECT_COMPLEMENT_OF),
			new ClassExpressionTypesEvaluator(ClassExpressionType.OBJECT_UNION_OF,
					ClassExpressionType.OBJECT_INTERSECTION_OF),
			new ClassExpressionTypesEvaluator(ClassExpressionType.OBJECT_SOME_VALUES_FROM,
					ClassExpressionType.OBJECT_ALL_VALUES_FROM),
			new SubClassesEvaluator(), // not correlated?
			new SuperClassesEvaluator(), // not correlated?
			new DifferentClassesEvaluator(), // slightly correlated with the previous two
			new C1AxiomTypesEvaluator(), // ?
			new C2ClassConstructorsEvaluator(), // correlated with NotELHEvaluator
			new C3UniversalImplicationEvaluator(), // irrelevant for ELH
			new C4SynonymOfThingEvaluator(), // ?
			new C5SynonymOfNothingEvaluator(), // ?
			new C6DomainAndNoExistentialEvaluator(), // ?
			// new C7ModalDepthEvaluator(), // correlation with nesting depth
			new C8SignatureDifferenceEvaluator(), // ?
			new C9AxiomTypeDiffEvaluator(), // ?
			new C10ClassConstructorDiffEvaluator(), // ?
			new C11LaconicGCICountEvaluator(), // weak correlation with size
			new C12AxiomPathsEvaluator() // ?
	).map(AggregateProofEvaluator::new).collect(Collectors.toList());

	private static List<IProofEvaluator<OWLAxiom>> evaluators = proofEvaluators;

	private static final List<Entry<String, List<Double>>> stats = new ArrayList<>();

	private static int count = 0;

	public static void main(String[] args) {

		if ((args.length < 3) || (args.length > 5)) {
			System.out.println("Expected 3-5 arguments: inputFolder EL|ALCH RULES|PROOFS [taskFolder] [signatureFile]");
			return;
		}

		inputFolder = args[0];
		Utils.EL_MODE = args[1].equals("EL");
		rules = args[2].equals("RULES");
		if (args.length >= 4) {
			tasksFolder = args[3];
		}
		if (args.length == 5) {
			String signatureFile = args[4];
			try {
				List<String> signature = Files.readAllLines(Paths.get(signatureFile));
				signatureCoverageEvaluator.setSignature(signature);
			} catch (IOException e) {
				System.err.println("Could not open signature file " + signatureFile);
				e.printStackTrace();
				return;
			}
		}

		count = 0;

		evaluators = rules ? ruleEvaluators : proofEvaluators;

		if ((!rules) && (tasksFolder == null)) {
			tasksFolder = findTasksFolder();
			if (tasksFolder == null) {
				return;
			}
		}

		String outputFile = inputFolder + File.separator + "stats.csv";

		Utils.processProofs(inputFolder, Stats::processProof);

		aggregateStats();

		writeStatsFile(outputFile);
	}

	private static String findTasksFolder() {
		String folderName = Utils.EL_MODE ? "el-tasks" : "alc-tasks";
		Path p = Paths.get(inputFolder).resolveSibling(folderName);
		if (Files.exists(p)) {
			return p.toString();
		}
		p = p.getParent().resolveSibling(folderName);
		if (Files.exists(p)) {
			return p.toString();
		}
		new IOException("Could not find folder '" + folderName + "' close to input folder!").printStackTrace();
		return null;
	}

	private static void aggregateStats() {
		int proofs = stats.size();
		List<Double> listOfZeros = listOf(0d);

		List<Double> sums = stats.stream().map(Entry::getValue).reduce(listOfZeros, (l1, l2) -> {
			List<Double> sum = new ArrayList<>();
			for (int i = 0; i < l1.size(); i++) {
				sum.add(l1.get(i) + l2.get(i));
			}
			return sum;
		});
		stats.add(new SimpleEntry<>("Sum", sums));
		printLastEntry();

		List<Double> averages = sums.stream().map(s -> s / proofs).collect(Collectors.toList());
		stats.add(new SimpleEntry<>("Average", averages));
		printLastEntry();
	}

	private static void writeStatsFile(String outputFile) {
		try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
			pw.println(
					"Proof," + evaluators.stream().map(IProofEvaluator::getDescription).collect(Collectors.joining(",")));
			for (Entry<String, List<Double>> e : stats) {
				pw.println(e.getKey() + ","
						+ e.getValue().stream().map(Object::toString).collect(Collectors.joining(",")));
			}
		} catch (IOException e) {
			System.err.println("Could not write output file " + outputFile);
			e.printStackTrace();
		}
	}

	private static void processProof(Path path) {
		count++;
		String name = path.getFileName().toString();
		IProof<OWLAxiom> proof;
		List<Double> data;
		try {
			if (!rules) {
				String taskName = name.startsWith("proof") ? name.replace("proof", "task") : name;
				String taskFile = tasksFolder + File.separator + taskName;
				IProof<OWLAxiom> task = Utils.loadProof(Paths.get(taskFile));
				if (task == null) {
					throw new IllegalStateException("Could not load task: " + taskName);
				}
				IInference<OWLAxiom> currentTask = task.getInferences().get(0);
				correctnessEvaluator.setTask(currentTask);
				signatureEvaluator.setTask(currentTask);
			}
			proof = Utils.loadProof(path);
			Path logFile = path.resolveSibling(name + LOG_SUFFIX);
			List<String> currentLog;
			if (Files.exists(logFile)) {
				currentLog = Files.readAllLines(logFile);
			} else {
				currentLog = Collections.emptyList();
			}
			runtimeEvaluator.setLog(currentLog);
		} catch (Throwable e) {
			// error parsing proof
			e.printStackTrace();
			data = listOf(-1d);
			stats.add(new SimpleEntry<>(name, data));
			printLastEntry();
			return;
		}
		if (proof == null) {
			// file was empty
			data = listOf(-1d);
			data.set(12, runtimeEvaluator.evaluate((IProof<OWLAxiom>) null));
			data.set(15, signatureEvaluator.evaluate((IProof<OWLAxiom>) null));
		} else {


			data = evaluators.stream().map(eval -> {
				try {
					return eval.evaluate(proof);
				} catch (ProofException ex) {
					ex.printStackTrace();
					return Double.MAX_VALUE;
				}
			}).collect(Collectors.toList());
		}
		stats.add(new SimpleEntry<>(name, data));
		printLastEntry();
	}

	protected static List<Double> listOf(Double d) {
		return evaluators.stream().map(e -> d).collect(Collectors.toList());
	}

	private static void printLastEntry() {
		Entry<String, List<Double>> e = stats.get(stats.size() - 1);
		System.out.println(inputFolder);
		System.out.println(count + ": " + e.getKey());
		System.out.println(e.getValue());
	}

}
