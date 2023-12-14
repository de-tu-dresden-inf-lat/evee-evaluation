package de.tu_dresden.inf.lat.evee.proofs.evaluation.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SignaturesFromRefsets {

	private static String INPUT_PREFIX = "der2_Refset_";

	private static String INPUT_SUFFIX = "SimpleFull_INT_20200731.txt";

	private static String IRI_PREFIX = "http://snomed.info/id/";

	private static String[] INPUTS = { "Dentistry", "GPFP", "GPS", "IPS", "NursingActivities", "NursingHealthIssues",
			"Odontogram", "" };

	private static String OUTPUT_PREFIX = "SNOMED_sig_";

	private static String OUTPUT_SUFFIX = ".txt";

	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.print("Expected arguments: INPUTFOLDER OUTPUTFOLDER");
			return;
		}

		String inputFolder = args[0];
		String outputFolder = args[1];

		for (String refSetName : INPUTS) {
			System.out.println("Refset name: " + refSetName);
			String inputFile = inputFolder + "/" + INPUT_PREFIX + refSetName + INPUT_SUFFIX;
			List<String> input = null;
			try {
				input = Files.readAllLines(Paths.get(inputFile));
			} catch (IOException e) {
				System.err.println("Could not read input file " + inputFile);
				e.printStackTrace();
				return;
			}
			System.out.println("Number of lines: " + input.size());

			List<String> iris = new ArrayList<String>();
			for (String line : input) {
				if (line.startsWith("id")) {
					continue;
				}
				String[] parts = line.split("\t");
				boolean active = (parts[2].equals("1"));
				String iri = parts[5];
				if (active) {
					iris.add(iri);
				} else {
					iris.remove(iri);
				}
			}
			System.out.println("Number of active IRIs: " + iris.size());

			if (refSetName.equals("")) {
				refSetName = "Default";
			}
			String outputFile = outputFolder + "/" + OUTPUT_PREFIX + refSetName + OUTPUT_SUFFIX;
			try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
				for (String iri : iris) {
					pw.println(IRI_PREFIX + iri);
				}
				pw.close();
			} catch (IOException e) {
				System.err.println("Could not write output file " + outputFile);
				e.printStackTrace();
			}
		}

	}

}
