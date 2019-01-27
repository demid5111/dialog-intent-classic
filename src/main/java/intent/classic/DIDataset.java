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

    DIDataset(List<ExtendedSequence> sequences) {
        this.sequences = sequences;
        testData = new ArrayList<>();
        learningData = new ArrayList<>();
    }

    void splitDataset(double range) {
        Random rand = new Random();
        this.testData = new ArrayList<>();
        this.learningData = new ArrayList<>();
        final int totalNumber = this.sequences.size();
        final int testDataSize = (int) Math.ceil((1 - range) * totalNumber);
        List<Integer> testIndexes = new ArrayList<>();
        for (; ; ) {
            int index = rand.nextInt(totalNumber);
            if (!testIndexes.contains(index)) {
                testIndexes.add(index);
            }
            if (testIndexes.size() >= testDataSize) {
                break;
            }
        }

        for (int i = 0; i < totalNumber; i++) {
            if (testIndexes.contains(i)) {
                this.testData.add(this.sequences.get(i));
            } else {
                this.learningData.add(this.sequences.get(i));
            }
        }

        System.out.println("Done splitting data. Learning = " + learningData.size() + " test = " + testData.size());
    }
}
