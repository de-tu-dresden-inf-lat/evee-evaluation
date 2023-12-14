package de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import com.google.common.base.Predicate;

import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.general.tools.ConceptNameGenerator;
import de.tu_dresden.inf.lat.evee.general.tools.IndividualNameGenerator;
import de.tu_dresden.inf.lat.evee.proofs.tools.ProofTools;
import de.tu_dresden.inf.lat.evee.general.tools.RoleNameGenerator;

public class InferenceCollection {

	private Map<IInference<OWLAxiom>, Long> inferences = new HashMap<>();

	public int size() {
		return inferences.size();
	}

	public void add(IInference<OWLAxiom> inf) {
		Long oldNum = inferences.containsKey(inf) ? inferences.get(inf) : 0l;
		inferences.put(inf, oldNum + 1);
	}

	public void filter(Predicate<IInference<OWLAxiom>> pred) {
		Iterator<IInference<OWLAxiom>> i = inferences.keySet().iterator();
		while (i.hasNext()) {
			if (!pred.apply(i.next())) {
				i.remove();
			}
		}
	}

	public void load(String folder, String prefix) {
		try {
			Iterator<Path> i = Files.walk(Paths.get(folder)).filter(Files::isRegularFile)
					.filter(f -> f.getFileName().toString().startsWith(prefix)).iterator();
			while (i.hasNext()) {
				loadInference(i.next());
			}
		} catch (IOException e) {
			System.err.println("Could not load inferences from folder: " + folder);
			e.printStackTrace();
		}
	}

//	public static void write(String folder, String prefix, Map<IInference, List<IInference>> proofs,
//			boolean writeOriginals) {
//		Utils.cleanFolder(folder);
//
//		int i = 1;
//		for (Entry<IInference, List<IInference>> pair : proofs.entrySet()) {
//			writeProofData(folder, prefix, i, pair.getKey(), pair.getValue(), (long) pair.getValue().size(),
//					writeOriginals);
//			i++;
//		}
//	}

	public void write(String folder, String prefix) {
		Utils.cleanFolder(folder);

		int i = 1;
		for (Entry<IInference<OWLAxiom>, Long> pair : inferences.entrySet()) {
			write(folder, prefix, i, pair.getKey(), pair.getValue());
			i++;
		}
	}

	private void write(String folder, String prefix, int i, IInference<OWLAxiom> proof, Long num) {
		Utils.writeProof(proof, String.format(folder + prefix + "%05d", i));
		Utils.writeLines(Long.toString(num).getBytes(), String.format(folder + "numb%05d.txt", i));
//		if (writeOriginals) {
//			int j = 1;
//			for (IInference inf : originals) {
//				Utils.writeProof(inf, String.format(folder + "orig%05d_%03d", i, j));
//				j++;
//			}
//		}
	}

	public void map(Function<IInference<OWLAxiom>, IInference<OWLAxiom>> f) {
		Map<IInference<OWLAxiom>, Long> newInfs = new HashMap<>();
		for (Entry<IInference<OWLAxiom>, Long> pair : inferences.entrySet()) {
			IInference<OWLAxiom> newInf = f.apply(pair.getKey());
			Long newNum = (newInfs.containsKey(newInf) ? newInfs.get(newInf) : 0) + pair.getValue();
			newInfs.put(newInf, newNum);
		}
		inferences = newInfs;
	}

	private void loadInference(Path taskPath) throws IOException {
		String id = taskPath.getFileName().toString().substring(4, 9);
		Path numbPath = taskPath.resolveSibling("numb" + id + ".txt");
		IInference<OWLAxiom> inf = Utils.loadProof(taskPath).getInferences().get(0);
		Long num = Long.valueOf(Files.readAllLines(numbPath).get(0));
		inferences.put(inf, num);
	}

	public void computeQuotient() {
		InferenceComparator comp = new InferenceComparator(true);
		Map<IInference<OWLAxiom>, Long> infs = inferences;
		inferences = new HashMap<>();
		int i = 0;
		for (Entry<IInference<OWLAxiom>, Long> pair : infs.entrySet()) {
			addModuloIsomorphisms(pair.getKey(), pair.getValue(), comp);
			i++;
			if (i % 100 == 0) {
				System.out.println(i);
			}
		}
	}

	public <T> void addModuloIsomorphisms(IInference<OWLAxiom> inf, Long num, InferenceComparator comp) {
		OWLObjectMap iso = null;
		IInference<OWLAxiom> existingInf = null;

		for (IInference<OWLAxiom> inf2 : inferences.keySet()) {
			iso = comp.isomorphic(inf, inf2);
			if (iso != null) {
				existingInf = inf2;
				break;
			}
		}
		if (iso == null) {
			// no isomorphic representative was found -> start a new equivalence class
			inferences.put(inf, num);
		} else {
			// merge the two inferences 'inf' and 'existingInf' into one entry
			IInference<OWLAxiom> simplifiedInf = simplify(iso, inf);
			Long newNum = inferences.get(existingInf) + num;
			inferences.remove(existingInf);
			inferences.put(simplifiedInf, newNum);
			if (!Utils.isCorrect(simplifiedInf)) {
				System.err.println("Incorrect inference after applying mapping!");
				System.err.println(inf);
				System.err.println(iso);
				System.err.println(existingInf);
				System.err.println(simplifiedInf);
			}
		}
	}

	private IInference<OWLAxiom> simplify(OWLObjectMap map, IInference<OWLAxiom> inf) {
		OWLObjectMap simpleMap = new OWLObjectMap();
		Set<OWLEntity> sig = ProofTools.getSignature(inf);
		ConceptNameGenerator A = new ConceptNameGenerator("", false, sig);
		RoleNameGenerator R = new RoleNameGenerator("", false, sig);
		IndividualNameGenerator I = new IndividualNameGenerator("", false, sig);

		for (OWLObject o : new LinkedList<>(map.getDomain())) {
			// remove maps that make the inference more complicated
			if (o instanceof OWLClass) {
				continue;
			}

			// rename all OWLObjects in the range, to avoid confusion with domain (inf may
			// already contain these OWLObjects)
			if (o instanceof OWLClassExpression) {
				simpleMap.add(o, A.next());
			} else if (o instanceof OWLObjectPropertyExpression) {
				simpleMap.add(o, R.next());
			} else if (o instanceof OWLIndividual) {
				simpleMap.add(o, I.next());
			} else {
				throw new UnsupportedOperationException(
						"Unsupported type of OWLObject: " + o + " (" + o.getClass().getName() + ")");
			}
		}

		return new OWLAxiomReplacer(simpleMap).visit(inf);
	}

	@Override
	public String toString() {
		return inferences.keySet().toString();
	}

}
