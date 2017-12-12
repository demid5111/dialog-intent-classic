package intent.classic;

import java.io.*;
import java.net.URL;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.gml.GMLReader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceStatsGenerator;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Predictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPT.CPTPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPTPlus.CPTPlusPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.DG.DGPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.LZ78.LZ78Predictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Markov.MarkovAllKPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Markov.MarkovFirstOrderPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.TDAG.TDAGPredictor;
import ca.pfv.spmf.test.MainTestPPM;
import com.tinkerpop.blueprints.util.io.gml.GMLTokens;
import gml.DIGMLReader;

import static com.tinkerpop.blueprints.util.io.gml.GMLReader.DEFAULT_LABEL;
import static gml.DIGMLReader.DEFAULT_BUFFER_SIZE;

public class Main {

    private static List<ExtendedSequence> sequences = new ArrayList<ExtendedSequence>();
    private static List<Sequence> testData = new ArrayList<Sequence>();
    private static List<Sequence> learningData = new ArrayList<Sequence>();

    public static void main(String[] args) throws Exception {

        final String algorithmTest = "DG";

        String russianAlphabet = "/Users/demidovs/Documents/Projects/dialog-intent-classic/src/main/resources/russian.txt";
        ParseRussian.readAlphabet(russianAlphabet);

        for (int i = 0; i < 5; i++) {
            TinkerGraph graph = new TinkerGraph();
            String sampleFileName = "/Users/demidovs/Documents/Projects/dialog-intent-classic/src/main/resources/graph";
            String fileExtension = ".gml";
            String filename = sampleFileName + (i + 1) + fileExtension;
            InputStream is = new FileInputStream(filename);
            BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            FileInputStream fis = new FileInputStream(filename);
            DIGMLReader.inputGraph(graph, fis, DEFAULT_BUFFER_SIZE, DEFAULT_LABEL, GMLTokens.BLUEPRINTS_ID, GMLTokens.BLUEPRINTS_ID, null);
            fis.close();
            traverse(graph);
            System.out.println("done traversing with seq size= " + sequences.size());
        }

        try {
            PrintWriter writer = new PrintWriter("out/sequences.txt", "UTF-8");
            for (Sequence sequence : sequences) {
                writer.println(sequence.toString());
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }

        splitData(sequences, 0.8);
        Predictor predictionModel = null;
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
        predictionModel.Train(learningData);
        System.out.println("done training");
        testData(predictionModel);

		/*
		 * System.out.println("Vertices of " + graph); for (Vertex vertex :
		 * graph.getVertices()) {
		 *
		 * String intent = vertex.getProperty("intent");
		 * System.out.println(intent); }
		 */
		/*
		 * System.out.println("Edges of " + graph); for (Edge edge :
		 * graph.getEdges()) { System.out.println(edge); }
		 */

		/*
		 * for (Vertex vertex : graph.getVertices()) {
		 * System.out.println(vertex); for(Edge e :
		 * vertex.getEdges(Direction.OUT)) { System.out.println(e); String
		 * intent = e.getVertex(Direction.OUT).getProperty("intent");
		 * System.out.println("intent:" + intent); } }
		 */
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

    private static void traverse(TinkerGraph graph) {
        for (Vertex vertex : graph.getVertices()) {
            String intent = vertex.getProperty("intent");
//            System.out.println("vertex:" + vertex + " intent:" + intent);
            ExtendedSequence seq = new ExtendedSequence(0);
            seq.addItem(new Item(0));
            travel(vertex, seq);
            if (seq.size() > 1) {
                sequences.add(seq);
            }
        }

    }

    static void testAlgorithm(Predictor predictionModel) {
        int rand = new Random().nextInt(testData.size());
        ExtendedSequence testSeq = (ExtendedSequence)testData.get(rand);
        ArrayList<Item> testItems = (ArrayList<Item>) testSeq.getItems();
        double precision;
        int hit = 0;
        int overall = 0;
        Sequence thePrediction = predictionModel.Predict(testSeq);
        System.out.println(
                "For the sequence " + testData.get(rand) + " the prediction for the next symbol is: " + thePrediction);

        for (Sequence seq : testData) {

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

    private static void testData(Predictor predictionModel) {
        // for all test data
        // need to get seq from 1 to n-1, save real n symbol, predict and get
        // hit
        int hitpoint = 0;
        int onesized = 0;
        int overall = 0;
        int skipped = 0;
        for (Sequence seq : testData) {
            if (seq.size() > 1) {
                // int seqRange = new Random().nextInt(seq.size() - 1) + 1;
                int realSeqRange = new Random().nextInt(seq.size()-1) +2;
                // System.out.println("rand :" + seqRange + " size:" +
                // seq.size());
                ArrayList<Item> currentItems = (ArrayList<Item>) seq.getItems();
                //int realSeqRange = seq.size();
                //int testSeqRange = seq.size() - 2;
                int testSeqRange = realSeqRange -1;

                if (testSeqRange > 0) {
                    overall++;
                    ExtendedSequence testSeq = new ExtendedSequence(0);

                    for (int i = 0; i < testSeqRange; i++) {
                        testSeq.addItem(currentItems.get(i));
                        testSeq.addMessageText(((ExtendedSequence)seq).getTextForSeqElement(i));
                        // added n-1 items to the new sequence
                        // now need to test it
                    }
                    Sequence thePrediction = predictionModel.Predict(testSeq);
                    Item predicted = thePrediction.get(0);
                    if (predicted.val == (currentItems.get(realSeqRange-1)).val) {
                        hitpoint++;
//                        System.out.println("For the sequence " + testSeq + " the prediction for the last symbol is: "
//                                + thePrediction);
//                        System.out.println("while last symbol:" + currentItems.get(realSeqRange-1).val);

                    } else {
//                        System.out.println("For the sequence " + testSeq + " the prediction for the last symbol is: "
//                                + thePrediction);
//                        System.out.println("while last symbol:" + currentItems.get(realSeqRange-1).val);
//
//                        System.out.println("on message:" + testSeq.getTextForSeqElement(testSeqRange-1)  + "  reply would be with :" + ParseRussian.getCharValue(predicted.val));
                    }
                } else {
                    System.out.println("test subsequence is too small, skip it ");
                    skipped++;
                }
            }
        }
        double hitratio = (double) hitpoint / overall;
        System.out.println("tested data, hited: " + hitpoint + " out of :" + overall + "with skipped:" + skipped);
        System.out.println("hit ratio:" + hitratio);
        System.out.println("test data size:" + testData.size());
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

    static void travel(Vertex vertex, ExtendedSequence seq) {
        String intentStr = vertex.getProperty("intent");

        if (intentStr.length() > 0) {
            char intentChar = intentStr.charAt(0);
            int value = ParseRussian.getIntValue(intentChar);

            seq.addItem(new Item((value)));
            seq.addMessageText(vertex.getProperty("text"));
        }
        for (Edge e : vertex.getEdges(Direction.OUT)) {
            // System.out.println(e);
            Vertex child = e.getVertex(Direction.IN);
            if (child == null) {
                System.out.println("end");
                return;
            } else {
                String intent = child.getProperty("intent");
                // System.out.println("child:" + child + " intent:" + intent);
                travel(child, seq);
            }
        }
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

    private static void splitData(List<ExtendedSequence> sequences, double range) {
        Random rand = new Random();
        for (ExtendedSequence seq : sequences) {
            double i = rand.nextDouble();
            if (i < range) {
                learningData.add(seq);
            } else {
                testData.add(seq);
            }
        }

        System.out.println("done splitting data. Learning = " + learningData.size() + " test = " + testData.size());
    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestPPM.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }

}