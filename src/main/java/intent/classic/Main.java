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

        ParseRussian alphabetParser = new ParseRussian(isGeneralUsed.equals("True"));
        alphabetParser.readAlphabet("russian.txt");

        DISequenceManager sequenceManager = new DISequenceManager();
        List<ExtendedSequence> sequences = sequenceManager.readSequences(alphabetParser, inputDirPath);
        sequenceManager.dumpSequences(outputDirPath, sequences);

        DIDataset dataset = new DIDataset(sequences);
        dataset.splitDataset(0.8);

        final String algorithmTest = "DG";
        Predictor predictionModel;
        String optionalParameters;
        switch (algorithmTest) {
            case "PPM":
                predictionModel = new MarkovFirstOrderPredictor("PPM");
                break;
            case "CPTPlus":
                optionalParameters = "CCF:true CBS:true CCFmin:1 CCFmax:3 CCFsup:2 splitMethod:0 minPredictionRatio:0.5 noiseRatio:1.0";
                predictionModel = new CPTPlusPredictor("CPT+", optionalParameters);
                break;

            case "CPT":
                optionalParameters = "splitLength:6 recursiveDividerMin:1 recursiveDividerMax:5";
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
                return;
        }
        System.out.println("Using " + algorithmTest + " to predict");
        predictionModel.Train(dataset.getLearningData());
        System.out.println("done training");
        testData(predictionModel, dataset);

        // PPMAlgr();

		/*
		 * traverse(graph); System.out.println("done traversing");
		 */
        //
//         MarkovFirstOrderPredictor predictionModel = new
//         MarkovFirstOrderPredictor("PPM");
//         predictionModel.Train(sequences);
//         Sequence sequence = new Sequence(0);
//         sequence.addItem(new Item(7));
//         sequence.addItem(new Item(7));
//         sequence.addItem(new Item(12));
        // // Then we perform the prediction
        // Sequence thePrediction = predictionModel.Predict(sequence);
        // System.out.println("For the sequence <(3),(14), (14)>, the prediction
        // for the next symbol is: +" + thePrediction);
        // System.out.println("vertex " + a.getId() + " has name " +
        // a.getProperty("name"));
    }



    static void testAlgorithm(Predictor predictionModel, DIDataset dataset) {
        int rand = new Random().nextInt(dataset.getTestData().size());
        ExtendedSequence testSeq = (ExtendedSequence) dataset.getTestData().get(rand);
        ArrayList<Item> testItems = (ArrayList<Item>) testSeq.getItems();
        double precision;
        int hit = 0;
        int overall = 0;
        Sequence thePrediction = predictionModel.Predict(testSeq);
        System.out.println(
                "For the sequence " + dataset.getTestData().get(rand) + " the prediction for the next symbol is: " + thePrediction);

        for (Sequence seq : dataset.getTestData()) {

            if (findSubsequence(seq.getItems(), testSeq.getItems())) {
                overall++;
                for (Item it : thePrediction.getItems()) {
                    testSeq.addItem(it);
                }
                if (findSubsequence(seq.getItems(), testSeq.getItems())) {
                    hit++;
                }
            }
        }
        System.out.println("found subsequences:" + overall + " and hited: " + hit);

    }

    private static void testData(Predictor predictionModel, DIDataset dataset) {
        // for all test data
        // need to get seq from 1 to n-1, save real n symbol, predict and get
        // hit
        int hitpoint = 0;
        int onesized = 0;
        int overall = 0;
        int skipped = 0;
        for (Sequence seq : dataset.getTestData()) {
            if (seq.size() > 1) {
                int realSeqRange = new Random().nextInt(seq.size() - 1) + 2;
                ArrayList<Item> currentItems = (ArrayList<Item>) seq.getItems();
                int testSeqRange = realSeqRange - 1;

                if (testSeqRange > 0) {
                    overall++;
                    ExtendedSequence testSeq = new ExtendedSequence(0);

                    for (int i = 0; i < testSeqRange; i++) {
                        testSeq.addItem(currentItems.get(i));
                        // added n-1 items to the new sequence
                        // now need to test it
                    }

                    Sequence thePrediction = predictionModel.Predict(testSeq);
                    Item predicted = thePrediction.get(0);
                    if (predicted.val == (currentItems.get(realSeqRange - 1)).val) {
                        hitpoint++;
                        System.out.println("For the sequence " + testSeq + " the prediction for the last symbol is: "
                                + thePrediction);
                        System.out.println("while last symbol:" + currentItems.get(realSeqRange-1).val);

                    } else {
                        System.out.println("For the sequence " + testSeq + " the prediction for the last symbol is: "
                                + thePrediction);
                        System.out.println("while last symbol:" + currentItems.get(realSeqRange-1).val);
                    }
                } else {
                    System.out.println("test subsequence is too small, skip it ");
                    skipped++;
                }
            }
        }

        double hitratio = (double) hitpoint / overall;

        System.out.println("tested data, hit: " + hitpoint + " out of: " + overall + " with skipped: " + skipped);
        System.out.println("hit ratio:" + hitratio);
        System.out.println("test data size:" + dataset.getTestData().size());
    }

    private static boolean findSubsequence(List<Item> input, List<Item> seq) {
        for (Item item : input) {
            for (Item it2 : seq) {
                if (!item.equals(it2)) {
                    return false;
                }
            }
        }
        return true;
    }



	/*
	 * static void PPMAlgr() throws IOException{ // Load the set of training
	 * sequences String inputPath = fileToPath("BIBLE.txt"); SequenceDatabase
	 * trainingSet = new SequenceDatabase();
	 * trainingSet.loadFileSPMFFormat(inputPath, Integer.MAX_VALUE, 0,
	 * Integer.MAX_VALUE);
	 *
	 * // Print the training sequences to the console
	 * System.out.println("--- Training sequences ---"); for(Sequence sequence :
	 * trainingSet.getSequences()) { System.out.println(sequence.toString()); }
	 * System.out.println();
	 *
	 * // Print statistics about the training sequences
	 * SequenceStatsGenerator.prinStats(trainingSet, " training sequences ");
	 *
	 * // Train the prediction model MarkovFirstOrderPredictor predictionModel =
	 * new MarkovFirstOrderPredictor("PPM");
	 * predictionModel.Train(trainingSet.getSequences());
	 *
	 * // Now we will make a prediction. // We want to predict what would occur
	 * after the sequence <1, 3>. // We first create the sequence Sequence
	 * sequence = new Sequence(0); sequence.addItem(new Item(160));
	 * sequence.addItem(new Item(662)); sequence.addItem(new Item(663)); // Then
	 * we perform the prediction Sequence thePrediction =
	 * predictionModel.Predict(sequence); System.out.
	 * println("For the sequence <(1),(4)>, the prediction for the next symbol is: +"
	 * + thePrediction); }
	 */



    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestPPM.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }

}