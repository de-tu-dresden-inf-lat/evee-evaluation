package de.tu_dresden.inf.lat.evee.proofs.evaluation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.fop.svg.PDFTranscoder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.DefaultXYDataset;

import de.tu_dresden.inf.lat.evee.proofs.evaluation.tools.Utils;

public class Charts {

	private static String root;
	private static String output_folder;

	private static String task_folder;

	private static final int width = 1000;

	private static final int height = 1000;

	private static final int axis_length = 100;

	private static final String font_name = "Latin Modern Roman";

	private static final int large_font_size = 60;

	private static final String[] measure_names = new String[] { //
			"Tree size", // 0
			"Proof size", // 1
			"Inferences", // 2
			"Depth", // 3
			"Redundancies", // 4
			"Correctness", // 5
			"Step complexity (sum)", // 6
			"Maximum step complexity", // 7
			"Average step complexity", // 8
			"Justification size", // 9
			"Justification complexity", // 10
			"Signature coverage (%)", // 11
			"Total time (ms)", // 12
			"Generation time (ms)", // 13
			"Minimization time (ms)", // 14
			"Task signature size" // 15
	};

	private static final int total_measures = measure_names.length;

	private static final String[] measure_ids = new String[] { "treesize", "size", "infs", "depth", "redun", "corr", "sum",
			"max", "avg", "just", "justc", "sigcov", "totaltime", "gentime", "mintime", "sigsize" };

	private static int[][] measure_comparisons;

	private static int[] measures;

	private static int[] ratio_measures;

	private static final int bins = 20;

	private static double[] measures_max;

	private static int[][] ratios;

	private static double[] ratios_max;

	private static int total_proofs;

	private static String[] folders;

	private static String[] names;

	private static int[][] comparisons;

	private static double[][][] data;

	private static int[] count;

	private static boolean adjusted = false;

