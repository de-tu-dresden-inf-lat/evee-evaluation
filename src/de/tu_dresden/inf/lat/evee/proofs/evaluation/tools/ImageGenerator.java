package de.tu_dresden.inf.lat.evee.proofs.evaluation.tools;

import static guru.nidi.graphviz.model.Factory.to;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;

import de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction.OWLAxiomReplacer;
import de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction.OWLObjectMap;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import de.tu_dresden.inf.lat.evee.general.tools.OWLTools;
import de.tu_dresden.inf.lat.prettyPrinting.formatting.SimpleOWLFormatterLongNames;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class ImageGenerator {

	private static boolean TREES = false;

	private static List<String> SIGNATURE = null;

	private static OWLOntology ONTOLOGY;

	private static final OWLAnnotationProperty PREFLABEL = OWLTools.odf
			.getOWLAnnotationProperty(SKOSVocabulary.PREFLABEL.getIRI());

	public static void main(String[] args) {

		if ((args.length < 2) || (args.length > 4)) {
			System.out.println("Arguments: inputFolder TREES|GRAPHS [ontologyFile] [signatureFile]");
			return;
		}

		String inputFolder = args[0];
		TREES = args[1].equals("TREES");
		if (args.length >= 3) {
			ONTOLOGY = Utils.loadOntology(Paths.get(args[2]));
		}
		if (args.length == 4) {
			String signatureFile = args[3];
			try {
				SIGNATURE = Files.readAllLines(Paths.get(signatureFile));
				System.out.println("Signature: " + SIGNATURE.size());
			} catch (IOException e) {
				System.err.println("Could not open signature file " + signatureFile);
				e.printStackTrace();
				return;
			}
		}

		Utils.processProofs(inputFolder, ImageGenerator::processProof);

	}

	private static void processProof(Path input) {
		System.out.println(input);
		Utils.loadProofAndThen(input, proof -> {
			String filename = input.getFileName().toString();
			String output = input.resolveSibling(filename.substring(0, filename.length() - 5)) + ".png";
			new ImageGenerator().drawProof(proof, output);
		});
	}

	private int id = 0;

	private final Set<OWLAxiom> processedNodes = new HashSet<>();
	private final Map<OWLAxiom, MutableNode> nodeMap = new HashMap<>();

	private void drawProof(IProof<OWLAxiom> proof, String filename) {
		MutableGraph graph = Factory.mutGraph(filename).setDirected(true);

		fillGraph(graph, proof.getFinalConclusion(), proof);

		try {
			Graphviz.fromGraph(graph).render(Format.PNG).toFile(new File(filename));
			Graphviz.fromGraph(graph).render(Format.SVG_STANDALONE).toFile(new File(filename.replace(".png", ".svg")));
		} catch (IOException e) {
			System.err.println("Unable to write graph file: " + filename);
			e.printStackTrace();
		}
	}

	private MutableNode fillGraph(MutableGraph graph, OWLAxiom conclusion, IProof<OWLAxiom> proof) {

		if (!TREES) {
			// reuse already created nodes
			if (processedNodes.contains(conclusion)) {
				return nodeMap.get(conclusion);
			}
		}

		MutableNode conclusionNode = Factory.mutNode(String.valueOf(id++))
				.add(Label.of(SimpleOWLFormatterLongNames.format(marked(conclusion))));

		if (!TREES) {
			// store node for later use
			processedNodes.add(conclusion);
			nodeMap.put(conclusion, conclusionNode);
		}

		for (IInference<OWLAxiom> inf : proof.getInferences(conclusion)) {
			MutableNode hyperConnection = Factory.mutNode(String.valueOf(id++)).add(
					Label.of(inf.getRuleName().replace(" ", "\n")), Style.FILLED, Color.rgb(0, 191, 255),
					Shape.RECTANGLE);
			graph.add(hyperConnection.addLink(to(conclusionNode)));

			for (OWLAxiom prem : inf.getPremises()) {
				MutableNode premiseNode = fillGraph(graph, prem, proof);
				graph.add(premiseNode.addLink(hyperConnection));
			}
		}

		return conclusionNode;
	}

	private static OWLAxiom marked(OWLAxiom axiom) {

		if (SIGNATURE == null) {
			return axiom;
		}

		OWLObjectMap map = new OWLObjectMap();

		for (OWLEntity entity : axiom.getSignature()) {

			String label = getLabel(entity);
			OWLEntity newEntity;
			boolean marked = SIGNATURE.contains(entity.getIRI().toString());


			if (entity instanceof OWLClass) {
				newEntity = OWLTools.odf.getOWLClass(IRI.create(label + (marked ? "**" : "")));
			} else if (entity instanceof OWLObjectProperty) {
				newEntity = OWLTools.odf.getOWLObjectProperty(IRI.create(label + (marked ? "**" : "")));
			} else {
				throw new UnsupportedOperationException(
						"Only OWLClass and OWLObjectProperty are supported in signatures.");
			}

			map.add(entity, newEntity);
		}

		return axiom.accept(new OWLAxiomReplacer(map));
	}

	private static String getLabel(OWLEntity entity) {
		for (OWLAnnotationAssertionAxiom ann : ONTOLOGY.getAnnotationAssertionAxioms(entity.getIRI())) {
			if (ann.getProperty().equals(PREFLABEL)) {
				return ((OWLLiteral) ann.getValue()).getLiteral();
			}
		}
		return entity.getIRI().toString();
	}

}
