package de.tu_dresden.inf.lat.evee.proofs.evaluation.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.proofs.json.JsonProofParser;

public class FilterProofsBySignatures {

	private static String SNOMED_PREFIX = "SnomedCT.owl-";

	private static String PROOFS_PREFIX = "SNOMED_tasks_";

	private static String SIGNATURE_PREFIX = "SNOMED_sig_";

	private static String LISTS_SUFFIX = ".txt";

	private static int COUNT = 0;

	public static void main(String[] args) {

		if (args.length != 4) {
			System.out.println(
					"Expected arguments: TASKS_FOLDER TASK_LISTS_FOLDER SIGNATURES_FOLDER FILTERED_TASKS_FOLDER");
			return;
		}

		String proofsFolder = args[0];
		String proofListsFolder = args[1];
		String signaturesFolder = args[2];
		String outputFolder = args[3];

		Map<String, List<String>> signatures = new HashMap<>();
		try {
			for (File signatureFile : new File(signaturesFolder).listFiles()) {
				String fileName = signatureFile.getName();
				String signatureId = fileName.substring(SIGNATURE_PREFIX.length(),
						fileName.length() - LISTS_SUFFIX.length());
				List<String> signature = Files.readAllLines(signatureFile.toPath());
				signatures.put(signatureId, signature);
				System.out.println("Read signature file " + signatureId + " (" + signature.size() + " IRIs)");
			}
		} catch (IOException e) {
			System.err.println("Could not open signature files: " + signaturesFolder);
			e.printStackTrace();
			return;
		}

		Map<String, PrintWriter> outputFiles = new HashMap<>();
		try {

			for (String signatureId : signatures.keySet()) {
				outputFiles.put(signatureId, new PrintWriter(Files.newBufferedWriter(
						Paths.get(outputFolder).resolve(PROOFS_PREFIX + signatureId + LISTS_SUFFIX))));
			}

		} catch (IOException e) {
			System.err.println("Could not create output files: " + outputFolder);
			e.printStackTrace();
			return;
		}

//		try (Stream<String> proofFiles = Files.lines(Paths.get(proofsList))) {
//
//			JsonProofParser parser = JsonProofParser.getInstance();
//
//			proofFiles.filter(proofName -> proofName.startsWith(SNOMED_PREFIX))
//					.forEach(proofName -> sortToFiles(proofName,
//							getProofSignature(proofsFolder + File.separator + proofName, parser), signatures,
//							outputFiles));
//
//		} catch (IOException e) {
//			System.err.println("Could not read file: " + proofsList);
//			e.printStackTrace();
//			return;
//		}

		JsonProofParser parser = JsonProofParser.getInstance();

		int sig = 0;

		for (Entry<String, List<String>> signatureEntry : signatures.entrySet()) {
			sig++;
			Path p = Paths.get(proofListsFolder).resolve(PROOFS_PREFIX + signatureEntry.getKey() + LISTS_SUFFIX);

			try {
				List<String> proofNames = Files.lines(p).filter(proofName -> proofName.startsWith(SNOMED_PREFIX))
						.collect(Collectors.toList());
				System.out.println("Processing " + signatureEntry.getKey() + " ... " + proofNames.size() + " tasks");

				int count = 0;

				for (String proofName : proofNames) {

					count++;
					if (count % 100 == 0) {
						System.out.println(
								"(" + sig + ") " + signatureEntry.getKey() + ": " + count + "/" + proofNames.size());
						outputFiles.get(signatureEntry.getKey()).flush();
					}

					Iterable<String> proofSignature = getProofSignature(proofsFolder + File.separator + proofName,
							parser);
					if (checkLargerOverlap(proofSignature, signatureEntry.getValue(), 5)) {
						outputFiles.get(signatureEntry.getKey()).println(proofName);
						System.out.println("(" + sig + ") " + signatureEntry.getKey() + ": " + proofName);
					}
				}

			} catch (IOException e) {
				System.err.println("Could not read file: " + p.toString());
				e.printStackTrace();
				return;
			}

		}

		for (PrintWriter file : outputFiles.values()) {
			file.close();
		}

	}

	private static void stop() {
		throw new RuntimeException();
	}

	private static Iterable<String> getProofSignature(String fileName, JsonProofParser parser) {
		IProof<OWLAxiom> proof = parser.fromFile(new File(fileName));
		List<? extends OWLAxiom> axioms = proof.getInferences().get(0).getPremises();
		Set<String> signature = new HashSet<>();
		for (OWLAxiom axiom : axioms) {
			for (OWLEntity entity : axiom.getSignature()) {
				signature.add(entity.getIRI().toString());
			}
		}
		return signature;
	}

	private static void sortToFiles(String proofName, Iterable<String> proofSignature,
			Map<String, List<String>> signature, Map<String, PrintWriter> outputFiles) {

		COUNT++;
		if (COUNT % 100 == 0) {
			System.out.println(COUNT);
			for (PrintWriter file : outputFiles.values()) {
				file.flush();
			}
//			stop();
		}

		for (Entry<String, List<String>> signatureEntry : signature.entrySet()) {
			if (checkNontrivialOverlap(proofSignature, signatureEntry.getValue())) {
//				System.out.println("Selected task for '" + signatureEntry.getKey() + "': " + proofName);
				outputFiles.get(signatureEntry.getKey()).println(proofName);
			}
		}
	}

	private static boolean checkNontrivialOverlap(Iterable<String> proofSignature, List<String> signature) {
//		System.out.println("Checking task for overlap: " + proofFile);

		boolean oneIsContained = false;
		boolean oneIsNotContained = false;

//		System.out.println("Loaded task of size " + axioms.size());

		for (String entity : proofSignature) {
//			System.out.print(entity.getIRI() + " ... ");
			if (signature.contains(entity)) {
				oneIsContained = true;
//				System.out.println("is contained.");
			} else {
				oneIsNotContained = true;
//				System.out.println("is not contained.");
			}
			if (oneIsContained && oneIsNotContained) {
				return true;
			}
		}
//		throw new RuntimeException();
		return false;

	}

	private static boolean checkLargerOverlap(Iterable<String> proofSignature, List<String> signature, int overlap) {
		int count = 0;
		for (String entity : proofSignature) {
			if (signature.contains(entity)) {
				count++;
				if (count == overlap) {
					return true;
				}
			}
		}
		return false;
	}

}