	public static void main(String[] args) {

		if ((args.length < 3) || (args.length > 5)) {
			System.out.println("Expected 3-4 arguments: ROOT_FOLDER OUTPUT_FOLDER MODE [TASK_FOLDER]");
			System.out.println(
					"MODE can be one of LPAR20, XLoKR21, IJCAR22-SNOMED, IJCAR22-GALEN, IJCAR22-FBA, IJCAR22-FBA-FAME, DL22-LETHE");
			return;
		}

		root = args[0];
		output_folder = args[0] + "/" + args[1];
		if (args.length > 3) {
			task_folder = args[3];
			adjusted = true;
		}

		switch (args[2]) {
			case "LPAR20":
				// Original LPAR20 experiments -- pairwise comparisons between ELK, LETHE and
				// FAME
				// on 1573 extracted justification patterns

				measures = new int[]{0, 3, 6, 7, 8};
				measures_max = new double[]{310d, 65d, 0d, 35d, 0d, 0d, 15000d, 1400d, 550d, 26d, 1500d, 100d, 1000d,
						100d, 1000d};
				total_proofs = 1573;
				folders = new String[]{"el-proofs-ELK/unique", "el-proofs-LETHE", "el-proofs-FAME"};
				names = new String[]{"ELK", "LETHE", "FAME"};
				comparisons = new int[][]{{0, 1}, {0, 2}, {1, 2}};

				loadData();
				Utils.cleanFolder(output_folder);
				plotGeneratorComparisons(false);

				break;
			case "XLoKR21":
				// XLoKR'21 experiments -- comparisons between different measures of proof
				// complexity vs. justification complexity, for ELK, LETHE, and FAME, including
				// histograms of their ratios (same 1573 justification patterns from above)

				measure_comparisons = new int[][]{{9, 1}, {10, 7}, {10, 8}};
				measures_max = new double[]{310d, 65d, 0d, 35d, 0d, 0d, 15000d, 900d, 500d, 26d, 1500d, 100d, 1000d,
						100d, 1000d};
				total_proofs = 3;
				folders = new String[]{"el-proofs-ELK/unique", "el-proofs-LETHE", "el-proofs-FAME"};
				names = new String[]{"ELK", "LETHE", "FAME"};

				// "Ratio of tree size vs. justification size", "Ratio of max jc vs. jc", "Ratio
				// of avg jc vs. jc"
				ratios = new int[][]{{1, 9}, {7, 10}, {8, 10}};
				ratios_max = new double[]{10d, 2.5d, 2d};

				loadData();
				Utils.cleanFolder(output_folder);
				plotMeasureComparisons();
				plotMeasureRatioHistograms();

				break;
			case "IJCAR22-SNOMED":
				// SNOMED signature experiments for IJCAR22 -- comparing impact of known
				// signature for 4 signatures after randomly selecting 500 proof tasks from
				// SNOMED (based on union of justifications) that each have overlap at least 5
				// with the known signature

				measures = new int[]{0, 3, 7, 8, 13, 14};
				measures_max = new double[]{310d, 65d, 0d, 35d, 0d, 0d, 15000d, 1400d, 550d, 26d, 1500d, 100d, 1000d,
						100d, 1000d};
				total_proofs = 500;
				folders = new String[]{"proofs-ELK/Default", "proofs-ELK/Default-sig", "proofs-ELK/GPFP",
						"proofs-ELK/GPFP-sig", "proofs-ELK/GPS", "proofs-ELK/GPS-sig", "proofs-ELK/IPS",
						"proofs-ELK/IPS-sig"};
				names = new String[]{"Def", "Def-sig", "GPFP", "GPFP-sig", "GPS", "GPS-sig", "IPS", "IPS-sig"};
				comparisons = new int[][]{{0, 1}, {2, 3}, {4, 5}, {6, 7}};
				ratio_measures = new int[]{0};

				loadData();
				Utils.cleanFolder(output_folder);
				plotGeneratorComparisonsSingleGraph();
				plotRatiosVsCoverageSingleGraph();
				break;
			case "IJCAR22-GALEN":
				// similar experiments for Galen, signatures were extracted from top-level
				// hierarchy
				measures = new int[]{0, 3, 7, 8, 13, 14};
				measures_max = new double[]{310d, 65d, 0d, 35d, 0d, 0d, 15000d, 1400d, 550d, 26d, 1500d, 100d, 0d, 0d,
						0d};
				total_proofs = 1000;
				folders = new String[]{"proofs-galen-min/AbstractState", "proofs-galen-con/AbstractState",
						"proofs-galen-min/Anonymous-3", "proofs-galen-con/Anonymous-3", "proofs-galen-min/Anonymous-700",
						"proofs-galen-con/Anonymous-700", "proofs-galen-min/Anonymous-739",
						"proofs-galen-con/Anonymous-739", "proofs-galen-min/ApplicationCategory",
						"proofs-galen-con/ApplicationCategory", "proofs-galen-min/Axiom_1", "proofs-galen-con/Axiom_1",
						"proofs-galen-min/Diuresis", "proofs-galen-con/Diuresis", "proofs-galen-min/DomainCategory",
						"proofs-galen-con/DomainCategory", "proofs-galen-min/ExcretionOfUrine",
						"proofs-galen-con/ExcretionOfUrine", "proofs-galen-min/FractionalRange",
						"proofs-galen-con/FractionalRange", "proofs-galen-min/ImpreciseNumberValueType",
						"proofs-galen-con/ImpreciseNumberValueType", "proofs-galen-min/MagnitudeValueType",
						"proofs-galen-con/MagnitudeValueType", "proofs-galen-min/MajorMinorSelector",
						"proofs-galen-con/MajorMinorSelector", "proofs-galen-min/ModelInformation",
						"proofs-galen-con/ModelInformation", "proofs-galen-min/ModifierConcept",
						"proofs-galen-con/ModifierConcept", "proofs-galen-min/NonNormalState",
						"proofs-galen-con/NonNormalState", "proofs-galen-min/NumericQuantity",
						"proofs-galen-con/NumericQuantity", "proofs-galen-min/OrdinalPositionValueType",
						"proofs-galen-con/OrdinalPositionValueType", "proofs-galen-min/OrganismState",
						"proofs-galen-con/OrganismState", "proofs-galen-min/Phenomenon", "proofs-galen-con/Phenomenon",
						"proofs-galen-min/ProcessSelector", "proofs-galen-con/ProcessSelector",
						"proofs-galen-min/ProcessState", "proofs-galen-con/ProcessState", "proofs-galen-min/Selector",
						"proofs-galen-con/Selector", "proofs-galen-min/State", "proofs-galen-con/State",
						"proofs-galen-min/StructuralSelector", "proofs-galen-con/StructuralSelector",
						"proofs-galen-min/StructuralState", "proofs-galen-con/StructuralState",
						"proofs-galen-min/TopCategory", "proofs-galen-con/TopCategory", "proofs-galen-min/SubstanceState",
						"proofs-galen-con/SubstanceState", "proofs-galen-min/SymbolicQuantity",
						"proofs-galen-con/SymbolicQuantity", "proofs-galen-min/SymbolicValueType",
						"proofs-galen-con/SymbolicValueType", "proofs-galen-min/TemporalQuantity",
						"proofs-galen-con/TemporalQuantity", "proofs-galen-min/TopCategory", "proofs-galen-con/TopCategory",
						"proofs-galen-min/UrineCulturing", "proofs-galen-con/UrineCulturing", "proofs-galen-min/UrineTest",
						"proofs-galen-con/UrineTest", "proofs-galen-min/UrineTransport", "proofs-galen-con/UrineTransport",
						"proofs-galen-min/oneOrMore", "proofs-galen-con/oneOrMore"};
				names = new String[]{"AbstractState", "AbstractState-con", "Anonymous-3", "Anonymous-3-con",
						"Anonymous-700", "Anonymous-700-con", "Anonymous-739", "Anonymous-739-con", "ApplicationCategory",
						"ApplicationCategory-con", "Axiom_1", "Axiom_1-con", "Diuresis", "Diuresis-con", "DomainCategory",
						"DomainCategory-con", "ExcretionOfUrine", "ExcretionOfUrine-con", "FractionalRange",
						"FractionalRange-con", "ImpreciseNumberValueType", "ImpreciseNumberValueType-con",
						"MagnitudeValueType", "MagnitudeValueType-con", "MajorMinorSelector", "MajorMinorSelector-con",
						"ModelInformation", "ModelInformation-con", "ModifierConcept", "ModifierConcept-con",
						"NonNormalState", "NonNormalState-con", "NumericQuantity", "NumericQuantity-con",
						"OrdinalPositionValueType", "OrdinalPositionValueType-con", "OrganismState", "OrganismState-con",
						"Phenomenon", "Phenomenon-con", "ProcessSelector", "ProcessSelector-con", "ProcessState",
						"ProcessState-con", "Selector", "Selector-con", "State", "State-con", "StructuralSelector",
						"StructuralSelector-con", "StructuralState", "StructuralState-con", "SubstanceState",
						"SubstanceState-con", "SymbolicQuantity", "SymbolicQuantity-con", "SymbolicValueType",
						"SymbolicValueType-con", "TemporalQuantity", "TemporalQuantity-con", "TopCategory",
						"TopCategory-con", "UrineCulturing", "UrineCulturing-con", "UrineTest", "UrineTest-con",
						"UrineTransport", "UrineTransport-con", "oneOrMore", "oneOrMore-con"};
				comparisons = new int[][]{{0, 1}, {2, 3}, {4, 5}, {6, 7}, {8, 9}, {10, 11}, {12, 13},
						{14, 15}, {16, 17}, {18, 19}, {20, 21}, {22, 23}, {24, 25}, {26, 27}, {28, 29},
						{30, 31}, {32, 33}, {34, 35}, {36, 37}, {38, 39}, {40, 41}, {42, 43}, {44, 45},
						{46, 47}, {48, 49}, {50, 51}, {52, 53}, {54, 55}, {56, 57}, {58, 59}, {60, 61},
						{62, 63}, {64, 65}, {66, 67}, {68, 69}};
				ratio_measures = new int[]{0};

				loadData();
				Utils.cleanFolder(output_folder);
				plotGeneratorComparisonsSingleGraph();
				plotRatiosVsCoverageSingleGraph();
				break;
			case "IJCAR22-FBA":
				// FBA experiments for IJCAR22 -- comparing different optimizations of
				// forgetting-based proof generators using 138 ALC union-of-justification
				// patterns extracted from BioPortal
				// additionally, plot diagrams adjusted by original task count (extracted from
				// task folder)

				measures = new int[]{0, 3, 7, 8, 12};
				measures_max = new double[]{120d, 65d, 0d, 20d, 0d, 0d, 15000d, 800d, 550d, 26d, 1500d, 100d, 10000d,
						100d, 6000d};
				total_proofs = 138;
				folders = new String[]{"lethe-original", "lethe-size-optimized", "lethe-symb-optimized"};
				names = new String[]{"LETHE-orig", "LETHE-size", "LETHE-symb"};
				comparisons = new int[][]{{0, 1}, {0, 2}, {1, 2}};

				loadData();
				Utils.cleanFolder(output_folder);
				plotGeneratorComparisons(true);

				break;
			case "IJCAR22-FBA-FAME":
				// FBA experiments for IJCAR22 -- FAME variant

				measures = new int[]{0, 3, 7, 8, 12};
				measures_max = new double[]{120d, 65d, 0d, 20d, 0d, 0d, 15000d, 800d, 550d, 26d, 1500d, 100d, 10000d,
						100d, 6000d};
				total_proofs = 138;
				folders = new String[]{"fame-original", "fame-size-optimized", "fame-symb-optimized"};
				names = new String[]{"FAME-orig", "FAME-size", "FAME-symb"};
				comparisons = new int[][]{{0, 1}, {0, 2}, {1, 2}};

				loadData();
				Utils.cleanFolder(output_folder);
				plotGeneratorComparisons(true);

				break;
			case "DL22-LETHE":
				// experiments for DL22 -- similar to IJCAR22-FBA, but also comparing
				// lethe-proof-extractor

				measures = new int[]{0, 3, 7, 8, 12, 15};
				measures_max = new double[]{120d, 65d, 0d, 20d, 0d, 0d, 15000d, 800d, 550d, 26d, 1500d, 100d, 10000d,
						100d, 6000d, 40d};
				total_proofs = 138;
				folders = new String[]{"lethe-heuristic", "lethe-size-optimized", "lethe-symb-optimized",
						"lethe-extractor", "lethe-weighted-size-optimized"};
				names = new String[]{"LETHE-heur", "LETHE-size", "LETHE-symb", "LETHE-extr", "LETHE-wsize"};
				comparisons = new int[][]{{0, 1}, {0, 2}, {1, 2}, {0, 3}, {1, 3}, {2, 3}, {1, 4}};

				measure_comparisons = new int[][]{{15, 12}};
				ratios = new int[][]{{12, 15}};
				ratios_max = new double[]{3000d};

				loadData();
				Utils.cleanFolder(output_folder);
				plotGeneratorComparisons(true);
				plotMeasureComparisons();
				plotMeasureRatioHistograms();

				break;
			default:
				System.out.println("Unknown mode: " + args[2]);
		}

	}

