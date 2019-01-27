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
    static List<ExtendedSequence> traverse(TinkerGraph graph, ParseRussian parser, int minSequenceLength) {
        List<ExtendedSequence> currTreeSeqs = new ArrayList<>();

        Vertex root = DIGraphUtils.findRoot(graph);
        if (root == null) {
            System.out.println("Unable to find a root");
            return currTreeSeqs;
        }

        List<Vertex> duplicateVertices = DIGraphUtils.findDuplicateVertices(graph);
        duplicateVertices.forEach(graph::removeVertex);

        List<Vertex> noIntentVertices = DIGraphUtils.findNoIntentVertices(graph, parser);
        noIntentVertices.forEach(graph::removeVertex);

        List<Vertex> leafVertices = DIGraphUtils.findLeafVertices(graph);
        final Integer[] ids = {0};
        leafVertices
                .forEach((Vertex leaf) -> {
                    List<ExtendedSequence> paths = DIGraphUtils.allSimplePaths(root, leaf, ids[0],
                            parser, minSequenceLength);
                    if (!paths.isEmpty()) {
                        ids[0] += paths.size();
                        currTreeSeqs.addAll(paths);
                    }
                });

        return currTreeSeqs;
    }

    private static List<Vertex> findLeafVertices(TinkerGraph graph) {
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

    private static List<ExtendedSequence> allSimplePaths(Vertex from, Vertex to, int startId,
                                                         ParseRussian parser, int minSequenceLength) {
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
            if (newSeq.size() < minSequenceLength) {
                continue;
            }
            res.add(newSeq);
            startId += 1;
        }
        return res;
    }

    private static ExtendedSequence seqFromVertexPath(Object path, int id, ParseRussian parser) {
        ExtendedSequence resSeq = new ExtendedSequence(id);

        ((ArrayList<Vertex>) path)
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
                .filter(v -> ((String) v.getId()).matches("\\d*00\\d"))
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
        // it is not the true Vertex from BlueBrints
        // instead it is the TinkerVertex instance that has inEdges information
        // however the whole class is not exported (public) in the library
        // therefore we need to access this field with the Java Reflection mechanism
        return StreamSupport
                .stream(graph.getVertices().spliterator(), false)
                .filter((v) -> {
                    // it is not the true Vertex from BlueBrints
                    // instead it is the TinkerVertex instance that has inEdges information
                    // however the whole class is not exported (public) in the library
                    // therefore we need to access this field with the Java Reflection mechanism
                    try {
                        Object inEdges = reflectionSetAccessible(v.getClass(), "inEdges").get(v);
                        Object outEdges = reflectionSetAccessible(v.getClass(), "outEdges").get(v);
                        Object inEdgesValue = (((HashMap) inEdges).get("undefined"));
                        Object outEdgesValue = (((HashMap) outEdges).get("undefined"));
                        return inEdgesValue == null &&
                                outEdgesValue != null &&
                                ((HashSet) outEdgesValue).size() > 0;
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .max((Vertex a, Vertex b) -> {
                    Object firstOutEdges = null;
                    Object secondOutEdges = null;
                    try {
                        firstOutEdges = reflectionSetAccessible(a.getClass(), "outEdges").get(a);
                        secondOutEdges = reflectionSetAccessible(b.getClass(), "outEdges").get(b);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return -1;
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                        return -1;
                    }
                    int firstSize = ((HashSet) (((HashMap) firstOutEdges).get("undefined"))).size();
                    int secondSize = ((HashSet) (((HashMap) secondOutEdges).get("undefined"))).size();
                    return Integer.compare(firstSize, secondSize);
                })
                .orElse(null);
    }

    private static int getIntentCode(Vertex vertex, ParseRussian parser) {
        String intentStr = vertex.getProperty("intent");

        if (intentStr == null || intentStr.length() == 0) {
            return -1;
        }

        char intentChar = intentStr.charAt(0);
        try {
            return parser.getIntValue(intentChar);
        } catch (NullPointerException e) {
            return -1;
        }
    }
}
