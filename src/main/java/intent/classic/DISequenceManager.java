package intent.classic;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.gml.GMLTokens;
import gml.DIGMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.tinkerpop.blueprints.util.io.gml.GMLReader.DEFAULT_LABEL;
import static gml.DIGMLReader.DEFAULT_BUFFER_SIZE;

class DISequenceManager {
    private List<ExtendedSequence> readFromSingleFile(ParseRussian parser, File file) throws IOException {
        List<ExtendedSequence> seqs = new ArrayList<>();
        TinkerGraph graph = new TinkerGraph();
        System.out.println("Start traversing file: " + file.toString());
        FileInputStream fis = new FileInputStream(file);
        DIGMLReader.inputGraph(graph, fis, DEFAULT_BUFFER_SIZE, DEFAULT_LABEL, GMLTokens.BLUEPRINTS_ID, GMLTokens.BLUEPRINTS_ID, null);
        fis.close();
        seqs.addAll(DIGraphUtils.traverse(graph, parser));
        System.out.println("Done traversing file: " + file.toString() + " with seq size: " + seqs.size());
        return seqs;
    }

    List<ExtendedSequence> readSequences(ParseRussian parser, String inputDirPath) throws IOException {
        List<ExtendedSequence> seqs = new ArrayList<>(100000);
        Files.walk(Paths.get(inputDirPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("gml"))
//                .limit(10)
                .forEach((f) -> {
                    try {
                        seqs.addAll(readFromSingleFile(parser, f.toFile()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return seqs;
    }

    void dumpSequences(String filePath, List<ExtendedSequence> sequences) throws IOException {
        PrintWriter writer = new PrintWriter(filePath + "/sequences.txt", "UTF-8");
        for (Sequence sequence : sequences) {
            writer.println(sequence.toString());
        }
        writer.close();
    }
}
