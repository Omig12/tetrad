package edu.cmu.tetrad.data;

import com.google.common.primitives.Ints;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for the generation of datasets with selection bias and missing values.
 *
 * @author Joseph Ramsey
 */

public class SelectionBias {

    private final Graph trueGraph;
    private final int biasedEdges;
    public final Graph biasGraph;
    @SuppressWarnings("FieldCanBeLocal")
    private List<Node> trueNodes;
    @SuppressWarnings("FieldCanBeLocal")
    private List<Node> biasNodes;
    private DataSet trueData;
    private DataSet biasData;

    public SelectionBias(Graph graph, int biasedEdges) {
        this.trueGraph = new EdgeListGraph(graph);
        this.biasedEdges = biasedEdges;
        this.biasGraph = biasGraph();
    }

    // Graph with selection variables for missingness simulation
    private Graph biasGraph() {
        Graph graph = new EdgeListGraph(this.trueGraph);
        this.trueNodes = graph.getNodes();
        this.biasNodes = new ArrayList<>();
        // Add biasNodes to biasGraph
        for (Node node : this.trueNodes) {
            Node u = new DiscreteVariable("U" + node.getName(), 2);
            biasNodes.add(u);
            graph.addNode(u);
        }
        // Add random edges with conditions Xi -> Uj, Xi non-descendant of Xj;
        int maxCount = trueNodes.size() * biasNodes.size();
        int count = 0;
        for (int t = 0; t < this.biasedEdges; t++) {
            if (count > maxCount) {
                break;
            }
            count++;
            int m = RandomUtil.getInstance().nextInt(trueNodes.size());
            int n = RandomUtil.getInstance().nextInt(trueNodes.size());
            if (m == n) {
                t--;
                continue;
            }
            if (graph.isDescendentOf(trueNodes.get(m), trueNodes.get(n))) {
                t--;
                continue;
            }
            graph.addDirectedEdge(trueNodes.get(m), biasNodes.get(n));
        }
        return graph;
    }

    // Test-Wise removal random missing variables allowed
    public DataSet BiasDataCell(DataSet dataSet) {
        this.trueData = dataSet;
        this.biasData = dataSet.copy();
        int vars = (biasData.getNumColumns() / 2);
        for (int i = 0; i < biasData.getNumRows(); i++) {
            for (int j = 0; j < vars; j++) {
                if (biasData.getInt(i, j + vars) == 0) {
                    biasData.setInt(i, j, -99);
                }
            }
        }
        int[] cols = new int[vars];
        for (int i = 0; i < vars; i++) {
            cols[i] = (i + vars);
        }
        biasData.removeCols(cols);
        return biasData;
    }

    // Row-Wise removal random missing variables allowed
    public DataSet BiasDataRow(DataSet dataSet) {
        this.trueData = dataSet;
        this.biasData = BiasDataCell(dataSet.copy());
        List<Integer> removed = new ArrayList<>();
        for (int i = 0; i < biasData.getNumRows(); i++) {
            for (int j = 0; j < biasData.getNumColumns(); j++) {
                if (!removed.contains(i)) {
                    if (biasData.getInt(i, j) == -99) {
                        removed.add(i);
                    }
                }
            }
        }
        biasData.removeRows(Ints.toArray(removed));
        return biasData;
    }

    // Test-Wise removal random missing variables not allowed
    public DataSet BiasDataCellAlt(DataSet dataSet) {
        this.trueData = dataSet;
        this.biasData = dataSet.copy();
        System.out.println("True dataSet: " + this.trueData);
        Graph graph = new EdgeListGraph(this.biasGraph);
        graph.removeEdges(this.trueGraph.getEdges());
        int vars = (biasData.getNumColumns() / 2);
        for (int i = 0; i < biasData.getNumRows(); i++) {
            for (int j = 0; j < vars; j++) {
                String name = biasData.getVariable(j + vars).getName();
                Node node = graph.getNode(name);
                if (biasData.getInt(i, j + vars) == 0 && graph.getIndegree(node) > 0) {
                    biasData.setInt(i, j, -99);
                }
            }
        }
        int[] cols = new int[vars];
        for (int i = 0; i < vars; i++) {
            cols[i] = (i + vars);
        }
        biasData.removeCols(cols);
        return biasData;
    }

    // Row-Wise removal random missing variables not allowed
    public DataSet BiasDataRowAlt(DataSet dataSet) {
        this.trueData = dataSet;
        this.biasData = BiasDataCellAlt(dataSet.copy());
        List<Integer> removed = new ArrayList<>();
        for (int i = 0; i < biasData.getNumRows(); i++) {
            for (int j = 0; j < biasData.getNumColumns(); j++) {
                if (!removed.contains(i)) {
                    if (biasData.getInt(i, j) == -99) {
                        removed.add(i);
                    }
                }
            }
        }
        biasData.removeRows(Ints.toArray(removed));
        return biasData;
    }
}

//        public static DataSet getRow(int i) {
//            List<Object> row = new ArrayList<>();
//            for (int j = 0; j < dataSet.getNumColumns(); j++) {
//                row.add(dataSet.getObject(i, j));
//            }
//            return (DataSet) row;
//        }