	// histograms for measure ratios, e.g. tree size vs. justification size
	private static void plotMeasureRatioHistograms() {
		for (int i = 0; i < ratios.length; i++) {
			int measure1 = ratios[i][0];
			int measure2 = ratios[i][1];
			String outputFile = output_folder + "/ratio-" + measure_ids[measure1] + "-" + measure_ids[measure2];
			System.out.println(outputFile);
			Series[] series = new Series[folders.length];
			for (int j = 0; j < series.length; j++) {
				List<Double> ratioList = new ArrayList<>();
				for (int k = 0; k < data[j][measure1].length; k++) {
					if (data[j][measure2][k] > 0d) {
						double r = data[j][measure1][k] / data[j][measure2][k];
						ratioList.add(r);
						if (r > ratios_max[i]) {
							System.out.println(first(names[j]) + " " + (k + 1) + " " + r);
						}
					}
				}
				double[] ratio = ratioList.stream().mapToDouble(d -> d).toArray();
				series[j] = new Series(first(names[j]), ratio);
			}
			// TODO: export data?
			String title = measure_names[measure1] + " : " + measure_names[measure2];
			drawHistogram(outputFile, "", title, "Number of tasks", ratios_max[i], 950d, true, series);

			if (adjusted) {
				String outputFileAdj = output_folder + "/ratio-adj-" + measure_ids[measure1] + "-" + measure_ids[measure2];
				Series[] seriesAdj = new Series[folders.length];
				for (int j = 0; j < series.length; j++) {
					List<Double> ratioListAdj = new ArrayList<>();
					for (int k = 0; k < data[j][measure1].length; k++) {
						if (data[j][measure2][k] > 0d) {
							double r = data[j][measure1][k] / data[j][measure2][k];
							for (int x = 0; x < count[k]; x++) {
								ratioListAdj.add(r);
							}
						}
					}
					double[] ratioAdj = ratioListAdj.stream().mapToDouble(d -> d).toArray();
					seriesAdj[j] = new Series(first(names[j]), ratioAdj);
				}
				drawHistogram(outputFileAdj, "", title, "Original number of justifications", ratios_max[i], 2200000d, true,
						seriesAdj);
			}
		}
	}

