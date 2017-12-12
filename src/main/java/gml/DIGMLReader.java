package gml;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.gml.GMLReader;
import com.tinkerpop.blueprints.util.io.gml.GMLTokens;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;

import java.io.*;
import java.nio.charset.Charset;

import static com.tinkerpop.blueprints.util.io.gml.GMLReader.DEFAULT_LABEL;

public class DIGMLReader {
    public static int DEFAULT_BUFFER_SIZE = 100;
    public static void inputGraph(final Graph inputGraph, final InputStream inputStream, final int bufferSize,
                                  final String defaultEdgeLabel, final String vertexIdKey, final String edgeIdKey,
                                  final String edgeLabelKey) throws IOException {
        final BatchGraph graph = BatchGraph.wrap(inputGraph, bufferSize);

        final Reader r = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        final StreamTokenizer st = new StreamTokenizer(r);

        try {
            st.commentChar(GMLTokens.COMMENT_CHAR);
            st.ordinaryChar('[');
            st.ordinaryChar(']');

            final String stringCharacters = "/\\(){}<>!£$%^&*-+=,.?:;@_`|~";
            for (int i = 0; i < stringCharacters.length(); i++) {
                st.wordChars(stringCharacters.charAt(i), stringCharacters.charAt(i));
            }

            new DIGMLParser(graph, defaultEdgeLabel, vertexIdKey, edgeIdKey, edgeLabelKey).parse(st);

            graph.commit();

        } catch (IOException e) {
            throw new IOException("GML malformed line number " + st.lineno() + ": ", e);
        } finally {
            r.close();
        }
    }
}
