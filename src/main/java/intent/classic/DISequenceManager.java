package intent.classic;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.gml.GMLTokens;
import com.tinkerpop.blueprints.util.io.gml.GMLWriter;
import gml.DIGMLReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.tinkerpop.blueprints.util.io.gml.GMLReader.DEFAULT_LABEL;
import static gml.DIGMLReader.DEFAULT_BUFFER_SIZE;

class DISequenceManager {

    private final int maxNumberFiles;
    private final int minSequenceLength;

    DISequenceManager(int maxNumberFiles, int minSequenceLength) {
        this.maxNumberFiles = maxNumberFiles;
        this.minSequenceLength = minSequenceLength;
    }

    private List<ExtendedSequence> readFromSingleFile(ParseRussian parser, File file) throws IOException {
        TinkerGraph graph = new TinkerGraph();
        System.out.println("Start traversing file: " + file.toString());
        FileInputStream fis = new FileInputStream(file);
        DIGMLReader.inputGraph(graph, fis, DEFAULT_BUFFER_SIZE, DEFAULT_LABEL, GMLTokens.BLUEPRINTS_ID, GMLTokens.BLUEPRINTS_ID, null);
        fis.close();
        List<ExtendedSequence> seqs = new ArrayList<>(DIGraphUtils.traverse(graph, parser, this.minSequenceLength));
        System.out.println("Done traversing file: " + file.toString() + " with seq size: " + seqs.size());
        return seqs;
    }

    List<ExtendedSequence> readSequences(ParseRussian parser, String inputDirPath) throws IOException {
        List<ExtendedSequence> seqs = new ArrayList<>(100000);
        Stream<Path> allFilesStream = Files.walk(Paths.get(inputDirPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("gml"));
        if (this.maxNumberFiles != -1) {
            allFilesStream = allFilesStream.limit(this.maxNumberFiles);
        }

        allFilesStream
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