	// compares measures, e.g., avg. step complexity vs. tree size
	private static void plotMeasureComparisons() {
		for (int[] measureComparison : measure_comparisons) {
			int measure1 = measureComparison[0];
			int measure2 = measureComparison[1];
			String outputFile = output_folder + "/" + measure_ids[measure1] + "-" + measure_ids[measure2];
			String outputFileAdj = output_folder + "/adj-" + measure_ids[measure1] + "-" + measure_ids[measure2];
			System.out.println(outputFile);
			boolean legend = true;
			boolean square = false;
			boolean correl = true;
			boolean adjust = false;
			Series[] series = new Series[folders.length];
			Series[] seriesAdj = new Series[folders.length];
			for (int j = 0; j < series.length; j++) {
				List<Double> data1List = new ArrayList<>();
				List<Double> data2List = new ArrayList<>();
				List<Double> dataAdj1List = new ArrayList<>();
				List<Double> dataAdj2List = new ArrayList<>();
				for (int k = 0; k < data[j][measure1].length; k++) {
					if (data[j][measure1][k] > 0d) {
						data1List.add(data[j][measure1][k]);
						data2List.add(data[j][measure2][k]);
						for (int x = 0; x < count[k]; x++) {
							dataAdj1List.add(data[j][measure1][k]);
							dataAdj2List.add(data[j][measure2][k]);
						}
					}
				}
				double[] data1 = data1List.stream().mapToDouble(d -> d).toArray();
				double[] data2 = data2List.stream().mapToDouble(d -> d).toArray();
				double[] dataAdj1 = dataAdj1List.stream().mapToDouble(d -> d).toArray();
				double[] dataAdj2 = dataAdj2List.stream().mapToDouble(d -> d).toArray();
				series[j] = new Series(first(names[j]), data1, measures_max[measure1], data2, measures_max[measure2]);
				seriesAdj[j] = new Series(first(names[j]), dataAdj1, measures_max[measure1], dataAdj2,
						measures_max[measure2]);
			}
			exportData(outputFile, "", measure_names[measure1], measure_names[measure2], measures_max[measure1],
					measures_max[measure2], square, adjust, series);
			drawScatterPlot(outputFile, "", measure_names[measure1], measure_names[measure2], measures_max[measure1],
					measures_max[measure2], legend, square, correl, series);
			drawScatterPlot(outputFileAdj, "", measure_names[measure1], measure_names[measure2], measures_max[measure1],
					measures_max[measure2], legend, square, correl, seriesAdj);
		}
	}

	// compare proof generators (ratio) vs. signature coverage (measured on the
	// first proof generator)
	private static void plotRatiosVsCoverageSingleGraph() {
		for (int measure : ratio_measures) {
			String outputFile = output_folder + "/" + measure_ids[measure] + "-ratio-vs-coverage";
			System.out.println(outputFile);

			String title = "original vs. condensed";
			String xName = measure_names[11];
			String yName = "Ratio of " + measure_names[measure] + " (%)";
			double xMax = measures_max[11];
			double yMax = 100d;
			boolean legend = true;
			boolean square = false;
			boolean correl = true;
			boolean adjust = false;

			Series[] series = new Series[comparisons.length];
			for (int i = 0; i < comparisons.length; i++) {
				int compare1 = comparisons[i][0];
				int compare2 = comparisons[i][1];

				double[] measureRatios = new double[data[compare1][measure].length];
				for (int k = 0; k < measureRatios.length; k++) {
					measureRatios[k] = 100d * data[compare2][measure][k] / data[compare1][measure][k];
				}
				series[i] = new Series(first(names[compare1]), data[compare1][11], xMax, measureRatios, yMax);
			}

			exportData(outputFile, title, xName, yName, xMax, yMax, square, adjust, series);
			drawScatterPlot(outputFile, title, xName, yName, xMax, yMax, legend, square, correl, series);
		}
	}

