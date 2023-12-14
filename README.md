
This collection of tools was written for the purpose of evaluating the proof services of [Evee](https://github.com/de-tu-dresden-inf-lat/evee), which can explain entailments in various description logics. Proofs are stored in a JSON format as used by the `evee-data` library.

This code was used for the following papers:

* Christian Alrabbaa, Franz Baader, Stefan Borgwardt, Patrick Koopmann, and Alisa Kovtunova. _Finding small proofs for description logic entailments: Theory and practice_. In Proceedings of the 23rd International Conference on Logic for Programming, Artificial Intelligence and Reasoning (**LPAR’20**), pages 32–67, 2020. EasyChair.
[doi:10.29007/nhpp](https://doi.org/10.29007/nhpp)

* Stefan Borgwardt. _Concise justifications versus detailed proofs for description logic entailments_. In 2nd Workshop on Explainable Logic-Based Knowledge Representation (**XLoKR’21**), 2021.
https://xlokr21.ai.vub.ac.be/papers/04/paper.pdf

* Christian Alrabbaa, Franz Baader, Stefan Borgwardt, Raimund Dachselt, Patrick Koopmann, and Julián Méndez. _Evonne: Interactive proof visualization for description logics (System description)_. In Proceedings of the 11th International Joint Conference on Automated Reasoning (**IJCAR’22**), pages 271–280, 2022. Springer-Verlag.
[doi:10.1007/978-3-031-10769-6_16](https://doi.org/10.1007/978-3-031-10769-6_16), [arxiv:2205.09583](https://arxiv.org/abs/2205.09583)

* Christian Alrabbaa, Stefan Borgwardt, Tom Friese, Patrick Koopmann, Julián Méndez, and Alexej Popovič. _On the eve of true explainability for OWL ontologies: Description logic proofs with Evee and Evonne_. In Proceedings of the 35th International Workshop on Description Logics (**DL’22**), 2022.
https://ceur-ws.org/Vol-3263/paper-2.pdf, [arxiv:2206.07711](https://arxiv.org/abs/2206.07711)


## Extracting entailments from ontologies

As a first step, one can extract a representative set of "entailment tasks" from a given ontology, up to renaming of atomic symbols. For this, all entailments of the form `A SubClassOf B` are computed and stored together with their justifications `J`. If one of the resulting tasks `(J, A SubClassOf B)` is equivalent to a previously computed task (up to renaming of atomic classes, properties, and individuals), it is discarded. The resulting tasks are anonymized, i.e., all names are replaced by generic ones like `A` and `r`.

_Main class:_ `de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction.ExtractEntailments`

_Arguments:_ 
1. Path to the OWL ontology file
2. Path to an existing output folder
3. Path to an existing file for logging error messages
4. Path to an existing file to use for storing the progress of the script (in case this is called subsequently for multiple ontologies)
5. The expressivity of the extracted tasks: Either `EL` or `ALCH` (the latter excludes tasks that are included in `EL`) 
6. Either `UNIONS` or `SINGLE`, indicating whether the tasks are constructed using the union of all justifications (making them harder for the proof generators), or each justification separately (resulting in more tasks)

_Output:_ A folder containing the extracted proof tasks in JSON format (`taskZZZZZ.json`), as well as files indicating how often each task appeared in the ontology (`numbZZZZZ.json`). If this is called for multiple ontologies, each call will reuse and merge the results with the output of the previous calls.


## Generating proofs

The second step is to generate proofs for the tasks with one of the proof generators from Evee, see `de.tu_dresden.inf.lat.evee.proofs.data.ProofGeneratorMain` in [Evee](https://github.com/de-tu-dresden-inf-lat/evee).

_Output:_ A folder containing proof files in JSON format (`proofZZZZZ.json`).


## Extracting proofs from derivation structures

In case a proof generator computes derivation structures that contain multiple proofs, the next step is to extract unique proofs according to some criteria.

_Main class:_ `de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.ExtractUniqueProofs`

_Arguments:_
1. Path to input folder containing derivation structures named `proofZZZZZ.json`
2. Either `TREES` or `GRAPHS`, depending on whether the tree size or the hypergraph size (number of distinct axioms) of the extracted proofs should be minimized.
3. (Optional) Path to a file containing the signature that should be used for condensing the proofs. The file contains one line for each IRI in the signature. Axioms that are formulated completely over this signature are assumed to be "known" and will not have a sub-proof.

_Output:_ A subfolder containing the extracted proofs in JSON format (`proofZZZZZ.json`). Additionally, a file `times.csv` containing the runtimes of all extraction operations.


## Generating proof images

To visualize the computed proofs, one can generate image files from the JSON format.

_Main class:_ `de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.ImageGenerator`

_Arguments:_
1. Input folder containing proofs in  JSON format
2. Either `TREES` or `GRAPHS`, to indicate the type of visualization
3. (Optional) Ontology file for replacing IRIs by labels (`skos:prefLabel`)
4. (Optional) Signature file (list of IRIs) for marking designated entities with `**` in the proof

_Output:_ For each JSON file, a PNG file and an SVG file with a visual representation of the proof


## Extracting rules from proofs

In case a proof generator does not use a fixed set of inference rules (e.g. `evee-elimination-proofs`), one can extract the set of all inference steps used in a given collection of proofs. This can be useful to analyze the diversity and comprehensibility of individual proof steps that are output by the proof generator. Any IRIs and inference labels used in the original proofs will be replaced by generic names like `A` and `r`.

_Main class:_ `de.tu_dresden.inf.lat.evee.proofs.evaluation.rule_extraction.ExtractRules`

_Arguments:_
1. Input folder containing the proofs in JSON format
2. Output folder
3. The expressivity of the proofs: Either `EL` or `ALCH`

_Output:_ Individuals inference steps in JSON format (`ruleZZZZZ.json`), as well as files indicating how often each rule was used in the given proofs (`numbZZZZZ.json`). If the output folder already contains such JSON files, the results are merged.


## Evaluating proofs

After generating proofs, one can evaluate them according to various measures.

_Main class:_ `de.tu_dresden.inf.lat.evee.proofs.evaluation.Stats`

_Arguments:_
1. Input folder containing JSON files
2. The expressivity of the proofs: Either `EL` or `ALCH`
3. Indicates whether full `PROOFS` or individual `RULES` should be evaluated (rules have to be extracted first, see above)
4. (For proofs) The folder containing the original proof tasks, for example to evaluate whether the proofs are correct
5. (Optional) A signature file (list of IRIs) for evaluating how many entities in the proofs are from the given signature

_Output:_ A file `stats.csv` containing one line per input proof/rule. For proofs, the following measures are computed (see the papers cited above for details):
1. Tree size
2. (Hypergraph) size
3. Number of inference steps
4. Depth
5. Redundancy (number of superfluous inference steps that re-derive an already derived axiom)
6. Correctness w.r.t. the original task
7. The sum of the "justification complexities" of all inference steps, as defined in: Matthew Horridge, Samantha Bail, Bijan Parsia, Uli Sattler: _Toward cognitive support for OWL justifications_. Knowl. Based Syst. 53: 66-79 (2013)
8. The maximal justification complexity
9. The average justification complexity
10. The number of leafs in the proofs
11. The justification complexity of the proof task itself (ignoring all intermediate steps)
12. How many of the entities occurring in the proof are included in the given signature (in %)
13. The time it took to compute the proof, possibly split into generation and minimization time. This only works if for each `proofZZZZZ.json` there is a file `proofZZZZZ.output` that contains the output of `de.tu_dresden.inf.lat.evee.proofs.data.ProofGeneratorMain` from `evee`
14. The size of the signature of the proof task


## Generating diagrams

After the evaluation measures are computed, one can generate diagrams comparing one or more proof generators or proof measures to each other. This was used to generate the graphs in the above-mentioned papers.

_Main class:_ de.tu_dresden.inf.lat.evee.proofs.evaluation.Charts

_Arguments:_
1. The path to the root folder containing all required subfolders for the comparisons, each with a `stats.csv` file
2. The name of the output subfolder
3. The 'mode' which can be one of `LPAR20`, `XLoKR21`, `IJCAR22-SNOMED`, `IJCAR22-GALEN`, `IJCAR22-FBA`, `IJCAR22-FBA-FAME`, `DL22-LETHE`, each of which corresponds to a set of graphs for the corresponding paper. For each mode, the folders are assumed to follow a certain naming scheme. To make other comparisons, the source code has to be adapted.
4. The folder containing the original proof tasks. This is used for adjusting certain numbers according to the frequency of the underlying entailment task in the original corpus of ontologies (recorded in the `numZZZZZ.txt` files)

_Output:_ A collection of CSV and PDF files containing the raw data and the graphs, respectively, for each comparison





