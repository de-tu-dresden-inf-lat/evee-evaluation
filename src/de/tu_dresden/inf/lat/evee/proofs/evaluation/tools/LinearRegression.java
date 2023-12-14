package de.tu_dresden.inf.lat.evee.proofs.evaluation.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.PopulationSize;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.Sigma;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.util.FastMath;
import org.semanticweb.owlapi.model.OWLAxiom;

import de.tu_dresden.inf.lat.evee.proofs.evaluation.Stats;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProofEvaluator;

public class LinearRegression {

	private static String inputFolder;

	private static final int total_features = 21;

	private static List<Integer> omit_features;

	private static double[][] rulesFeatures;

	public static void main(String[] args) {

		if (args.length != 3) {
			System.out.println("Arguments: inputFolder ratingsFolder EL|ALCH");
			return;
		}

		inputFolder = args[0];
		String ratingsFolder = args[1];
		Utils.EL_MODE = args[2].equals("EL");

		int total_rules;
		if (Utils.EL_MODE) {
			total_rules = 1176;
		} else {
			total_rules = 0;
		}
		rulesFeatures = new double[total_rules][total_features];

		readStats();

		try (Stream<Path> paths = Files.list(Paths.get(ratingsFolder))) {
			paths.filter(p -> p.getFileName().toString().startsWith("ratings")
					&& p.getFileName().toString().endsWith(".csv")).forEach(LinearRegression::processRatings);
		} catch (IOException e) {
			System.err.println("Could not read ratings: " + ratingsFolder);
			e.printStackTrace();
		}
	}

