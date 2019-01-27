package intent.classic;

import java.io.*;
import java.net.URL;

import java.util.*;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Predictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPT.CPTPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPTPlus.CPTPlusPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.DG.DGPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.LZ78.LZ78Predictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Markov.MarkovAllKPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Markov.MarkovFirstOrderPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.TDAG.TDAGPredictor;
import ca.pfv.spmf.test.MainTestPPM;

import org.apache.commons.cli.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Options options = DICLIUtils.getCLIOptions();
        CommandLine cmd = DICLIUtils.parseCLIArgs(args, options);

        String inputDirPath = cmd.getOptionValue("input-dir");
        String outputDirPath = cmd.getOptionValue("output-dir");
        String algosNames = cmd.getOptionValue("algorithms");
        String isGeneralUsed = cmd.getOptionValue("generalize-types");
        int numberRuns = Integer.valueOf(cmd.getOptionValue("number-runs"));

        HashMap<String, HashMap<String, Object>> models = initializeModels(algosNames);

        if (models == null) {
            System.out.println("Unable to initialize models");
            return;
        }

        ParseRussian alphabetParser = new ParseRussian(isGeneralUsed.equals("True"));
        alphabetParser.readAlphabet("russian.txt");

        DISequenceManager sequenceManager = new DISequenceManager();
        List<ExtendedSequence> sequences = sequenceManager.readSequences(alphabetParser, inputDirPath);
        sequenceManager.dumpSequences(outputDirPath, sequences);

        DIDataset dataset = new DIDataset(sequences);

        collectPerformanceData(models, dataset, numberRuns, 0.8);

        printReport(models);
    }

    private static void collectPerformanceData(HashMap<String, HashMap<String, Object>> models,
                                               DIDataset dataset,
                                               int numberRuns,
                                               double trainingDataRatio) {
        for (Map.Entry<String, HashMap<String, Object>> pair : models.entrySet()) {
            final String algorithmName = pair.getKey();
            final Predictor predictionModel = (Predictor) pair.getValue().get("model");

            double total = 0.;
            double bestResult = 0.;
            for (int i = 0; i < numberRuns; i++) {
                dataset.splitDataset(trainingDataRatio);

                System.out.println("Training: " + algorithmName);
                predictionModel.Train(dataset.getLearningData());
                System.out.println("Done training");

                System.out.println("Inference started: " + algorithmName);
                final double currentResult = testData(predictionModel, dataset);
                total += currentResult;
                if (currentResult > bestResult){
                    bestResult = currentResult;
                }
            }

            double averageAccuracy = total / numberRuns;
            models.get(algorithmName).put("accuracy", averageAccuracy);
            models.get(algorithmName).put("max_accuracy", bestResult);

            System.out.println("Done inference: " + algorithmName +
                    " Accuracy: " + averageAccuracy +
                    " Max Accuracy: " + bestResult);
        }
    }

    private static void printReport(HashMap<String, HashMap<String, Object>> models){
        for (Map.Entry<String, HashMap<String, Object>> pair : models.entrySet()) {
            System.out.println("Model: " + pair.getKey());
            System.out.println("\tParameters: " + pair.getValue().get("parameters"));
            System.out.println("\tAccuracy: " + pair.getValue().get("accuracy"));
            System.out.println("\tMaximum accuracy: " + pair.getValue().get("max_accuracy"));
        }
    }

    private static HashMap<String, HashMap<String, Object>> initializeModels(String algosNames) {
        HashMap<String, HashMap<String, Object>> models = new HashMap<>();
        for (String algorithmTest : algosNames.split(",")) {
            Predictor predictionModel;
            String optionalParameters = "";
            switch (algorithmTest) {
                case "PPM":
                    predictionModel = new MarkovFirstOrderPredictor("PPM");
                    break;
                case "CPTPlus":
                    optionalParameters = "CCF:true CBS:true CCFmin:1 CCFmax:6 CCFsup:2 splitMethod:0 splitLength:4 minPredictionRatio:1.0 noiseRatio:1.0";
                    predictionModel = new CPTPlusPredictor("CPT+", optionalParameters);
                    break;

                case "CPT":
                    optionalParameters = "splitLength:6 splitMethod:0 recursiveDividerMin:1 recursiveDividerMax:5";
                    predictionModel = new CPTPredictor("CPT", optionalParameters);
                    break;

                case "DG":
                    optionalParameters = "lookahead:2";
                    predictionModel = new DGPredictor("DG", optionalParameters);
                    break;

                case "AKOM":
                    optionalParameters = "order:4";
                    predictionModel = new MarkovAllKPredictor("AKOM", optionalParameters);
                    break;
                case "TDAG":
                    predictionModel = new TDAGPredictor("TDAG");
                    break;

                case "LZ78":
                    predictionModel = new LZ78Predictor("LZ78");
                    break;

                default:
                    System.out.println("Wrong argument...");
                    return null;
            }

            HashMap<String, Object> info = new HashMap<>();
            info.put("model", predictionModel);
            info.put("accuracy", 0.);
            info.put("max_accuracy", 0.);
            info.put("parameters", optionalParameters);

            models.put(algorithmTest, info);
        }
        return models;
    }

    private static double testData(Predictor predictionModel, DIDataset dataset) {
        // for all test data
        // need to get seq from 1 to n-1, save real n symbol, predict and get
        // hit
        int hitpoint = 0;
        int skipped = 0;
        int noSingleWinner = 0;
        for (Sequence seq : dataset.getTestData()) {
            if (seq.size() <= 1) {
                System.out.println("Too small sequence. Skipping");
                skipped++;
            }

            int lastIndex = seq.size() - 1;
            ArrayList<Item> currentItems = (ArrayList<Item>) seq.getItems();

            ExtendedSequence testSeq = new ExtendedSequence(0);
            seq.getItems()
                    .stream()
                    .limit(lastIndex) // added n-1 items to the new sequence
                    .forEach(testSeq::addItem);

            // now need to test it
            Sequence thePrediction = predictionModel.Predict(testSeq);

            Integer expectedLastElementValue = currentItems.get(lastIndex).val;
            Integer predictedLastElementValue;

            if (thePrediction.getItems().size() == 0){
//                System.out.println("No particular winner. Select it ourselves. Works only for CPTPlus");

                Map<Integer, Float> countTable;
                try {
                    countTable = ((CPTPlusPredictor) predictionModel).getCountTable();
                } catch (Exception e){
                    noSingleWinner += 1;
                    continue;
                }

//                for (Map.Entry<Integer, Float> integerFloatEntry : countTable.entrySet()) {
//                    System.out.println("symbol" + ((Map.Entry) integerFloatEntry).getKey() +
//                            "\t score: " + ((Map.Entry) integerFloatEntry).getValue());
//                }

                List<Integer> symbolKeys = new ArrayList<>(countTable.keySet());
                symbolKeys.sort((keyOne, keyTwo) -> {
                    float valueOne = countTable.get(keyOne);
                    float valueTwo = countTable.get(keyTwo);
                    return Float.compare(valueOne, valueTwo);
                });

                predictedLastElementValue = symbolKeys.get(symbolKeys.size() - 1);
            } else {
                Item predicted = thePrediction.get(0);
                predictedLastElementValue = predicted.val;
            }

            if (predictedLastElementValue.equals(expectedLastElementValue)) {
                hitpoint++;
            }
//            else {
//                System.out.println("For the sequence " + testSeq + " the prediction for the last symbol is: "
//                        + predictedLastElementValue);
//                System.out.println("while last symbol:" + expectedLastElementValue);
//            }
        }

        final int overall = dataset.getTestData().size() - skipped - noSingleWinner;

        return  (double) hitpoint / overall;
    }
}