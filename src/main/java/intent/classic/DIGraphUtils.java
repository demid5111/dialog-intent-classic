package intent.classic;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import java.util.ArrayList;
import java.util.List;

class DIGraphUtils {
    static List<ExtendedSequence> traverse(TinkerGraph graph, ParseRussian parser) {
        List<ExtendedSequence> currTreeSeqs = new ArrayList<>();
        for (Vertex vertex : graph.getVertices()) {
//            String intent = vertex.getProperty("intent");
//            System.out.println("vertex:" + vertex + " intent:" + intent);
            ExtendedSequence seq = new ExtendedSequence(0);
            seq.addItem(new Item(0));
            travel(vertex, seq, parser);
            if (seq.size() > 1) {
                currTreeSeqs.add(seq);
            }
        }
        return currTreeSeqs;
    }

    private static void travel(Vertex vertex, ExtendedSequence seq, ParseRussian parser) {
        String intentStr = vertex.getProperty("intent");

        if (intentStr.length() > 0) {
            char intentChar = intentStr.charAt(0);
            int value = parser.getIntValue(intentChar);

            seq.addItem(new Item((value)));
//            seq.addMessageText(vertex.getProperty("text"));
        }
        for (Edge e : vertex.getEdges(Direction.OUT)) {
            // System.out.println(e);
            Vertex child = e.getVertex(Direction.IN);
            if (child == null) {
                System.out.println("end");
                return;
            } else {
//                String intent = child.getProperty("intent");
//                 System.out.println("child:" + child + " intent:" + intent);
                travel(child, seq, parser);
            }
        }
    }
}