	private static void readStats() {
		String filename = inputFolder + "/stats.csv";
		String line;
		int l = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			while ((line = br.readLine()) != null) {
				String[] contents = line.split(",");
				if (contents.length == 0 || contents[0].equals("Proof") || contents[0].equals("Sum")
						|| contents[0].equals("Average")) {
					continue;
				}
				for (int j = 0; j < total_features; j++) {
					rulesFeatures[l][j] = Double.parseDouble(contents[j + 1]);
				}
				l++;
			}
		} catch (IOException e) {
			System.err.println("Could not read stats file: " + filename);
			e.printStackTrace();
		}
	}

	private static void processRatings(Path ratings) {
		System.out.println("===================== " + ratings.getFileName());
		List<String> lines;
		try {
			lines = Files.readAllLines(ratings);
			lines.removeIf(s -> !s.contains(","));
		} catch (IOException e) {
			System.err.println("Could not read ratings file: " + ratings);
			e.printStackTrace();
			return;
		}

		double[] y = new double[lines.size()];
		double[][] x = new double[lines.size()][total_features];
		for (int i = 0; i < lines.size(); i++) {
			String[] contents = lines.get(i).split(",");
			int rule = Integer.parseInt(contents[0].substring(4));
			y[i] = Double.parseDouble(contents[1]);
			x[i] = rulesFeatures[rule];
		}

		double[][] newX = omitZeroFeatures(x);

		double[] parameters = doOptimize(lines, y, newX);

		double[] filled_params = fillParameters(x, parameters);
		System.out.println("Important features:");
		int j = 0;
		for (IProofEvaluator<OWLAxiom> eval : Stats.ruleEvaluators) {
			if ((FastMath.exp(filled_params[j * 3]) > 0.3) && !omit_features.contains(j)) {
				System.out.println(eval.getDescription().replace("Aggregate (sum) of ", "") + ": "
						+ toString(filled_params[j * 3]) + " (" + toString(FastMath.exp(filled_params[j * 3])) + "), "
						+ toString(filled_params[j * 3 + 1]) + ", " + toString(filled_params[j * 3 + 2])
						+ " (min/mean/max: " + toString(min(x, j)) + ", " + toString(mean(x, j)) + ", "
						+ toString(max(x, j)) + ")");
			}
			j++;
		}
		writeCoefficientsFile(ratings, filled_params);
	}

	protected static double[] doOptimize(List<String> lines, double[] y, double[][] x) {
		System.out.println("Dimensions (x / y): " + x.length + "x" + x[0].length + " / " + y.length);
		double[] coeffs = getStartingPoint(x);
		validate(lines, x, y, coeffs);
		System.out.println("===================== START");

		coeffs = optimize(x, y, coeffs);

		System.out.println("===================== END");
		validate(lines, x, y, coeffs);
		return coeffs;
	}

	private static void writeCoefficientsFile(Path ratings, double[] filled_coeffs) {
		Path output = ratings.resolveSibling(ratings.getFileName().toString().replace("ratings", "coeffs"));
		try {
			Files.write(output, toString(filled_coeffs).getBytes());
		} catch (IOException e) {
			System.err.println("Could not write file: " + output);
			e.printStackTrace();
		}
	}

	private static double[] fillParameters(double[][] x, double[] parameters) {
		double[] start = getStartingPoint(x);
		double[] filled_params = new double[start.length];
		int omitted = 0;
		for (int j = 0; j < total_features; j++) {
			if (!omit_features.contains(j)) {
				filled_params[j * 3] = parameters[(j - omitted) * 3];
				filled_params[j * 3 + 1] = parameters[(j - omitted) * 3 + 1];
				filled_params[j * 3 + 2] = parameters[(j - omitted) * 3 + 2];
			} else {
				filled_params[j * 3] = start[j * 3];
				filled_params[j * 3 + 1] = start[j * 3 + 1];
				filled_params[j * 3 + 2] = start[j * 3 + 2];
				omitted++;
			}
		}
		return filled_params;
	}

	private static double[][] omitZeroFeatures(double[][] x) {
		omit_features = new LinkedList<>();
		for (int j = 0; j < total_features; j++) {
			boolean omit = true;
			for (double[] row : x) {
				if (row[j] > 0) {
					omit = false;
					break;
				}
			}
			if (omit) {
				omit_features.add(j);
			}
		}
		System.out.println("Omittted features: " + omit_features);

		double[][] newX = new double[x.length][total_features - omit_features.size()];
		for (int l = 0; l < x.length; l++) {
			int omitted = 0;
			for (int j = 0; j < total_features; j++) {
				if (!omit_features.contains(j)) {
					newX[l][j - omitted] = x[l][j];
				} else {
					omitted++;
				}
			}
		}
		return newX;
	}

	private static double[] linearRegression(double[][] x, double[] y) {
		OLSMultipleLinearRegression reg = new OLSMultipleLinearRegression();

		reg.setNoIntercept(true);
		reg.newSampleData(y, x);

		System.out.println("Coefficients: " + toString(reg.estimateRegressionParameters()));
		System.out.println("Residuals: " + toString(reg.estimateResiduals()));
		System.out.println("Squared sum: " + String.format("%.2f", reg.calculateResidualSumOfSquares()));
		return reg.estimateRegressionParameters();
	}

	private static double getProduct(double[] features, int k) {
		return features[FastMath.floorDiv(k, features.length)] * features[FastMath.floorMod(k, features.length)];
	}

	private static void validate(List<String> lines, double[][] x, double[] y, double[] coeffs) {
		System.out.println("coefficients: " + toString(coeffs));
		int i = 0;
		double sum = 0d;
		Map<String, Integer> classes = new HashMap<>();
		Map<String, Integer> matches = new HashMap<>();
		for (int l = 0; l < y.length; l++) {
			double predictedValue = computeValue(x[l], coeffs);
			double actualValue = y[l];
			double squaredError = (predictedValue - actualValue) * (predictedValue - actualValue);
			sum += squaredError;
			String predictedClass = getClass(predictedValue);
			String actualClass = getClass(actualValue);
			boolean match = predictedClass.equals(actualClass);
			addToMap(classes, actualClass);
			if (match) {
				addToMap(matches, actualClass);
			}
			if (i < 10) {
				System.out.println(lines.get(l).split(",")[0] + ": (" + toString(predictedValue) + " - "
						+ toString(actualValue) + ")^2: " + toString(squaredError) + " ; " + predictedClass + " "
						+ (match ? "=" : "!") + " " + actualClass);
				i++;
			}
		}
		System.out.println("Sum of squared errors: " + sum);
		System.out.println("Mean squared errors: " + (sum / y.length));
		System.out.println("RMS: " + FastMath.sqrt(sum / y.length));
		int totalMatches = 0;
		for (String cls : classes.keySet()) {
			int m = getMap(matches, cls);
			int n = getMap(classes, cls);
			totalMatches += m;
			System.out.println(cls + " matches: " + m + " / " + n + " = " + (((double) m) / n));
		}
		System.out.println(
				"Total matches: " + totalMatches + " / " + y.length + " = " + (((double) totalMatches) / y.length));
	}

	private static <T> void addToMap(Map<T, Integer> map, T key) {
		int current = getMap(map, key);
		map.put(key, current + 1);
	}

	private static <T> int getMap(Map<T, Integer> map, T key) {
		return map.getOrDefault(key, 0);
	}

	private static String getClass(double rating) {
		if (rating < 3.333d) {
			return "EASY";
		} else if (rating < 6.667d) {
			return "MEDIUM";
		} else {
			return "HARD";
		}
	}

	private static double min(double[][] x, int column) {
		double min = Double.MAX_VALUE;
		for (double[] row : x) {
			if (row[column] < min) {
				min = row[column];
			}
		}
		return min;
	}

	private static double mean(double[][] x, int column) {
		double sum = 0d;
		for (double[] row : x) {
			sum += row[column];
		}
		return sum / x.length;
	}

	private static double max(double[][] x, int column) {
		double max = Double.MIN_VALUE;
		for (double[] row : x) {
			if (row[column] > max) {
				max = row[column];
			}
		}
		return max;
	}

	private static double[] getStartingPoint(double[][] x) {
		int numFeatures = x[0].length;
		double[] startingPoint = new double[numFeatures * 3];
		for (int j = 0; j < numFeatures; j++) {
			startingPoint[j * 3] = FastMath.log(10d / numFeatures);
			startingPoint[j * 3 + 1] = 1d;
			startingPoint[j * 3 + 2] = mean(x, j);
		}
		return startingPoint;
	}

	private static double[] getExpectedDistanceFromStartingPoint(double[][] x) {
		int numFeatures = x[0].length;
		double[] expectedDistance = new double[numFeatures * 3];
		for (int j = 0; j < numFeatures; j++) {
			expectedDistance[j * 3] = FastMath.log(numFeatures / 2d);
			expectedDistance[j * 3 + 1] = 1d;
			expectedDistance[j * 3 + 2] = .75d * mean(x, j);
		}
		return expectedDistance;
	}

	private static double[] getLowerBounds(double[][] x) {
		int numFeatures = x[0].length;
		double[] lowerBounds = new double[numFeatures * 3];
		for (int j = 0; j < numFeatures; j++) {
			lowerBounds[j * 3] = -12d;
			lowerBounds[j * 3 + 1] = 0d;
			lowerBounds[j * 3 + 2] = 0d;
		}
		return lowerBounds;
	}

	private static double[] getUpperBounds(double[][] x) {
		int numFeatures = x[0].length;
		double[] upperBounds = new double[numFeatures * 3];
		for (int j = 0; j < numFeatures; j++) {
			upperBounds[j * 3] = FastMath.log(10d);
			upperBounds[j * 3 + 1] = 4d;
			upperBounds[j * 3 + 2] = 3d * mean(x, j);
		}
		return upperBounds;
	}

	private static double computeSummand(double[] features, int featureIndex, double[] parameters) {
		return FastMath.exp(parameters[featureIndex * 3]) / (1 + FastMath
				.exp(-parameters[featureIndex * 3 + 1] * (features[featureIndex] - parameters[featureIndex * 3 + 2])));
	}

	private static double computeValue(double[] features, double[] parameters) {
		double predictedValue = 0d;
		for (int j = 0; j < features.length; j++) {
			predictedValue += computeSummand(features, j, parameters);
		}
		return predictedValue;
	}

	private static double computeErrorTerm(double prediction, double observation) {
		if (prediction < -0.9d) {
			return -5.233d * prediction;
		} else if (prediction > 10.9d) {
			return 5.233d * prediction - 52.33d;
		} else if (prediction < observation) {
			return -FastMath.log((prediction + 1) / (observation + 1));
		} else if (prediction > observation) {
			return -FastMath.log((11 - prediction) / (11 - observation));
		} else {
			return 0d;
		}
	}

	private static double computeDerivativeOfErrorTerm(double prediction, double observation) {
		if (prediction < -0.9d) {
			return -5.233d;
		} else if (prediction > 10.9d) {
			return 5.233d;
		} else if (prediction < observation) {
			return -1 / (prediction + 1);
		} else if (prediction > observation) {
			return -1 / (11 - prediction);
		} else {
			return 0d;
		}
	}

	private static double computeJacobianEntry(double[] features, int j, int q, double summand, double[] parameters) {
		switch (q) {
		case 0:
			return summand;
		case 1:
			return -(1 - summand) * summand * (features[j] + parameters[j * 3 + 2]);
		case 2:
			return -(1 - summand) * summand * parameters[j * 3 + 1];
		default:
			throw new RuntimeException("Math broke down.");
		}
	}

	private static void forEachParameter(double[] features, double[] parameters,
										 BiConsumer<Integer, Double> consumer) {
		for (int j = 0; j < features.length; j++) {
			double summand = computeSummand(features, j, parameters);
			for (int q = 0; q < 3; q++) {
				consumer.accept(j * 3 + q, computeJacobianEntry(features, j, q, summand, parameters));
			}
		}
	}

	private static double[] optimize(double[][] x, double[] y, double[] startingPoint) {
		int numObservations = x.length;
		int numParameters = startingPoint.length;

		MultivariateFunction objective = point -> {
			double sumOfErrors = 0d;
			for (int l = 0; l < y.length; l++) {
				double[] features = x[l];
				double observation = y[l];
				double prediction = computeValue(features, point);
				sumOfErrors += computeErrorTerm(prediction, observation);
			}
			return sumOfErrors;
		};

		MultivariateVectorFunction gradient = point -> {
			double[] gradient1 = new double[numParameters];
			Arrays.fill(gradient1, 0d);
			for (int l = 0; l < numObservations; l++) {
				double[] features = x[l];
				double observation = y[l];
				double prediction = computeValue(features, point);
				double firstFactor = computeDerivativeOfErrorTerm(prediction, observation);
				forEachParameter(features, point, (p, jacEntry) -> gradient1[p] += firstFactor * jacEntry);
			}
			return gradient1;
		};


		ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(1.0e-20, 1.0e-20);
		CMAESOptimizer optimizer = new CMAESOptimizer(100000, 1e-3, true, 300, 100, new JDKRandomGenerator(), false,
				checker);
		PointValuePair optimum = optimizer.optimize(GoalType.MINIMIZE, new ObjectiveFunction(objective),
				new ObjectiveFunctionGradient(gradient), new InitialGuess(getStartingPoint(x)),
				new Sigma(getExpectedDistanceFromStartingPoint(x)),
				new SimpleBounds(getLowerBounds(x), getUpperBounds(x)), new MaxEval(1000000), new MaxIter(100000),
				new PopulationSize((int) FastMath.round(4 + 3 * FastMath.log(numParameters))));

		System.out.println("evaluations: " + optimizer.getEvaluations());
		System.out.println("iterations: " + optimizer.getIterations());

		return optimum.getPoint();
	}

	private static String toString(double[] values) {
		String[] strings = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			strings[i] = toString(values[i]);
		}
		return String.join(", ", strings);
	}

	private static String toString(double value) {
		return String.format(Locale.ENGLISH, "%.4f", value);
	}

}