	// compare proof generators (ratio) vs. signature coverage (measured on the
	// first proof generator)
	private static void plotRatiosVsCoverage() {
		for (int[] comparison : comparisons) {
			int compare1 = comparison[0];
			int compare2 = comparison[1];
			for (int measure : ratio_measures) {
				String outputFile = output_folder + "/" + names[compare1] + "-" + names[compare2] + "-"
						+ measure_ids[measure] + "-ratio-vs-coverage";
				System.out.println(outputFile);

				String title = first(names[compare1]) + " vs. " + first(names[compare2]);
				String xName = measure_names[11];
				String yName = "Ratio of " + measure_names[measure] + " (%)";
				double xMax = measures_max[11];
				double yMax = 100d;
				boolean legend = false;
				boolean square = false;
				boolean correl = true;
				boolean adjust = false;
				double[] measureRatios = new double[data[compare1][measure].length];
				for (int k = 0; k < measureRatios.length; k++) {
					measureRatios[k] = 100d * data[compare2][measure][k] / data[compare1][measure][k];
				}
				Series series = new Series("", data[compare1][11], xMax, measureRatios, yMax);

				exportData(outputFile, title, xName, yName, xMax, yMax, square, adjust, series);
				drawScatterPlot(outputFile, title, xName, yName, xMax, yMax, legend, square, correl, series);
			}
		}
	}

	// compares proof generators;
	// all comparisons in the same plot (several series with different colors)
	// assumes that they are all of the same type / dimensions
	private static void plotGeneratorComparisonsSingleGraph() {
		for (int measure : measures) {
			String outputFile = output_folder + "/" + measure_ids[measure] + "-comp";
			System.out.println(outputFile);

			String title = measure_names[measure];
			String xName = "original";
			String yName = "condensed";
			double xMax = measures_max[measure];
			double yMax = measures_max[measure];
			boolean legend = true;
			boolean square = true;
			boolean correl = false;
			boolean adjust = false;

			Series[] series = new Series[comparisons.length];
			for (int i = 0; i < comparisons.length; i++) {
				int compare1 = comparisons[i][0];
				int compare2 = comparisons[i][1];

				series[i] = new Series(first(names[compare1]), data[compare1][measure], measures_max[measure],
						data[compare2][measure], measures_max[measure]);
			}

			exportData(outputFile, title, xName, yName, xMax, yMax, square, adjust, series);
			drawScatterPlot(outputFile, title, xName, yName, xMax, yMax, legend, square, correl, series);
		}
	}

	// compares proof generators
	private static void plotGeneratorComparisons(boolean adjusted) {
		for (int[] comparison : comparisons) {
			int compare1 = comparison[0];
			int compare2 = comparison[1];
			for (int measure : measures) {
				String outputFile = output_folder + "/" + names[compare1] + "-" + names[compare2] + "-"
						+ measure_ids[measure];
				System.out.println(outputFile);

				String title = measure_names[measure];
				String xName = first(names[compare1]);
				String yName = first(names[compare2]);
				double xMax = measures_max[measure];
				double yMax = measures_max[measure];
				boolean legend = false;
				boolean square = true;
				boolean correl = false;
				boolean adjust = false;
				Series series = new Series("", data[compare1][measure], measures_max[measure], data[compare2][measure],
						measures_max[measure]);

				exportData(outputFile, title, xName, yName, xMax, yMax, square, adjust, series);
				drawScatterPlot(outputFile, title, xName, yName, xMax, yMax, legend, square, correl, series);

				if (adjusted) {
					outputFile = output_folder + "/" + names[compare1] + "-" + names[compare2] + "-"
							+ measure_ids[measure] + "-adj";
					System.out.println(outputFile);
					adjust = true;
					series = new Series("", data[compare1][measure], measures_max[measure], data[compare2][measure],
							measures_max[measure], count);
					exportData(outputFile, title, xName, yName, xMax, yMax, square, adjust, series);
					// TODO support drawing adjusted diagrams
				}
			}
		}
	}

	private static String first(String name) {
		return name;
//		return name.split("-")[0];
	}

	static class Series {
		protected String name;
		protected double[][] data;

//		public Series(String name, double[] dataX, double[] dataY) {
//			this.name = name;
//			this.data = new double[][] { dataX, dataY };
////			for (int i = 0; i < dataX.length; i++) {
////				if (dataX[i] < 6000 && dataY[i] > 12000) {
////					System.out.println(name + " " + (i + 1) + " " + dataX[i] + " " + dataY[i]);
////				}
////			}
//		}

