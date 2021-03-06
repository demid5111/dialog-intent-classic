package intent.classic;

import java.util.ArrayList;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;

public class ExtendedSequence extends Sequence {

    private ArrayList<String> textMessages;

    public ExtendedSequence(int id) {
        super(id);

    }

    public void addMessageText(String text) {
        if (textMessages == null) {
            textMessages = new ArrayList<>();
        }
        textMessages.add(text);
    }

    public String getTextForSeqElement(int id) {
        return textMessages.get(id);
    }


}