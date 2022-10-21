package com.dat3m.dartagnan.solver.caat.misc;


import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;

import java.util.*;
import java.util.function.Predicate;

public class PathAlgorithm {

    //TODO: We need custom data datastructures that work with primitive integers
    private final static ThreadLocal<Queue<Integer>> queue1 = ThreadLocal.withInitial(ArrayDeque::new);
    private final static ThreadLocal<Queue<Integer>> queue2 = ThreadLocal.withInitial(ArrayDeque::new);

    private static final ThreadLocal<Edge[]> parentMap1 = ThreadLocal.withInitial(() -> new Edge[0]);
    private static final ThreadLocal<Edge[]> parentMap2 = ThreadLocal.withInitial(() -> new Edge[0]);

    public static void ensureCapacity(int capacity) {
        if (capacity <= parentMap1.get().length) {
            return;
        }

        final int newCapacity = capacity + 20;
        parentMap1.set(Arrays.copyOf(parentMap1.get(), newCapacity));
        parentMap2.set(Arrays.copyOf(parentMap2.get(), newCapacity));
    }


    /*
        This uses a bidirectional BFS to find a shortest path.
        A <filter> can be provided to skip certain edges during the search.
     */
    public static List<Edge> findShortestPath(RelationGraph graph, int start, int end,
                                              Predicate<Edge> filter) {
        queue1.get().clear();
        queue2.get().clear();

        Arrays.fill(parentMap1.get(), null);
        System.arraycopy(parentMap1.get(), 0, parentMap2.get(), 0, Math.min(parentMap1.get().length, parentMap2.get().length));

        queue1.get().add(start);
        queue2.get().add(end);
        boolean found = false;
        boolean doForwardBFS = true;
        int cur = -1;

        while (!found && (!queue1.get().isEmpty() || !queue2.get().isEmpty())) {
            if (doForwardBFS) {
                // Forward BFS
                int curSize = queue1.get().size();
                while (curSize-- > 0 && !found) {
                    for (Edge next : graph.outEdges(queue1.get().poll())) {
                        if (!filter.test(next)) {
                            continue;
                        }

                        cur = next.getSecond();

                        if (cur == end || parentMap2.get()[cur] != null) {
                            parentMap1.get()[cur] = next;
                            found = true;
                            break;
                        } else if (parentMap1.get()[cur] == null) {
                            parentMap1.get()[cur] = next;
                            queue1.get().add(cur);
                        }
                    }
                }
                doForwardBFS = false;
            } else {
                // Backward BFS
                int curSize = queue2.get().size();
                while (curSize-- > 0 && !found) {
                    for (Edge next : graph.inEdges(queue2.get().poll())) {
                        if (!filter.test(next)) {
                            continue;
                        }
                        cur = next.getFirst();

                        if (parentMap1.get()[cur] != null) {
                            parentMap2.get()[cur] = next;
                            found = true;
                            break;
                        } else if (parentMap2.get()[cur] == null) {
                            parentMap2.get()[cur] = next;
                            queue2.get().add(cur);
                        }
                    }
                }
                doForwardBFS = true;
            }
        }

        if (!found) {
            return Collections.emptyList();
        }

        LinkedList<Edge> path = new LinkedList<>();
        int e = cur;
        do {
            Edge backEdge = parentMap1.get()[e];
            path.addFirst(backEdge);
            e = backEdge.getFirst();
        } while (e != start);

        e = cur;
        while (e != end) {
            Edge forwardEdge = parentMap2.get()[e];
            path.addLast(forwardEdge);
            e = forwardEdge.getSecond();
        }

        return path;
    }




    // =============================== Public Methods ===============================

    public static List<Edge> findShortestPath(RelationGraph graph, int start, int end) {
        Predicate<Edge> alwaysTrueFilter = (edge -> true);
        return findShortestPath(graph, start, end, alwaysTrueFilter);
    }


    public static List<Edge> findShortestPath(RelationGraph graph, int start, int end, int derivationBound) {
        Predicate<Edge> filter = (edge -> edge.getDerivationLength() <= derivationBound);
        return findShortestPath(graph, start, end, filter);
    }
}