		public Series(String name, double[] dataX, double maxX, double[] dataY, double maxY, int... count) {
			if (count.length == 0) {
				count = new int[dataX.length];
				for (int i = 0; i < dataX.length; i++) {
					count[i] = 1;
				}
			}
			this.name = name;
			this.data = new double[][] { dataX, dataY };
			SummaryStatistics statsX = new SummaryStatistics();
			SummaryStatistics statsY = new SummaryStatistics();
			SummaryStatistics statsR = new SummaryStatistics();
			SummaryStatistics statsR2 = new SummaryStatistics();
			SummaryStatistics statsRF = new SummaryStatistics();
			SummaryStatistics statsRF2 = new SummaryStatistics();
			int equal = 0;
			int XgY = 0;
			int XsY = 0;
			int trivialY = 0;
			int trivialX = 0;
			for (int i = 0; i < dataX.length; i++) {
				double x = dataX[i];
				double y = dataY[i];
				int c = count[i];
				for (int j = 0; j < c; j++) {
					statsX.addValue(x);
					statsY.addValue(y);
					statsR.addValue(x / y);
					statsR2.addValue(y / x);
				}
				if (x > y) {
					XgY += c;
				}
				if (x < y) {
					XsY += c;
				}
				if (x == y) {
					equal += c;
				} else if (y <= 1) {
					trivialY += c;
				} else if (x <= 1) {
					trivialX += c;
				} else {
					for (int j = 0; j < c; j++) {
						statsRF.addValue(x / y);
						statsRF2.addValue(y / x);
					}
				}
				if (x > maxX || y > maxY) {
					System.out.println(name + " " + (i + 1) + ": " + x + " (" + maxX + "), " + y + " (" + maxY + ")");
				}
			}
			printStatistics(statsX, name + " (X)");
			printStatistics(statsY, name + " (Y)");
			printStatistics(statsR, name + " (X/Y)");
			printStatistics(statsR2, name + " (Y/X)");
			System.out.println(name + " (X=Y): " + equal);
			System.out.println(name + " (X>Y): " + XgY);
			System.out.println(name + " (X<Y): " + XsY);
			System.out.println(name + " (Y=1): " + trivialY);
			System.out.println(name + " (X=1): " + trivialX);
			printStatistics(statsRF, name + " (filtered X/Y)");
			printStatistics(statsRF2, name + " (filtered Y/X)");
		}

		public Series(String name, double[] dataX) {
			this.name = name;
			this.data = new double[][] { dataX };
			SummaryStatistics stats = new SummaryStatistics();
			for (double x : dataX) {
				stats.addValue(x);
			}
			printStatistics(stats, name);
		}

	}

	private static void printStatistics(SummaryStatistics stats, String name) {
		System.out.printf("%5s: Mean = %.2f; SD = %.2f; Max = %.2f; Min = %.2f%n", name, stats.getMean(),
				stats.getStandardDeviation(), stats.getMax(), stats.getMin());
	}

	private static void loadData() {
		data = new double[folders.length][total_measures][total_proofs];

		for (int i = 0; i < folders.length; i++) {
			String filename = root + "/" + folders[i] + "/stats.csv";
			String line;
			int l = 0;
			try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
				while ((line = br.readLine()) != null) {
					String[] contents = line.split(",");
					if (contents.length == 0 || contents[0].equals("Proof") || contents[0].equals("Sum")
							|| contents[0].equals("Average")) {
						continue;
					}
					for (int j = 0; j < total_measures; j++) {
						data[i][j][l] = Double.parseDouble(contents[j + 1]);
					}
					l++;
				}

			} catch (IOException e) {
				System.err.println("Could not read file: " + filename);
				e.printStackTrace();
			}
		}

