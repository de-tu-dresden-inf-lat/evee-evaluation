package de.tu_dresden.inf.lat.evee.proofs.evaluation.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import de.tu_dresden.inf.lat.evee.proofs.data.exceptions.ProofGenerationFailedException;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.tools.MinimalHypergraphProofExtractor;
import de.tu_dresden.inf.lat.evee.proofs.tools.OWLSignatureBasedMinimalProofExtractor;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;
import de.tu_dresden.inf.lat.evee.proofs.tools.measures.TreeSizeMeasure;

public class ExtractUniqueProofs {

	private static String OUTPUT_SUBFOLDER = "graphs";

	private static String TREE_OUTPUT_SUBFOLDER = "trees";

	private final static String TIMES_FILE = "times.csv";

	private static boolean TREES = true;

	private static List<String> SIGNATURE = null;

	private static final int COMPUTATIONS = 5;

	private static final List<Entry<String, Double>> times = new LinkedList<>();

	public static void main(String[] args) {

		if ((args.length < 2) || (args.length > 3)) {
			System.out.println("Arguments: inputFolder TREES|GRAPHS [signatureFile]");
			return;
		}

		String inputFolder = args[0];
		TREES = args[1].equals("TREES");
		if (args.length == 3) {
			String signatureFile = args[2];
			TREE_OUTPUT_SUBFOLDER = "condensed_trees";
			OUTPUT_SUBFOLDER = "condensed_unique_not_supported_yet";
			try {
				SIGNATURE = Files.readAllLines(Paths.get(signatureFile));
			} catch (IOException e) {
				System.err.println("Could not open signature file " + signatureFile);
				e.printStackTrace();
				return;
			}
		}

		Path output = Paths.get(inputFolder).resolve(getOutputSubfolder());

		Utils.cleanFolder(output.toString());

		Utils.processProofs(inputFolder, ExtractUniqueProofs::processProof);

		File outputFile = output.resolve(TIMES_FILE).toFile();
		try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
			pw.println("Proof,Time (ms)");
			for (Entry<String, Double> e : times) {
				pw.println(e.getKey() + "," + e.getValue().toString());
			}
		} catch (IOException e) {
			System.err.println("Could not write output file " + outputFile);
			e.printStackTrace();
		}
	}

	private static String getOutputSubfolder() {
		if (TREES) {
			return TREE_OUTPUT_SUBFOLDER;
		} else {
			return OUTPUT_SUBFOLDER;
		}
	}

	private static void processProof(Path input) {
		System.out.println(input);
		Path output = input.resolveSibling(getOutputSubfolder()).resolve(input.getFileName());
		Utils.loadProofAndThenOrElse(input, proof -> {
			long totalTime = 0L;
			IProof<OWLAxiom> uniqueProof = null;
			for (int i = 0; i < COMPUTATIONS; i++) {
				long start = System.nanoTime();
				Set<OWLEntity> croppedSignature = null;
				if (SIGNATURE != null) {
					croppedSignature = ProofTools.getSignature(proof).stream()
							.filter(entity -> SIGNATURE.contains(entity.getIRI().toString()))
							.collect(Collectors.toSet());
				}
				try {
					uniqueProof = TREES ? new OWLSignatureBasedMinimalProofExtractor(new TreeSizeMeasure<>())
							.extract(proof, croppedSignature) : MinimalHypergraphProofExtractor.makeUnique(proof);
				} catch (ProofGenerationFailedException ex) {
					try {
						Files.createFile(output);
					} catch (IOException e) {
						System.err.println("Could not create file: " + output);
					}
				}
				totalTime += (System.nanoTime() - start);
			}
			double milliseconds = ((double) totalTime) / COMPUTATIONS / 1e6;
			times.add(new SimpleEntry<>(output.getFileName().toString(), milliseconds));
			Utils.writeProof(uniqueProof, output.toString().substring(0, output.toString().length() - 5));
		}, () -> {
			try {
				Files.createFile(output);
			} catch (IOException e) {
				System.err.println("Could not create file: " + output);
			}
		});
	}

}
