package intent.classic;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class DIGraphUtils {
    static List<ExtendedSequence> traverse(TinkerGraph graph, ParseRussian parser) {
        List<ExtendedSequence> currTreeSeqs = new ArrayList<>();

        Vertex root = DIGraphUtils.findRoot(graph);

        List<Vertex> leafVertices = DIGraphUtils.keepLeafVertices(graph);
        List<Vertex> duplicateVertices = DIGraphUtils.findDuplicateVertices(graph);
        duplicateVertices.forEach(graph::removeVertex);

        List<Vertex> noIntentVertices = DIGraphUtils.findNoIntentVertices(graph, parser);
        noIntentVertices.forEach(graph::removeVertex);

        final Integer[] ids = {0};
        leafVertices
                .forEach((Vertex leaf) -> {
                    List<ExtendedSequence> paths = DIGraphUtils.allSimplePaths(root, leaf, ids[0], parser);
                    if (!paths.isEmpty()) {
                        ids[0] += paths.size();
                        currTreeSeqs.addAll(paths);
                    }
                });

        return currTreeSeqs;
    }

    private static List<Vertex> keepLeafVertices(TinkerGraph graph) {
        return StreamSupport
                .stream(graph.getVertices().spliterator(), false)
                .filter((v) -> {
                    try {
                        Object value = reflectionSetAccessible(v.getClass(), "outEdges").get(v);
                        return (((HashMap) value).get("undefined")) == null;
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                        return false;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private static List<ExtendedSequence> allSimplePaths(Vertex from, Vertex to, int startId, ParseRussian parser) {
        final GremlinPipeline pipe = new GremlinPipeline(from)
                .as("source")
                .both()
                .loop("source",
                        new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                            @Override
                            public Boolean compute(LoopPipe.LoopBundle<Vertex> lb) {
                                return lb.getLoops() < 6 && lb.getObject() != to;
                            }
                        },
                        new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                            @Override
                            public Boolean compute(LoopPipe.LoopBundle<Vertex> lb) {
                                return true;
                            }
                        }).has("id", to.getId()).simplePath().path();

        List<ExtendedSequence> res = new ArrayList<>();

        for (Object path : pipe) {
            ExtendedSequence newSeq = DIGraphUtils.seqFromVertexPath(path, startId, parser);
            if (newSeq.size() <= 2){
                continue;
            }
            res.add(newSeq);
            startId += 1;
        }
        return res;
    }

    private static ExtendedSequence seqFromVertexPath(Object path, int id, ParseRussian parser){
        ExtendedSequence resSeq = new ExtendedSequence(id);

        ((ArrayList<Vertex>)path)
                .stream()
                .skip(1)
                .map(v -> DIGraphUtils.getIntentCode(v, parser))
                .forEach(s -> {
                    resSeq.addItem(new Item(s));
                });

        return resSeq;
    }

    private static List<Vertex> findDuplicateVertices(TinkerGraph graph) {
        return StreamSupport
                .stream(graph.getVertices().spliterator(), false)
                .filter(v -> ((String)v.getId()).matches("\\d*00\\d"))
                .collect(Collectors.toList());
    }

    private static List<Vertex> findNoIntentVertices(TinkerGraph graph, ParseRussian parser) {
        Vertex root = DIGraphUtils.findRoot(graph);

        return StreamSupport
                .stream(graph.getVertices().spliterator(), false)
                .filter(v -> getIntentCode(v, parser) == -1 && v != root)
                .collect(Collectors.toList());
    }

    private static Field reflectionSetAccessible(Class cls, String name) throws NoSuchFieldException {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Vertex findRoot(TinkerGraph graph) {
        return StreamSupport
                .stream(graph.getVertices().spliterator(), false)
                .filter((v) -> {
                    // it is not the true Vertex from BlueBrints
                    // instead it is the TinkerVertex instance that has inEdges information
                    // however the whole class is not exported (public) in the library
                    // therefore we need to access this field with the Java Reflection mechanism
                    try {
                        Object value = reflectionSetAccessible(v.getClass(), "inEdges").get(v);
                        return (((HashMap) value).get("undefined")) == null;
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    private static int getIntentCode(Vertex vertex, ParseRussian parser) {
        String intentStr = vertex.getProperty("intent");

        if (intentStr == null || intentStr.length() == 0){
            return -1;
        }

        char intentChar = intentStr.charAt(0);
        return parser.getIntValue(intentChar);
    }
}