		count = new int[total_proofs];
		if (adjusted) {
			long total = 0;
			for (int i = 0; i < total_proofs; i++) {
				String filename = String.format(task_folder + "/numb%05d.txt", i + 1);
				try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
					count[i] = Integer.parseInt(br.readLine());
					total += count[i];
				} catch (IOException e) {
					System.err.println("Could not read file: " + filename);
					e.printStackTrace();
				}
			}
			System.out.println("Total (potentiall isomorphic) proof tasks: " + total);
		} else {
			for (int i = 0; i < total_proofs; i++) {
				count[i] = 1;
			}
		}

	}

	private static void exportData(String filename, String title, String xName, String yName, double xMax, double yMax,
								   boolean square, boolean adj, Series... series) {
		if (square) {
			int xLarger = 0;
			int yLarger = 0;
			for (int i = 0; i < series[0].data[0].length; i++) {
				double x = series[0].data[0][i];
				double y = series[0].data[1][i];
				if ((x > 0d) && (y > 0d)) {
					if (x > y) {
						xLarger += (adj) ? count[i] : 1;
					}
					if (x < y) {
						yLarger += (adj) ? count[i] : 1;
					}
				}
			}
			xName += " (" + xLarger + ")";
			yName += " (" + yLarger + ")";
		}
		try {
			Files.write(Paths.get(filename + ".txt"),
					(title + "," + xName + "," + yName + "," + ((int) xMax) + "," + ((int) yMax)).getBytes());
			for (int i = 0; i < series.length; i++) {
				StringBuilder content = new StringBuilder();
				for (int j = 0; j < series[i].data[0].length; j++) {
					double x = series[i].data[0][j];
					double y = series[i].data[1][j];
					content.append((int) x);
					content.append(',');
					content.append((int) y);
					if (adj) {
						content.append(',');
						content.append(count[j]);
					}
					content.append('\n');
				}
				Files.write(Paths.get(filename + ((i > 0) ? ("-" + (i + 1)) : ("")) + ".csv"),
						content.toString().getBytes());
			}
		} catch (IOException e) {
			System.err.println("Could not write file: " + filename);
			e.printStackTrace();
		}
	}

	private static void drawHistogram(String filename, String title, String xName, String yName, double xMax,
			double yMax, boolean legend, Series... series) {

		SVGGraphics2D graphics = setupGraphics(true);

		HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.FREQUENCY);
		for (int i = 0; i < series.length; i++) {
			dataset.addSeries(series[i].name, series[i].data[0], bins, 0d, xMax);

			double mean = Arrays.stream(series[i].data[0]).average().orElse(0d);
			double sd = new StandardDeviation().evaluate(series[i].data[0]);
			System.out.printf("%5s: M = %.2f; SD = %.2f%n", names[i], mean, sd);
		}

		JFreeChart chart = ChartFactory.createHistogram(title, xName, yName, dataset, PlotOrientation.VERTICAL, legend,
				false, false);
		XYPlot plot = setupChart(chart);
		setupAxis((NumberAxis) plot.getDomainAxis(), updateMax(xMax, 0, series));
		setupAxis((NumberAxis) plot.getRangeAxis(), updateMax(yMax, 1, series));

		chart.draw(graphics, new Rectangle2D.Float(0, 0, width, height), new ChartRenderingInfo());

		saveToFile(filename, graphics);

	}

	private static void drawScatterPlot(String filename, String title, String xName, String yName, double xMax,
			double yMax, boolean legend, boolean square, boolean correl, Series... series) {

		if (correl) {
			for (Series s : series) {
				double[][] trans = new double[s.data[0].length][s.data.length];
				for (int j = 0; j < trans.length; j++) {
					for (int k = 0; k < trans[0].length; k++) {
						trans[j][k] = s.data[k][j];
					}
				}
				PearsonsCorrelation pear = new PearsonsCorrelation(trans);
				double corr = pear.getCorrelationMatrix().getEntry(0, 1);
				double stde = pear.getCorrelationStandardErrors().getEntry(0, 1);
				double pval = pear.getCorrelationPValues().getEntry(0, 1);
				System.out.printf("%5s: r = %.3f; σᵣ = %.3f; p = %.3f%n", s.name, corr, stde, pval);
			}
		}
		if (series[0].data[0].length > 5000) {
			return;
		}

		SVGGraphics2D graphics = setupGraphics(false);

		DefaultXYDataset dataset = new DefaultXYDataset();
		for (Series s : series) {
			dataset.addSeries(s.name, s.data);
		}
		if (square) {
			int xLarger = 0;
			int yLarger = 0;
			for (Series s : series) {
				for (int i = 0; i < s.data[0].length; i++) {
					double x = s.data[0][i];
					double y = s.data[1][i];
					if ((x > 0d) && (y > 0d)) {
						if (x > y) {
							xLarger++;
						}
						if (x < y) {
							yLarger++;
						}
					}
				}
			}
			xName += " (" + xLarger + ")";
			yName += " (" + yLarger + ")";
		}

		JFreeChart chart = ChartFactory.createScatterPlot(title, xName, yName, dataset, PlotOrientation.VERTICAL,
				legend, false, false);
		XYPlot plot = setupChart(chart);
		xMax = updateMax(xMax, 0, series);
		yMax = updateMax(yMax, 1, series);
		if (square) {
			xMax = yMax = Math.max(xMax, yMax);
		}
		NumberAxis dax = (NumberAxis) plot.getDomainAxis();
		NumberAxis rax = (NumberAxis) plot.getRangeAxis();
		setupAxis(dax, xMax);
		setupAxis(rax, yMax);

		ChartRenderingInfo info = new ChartRenderingInfo();
		chart.draw(graphics, new Rectangle2D.Float(0, 0, width, height), info);
//		Rectangle2D titleBounds = chart.getTitle().getBounds();
//		double titleCenterX = titleBounds.getCenterX();
//		chart.getTitle().setBounds(new Rectangle2D.Double(titleBounds.getX() + (width / 2) - titleCenterX,
//				titleBounds.getY(), titleBounds.getWidth(), titleBounds.getHeight()));
//		chart.draw(graphics, new Rectangle2D.Float(0, 0, width, height), info);
		if (square) {
			// sync ticks of axes, draw diagonal line
			Rectangle2D plotArea = info.getPlotInfo().getDataArea();
			rax.setTickUnit(dax.getTickUnit(), true, true);
			chart.draw(graphics, new Rectangle2D.Float(0, 0, width, height), info);
			graphics.setColor(Color.GRAY);
			graphics.drawLine((int) plotArea.getMinX(), (int) plotArea.getMaxY(), (int) plotArea.getMaxX(),
					(int) plotArea.getMinY());
		}

		saveToFile(filename, graphics);

	}

	private static XYPlot setupChart(JFreeChart chart) {
		chart.setAntiAlias(false);
		chart.setTextAntiAlias(true);
		chart.setPadding(new RectangleInsets(0, 0, 0, 30));
		chart.getTitle().setPadding(new RectangleInsets(0, 0, 20, 0));

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setDomainGridlineStroke(new BasicStroke());
		plot.setRangeGridlineStroke(new BasicStroke());

		if (plot.getRenderer() instanceof XYBarRenderer) {
			ClusteredXYBarRenderer renderer = new ClusteredXYBarRenderer();
			renderer.setBarPainter(new StandardXYBarPainter());
//			renderer.setDefaultLegendTextFont(((StandardChartTheme) ChartFactory.getChartTheme()).getSmallFont());
			renderer.setShadowVisible(false);
			plot.setRenderer(renderer);
		}

		return plot;
	}

	private static double updateMax(double max, int index, Series... series) {
		if (max == 0d) {
			DoubleSummaryStatistics values = new DoubleSummaryStatistics();
			for (Series s : series) {
				for (double value : s.data[index]) {
					values.accept(value);
				}
			}
			return values.getMax();
		}
		return max;
	}

	private static void saveToFile(String filename, SVGGraphics2D graphics) {
		try {

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Writer out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			graphics.stream(out);
			String svg = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
			SVGAbstractTranscoder transcoder = new PDFTranscoder();
			transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) width);
			transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float) height);
			transcoder.transcode(new TranscoderInput(new StringReader(svg)),
					new TranscoderOutput(Files.newOutputStream(new File(filename + ".pdf").toPath())));

		} catch (IOException | TranscoderException e) {
			System.err.println("Could not write file: " + filename);
			e.printStackTrace();
		}
	}

	private static SVGGraphics2D setupGraphics(boolean histogram) {
		SVGGraphics2D graphics = new SVGGraphics2D(GenericDOMImplementation.getDOMImplementation()
				.createDocument("http://www.w3.org/2000/svg", "svg", null));

		StandardChartTheme myTheme = (StandardChartTheme) org.jfree.chart.StandardChartTheme.createJFreeTheme();
		myTheme.setExtraLargeFont(new Font(font_name, Font.PLAIN, large_font_size / 10 * 9));
		myTheme.setLargeFont(new Font(font_name, Font.PLAIN, large_font_size / 10 * 7));
		myTheme.setRegularFont(new Font(font_name, Font.PLAIN, large_font_size / 10 * 6));
		myTheme.setSmallFont(new Font(font_name, Font.PLAIN, large_font_size / 2));
		myTheme.setPlotBackgroundPaint(Color.WHITE);
		myTheme.setDomainGridlinePaint(Color.GRAY);
		myTheme.setRangeGridlinePaint(Color.GRAY);
		myTheme.setAxisLabelPaint(Color.BLACK);
		myTheme.setTickLabelPaint(Color.BLACK);
		myTheme.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
		Paint[] paintSequence = adaptColors(DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE);
		if (!histogram) {
			paintSequence = makeTransparent(paintSequence);
		}
		Shape[] shapeSequence = enlarge(DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
		myTheme.setDrawingSupplier(new DefaultDrawingSupplier(paintSequence,
				DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE, DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE, shapeSequence));
		ChartFactory.setChartTheme(myTheme);
		return graphics;
	}

	private static Shape[] enlarge(Shape[] sources) {
		double f = 1.5d;
		Shape[] newShapes = new Shape[sources.length];
		for (int i = 0; i < sources.length; i++) {
			if (sources[i] instanceof Rectangle2D.Double) {
				Rectangle2D.Double r = (Rectangle2D.Double) sources[i];
				newShapes[i] = new Rectangle2D.Double(r.x * f, r.y * f, r.width * f, r.height * f);
			}
			if (sources[i] instanceof Ellipse2D.Double) {
				Ellipse2D.Double e = (Ellipse2D.Double) sources[i];
				newShapes[i] = new Ellipse2D.Double(e.x * f, e.y * f, e.width * f, e.height * f);
			}
			if (sources[i] instanceof Polygon) {
				Polygon p = (Polygon) sources[i];
				int[] newX = new int[p.npoints];
				int[] newY = new int[p.npoints];
				for (int j = 0; j < p.npoints; j++) {
					newX[j] = (int) (p.xpoints[j] * f);
					newY[j] = (int) (p.ypoints[j] * f);
				}
				newShapes[i] = new Polygon(newX, newY, p.npoints);
			}
		}
		return newShapes;
	}

	private static Color[] makeTransparent(Paint[] sources) {
		Color[] newColors = new Color[sources.length];
		for (int i = 0; i < sources.length; i++) {
			newColors[i] = makeTransparent((Color) sources[i], 50);
		}
		return newColors;
	}

	private static Paint[] adaptColors(Paint[] sources) {
		Paint[] colors = Arrays.copyOf(sources, sources.length);
		colors[0] = new Color(0xfa8c69, false);
		colors[1] = new Color(0x8d50fa, false);
		colors[2] = new Color(0x4ecfaf, false);
		colors[3] = new Color(0xc1b706, false);
		return colors;
	}

	private static Color makeTransparent(Color source, int alpha) {
		return new Color(source.getRed(), source.getGreen(), source.getBlue(), alpha);
	}

	private static void setupAxis(NumberAxis axis, double max) {
		double roundToNearest = Math.pow(10, Math.floor(Math.log10(max / 10)));
		axis.setRange(new Range(0, roundToNearest(max, roundToNearest)), true, true);
//		axis.setMinorTickCount(bins + 1);
		axis.setFixedAutoRange(axis_length);
		axis.setLabelInsets(new RectangleInsets(5, 0, 0, 5));
//		axis.setLabelFont(((StandardChartTheme) ChartFactory.getChartTheme()).getLargeFont());
		axis.setTickUnit(new NumberTickUnit(2 * roundToNearest(max / ((double) bins), roundToNearest)), true, true);
	}

	private static double roundToNearest(double max, double roundToNearest) {
		return Math.ceil(max / roundToNearest) * roundToNearest;
	}

}
