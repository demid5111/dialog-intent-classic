package intent.classic;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DIDataset {
    private List<ExtendedSequence> sequences;

    public List<Sequence> getTestData() {
        return testData;
    }

    public List<Sequence> getLearningData() {
        return learningData;
    }

    private List<Sequence> testData;
    private List<Sequence> learningData;

    DIDataset(List<ExtendedSequence> sequences){
        this.sequences = sequences;
        testData = new ArrayList<>();
        learningData = new ArrayList<>();
    }

    void splitDataset(double range) {
        Random rand = new Random();
        for (ExtendedSequence seq : this.sequences) {
            double i = rand.nextDouble();
            if (i < range) {
                learningData.add(seq);
            } else {
                testData.add(seq);
            }
        }

        System.out.println("Done splitting data. Learning = " + learningData.size() + " test = " + testData.size());
    }
}
