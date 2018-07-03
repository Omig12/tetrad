package edu.cmu.tetrad.data;

import com.google.common.primitives.Ints;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;

public class SelectionBias {

    public Graph trueGraph;
    public int biasedEdges;
    public Graph biasGraph;
    public List<Node> trueNodes;
    public List<Node> biasNodes;
    public DataSet trueData;
    public DataSet biasData;

    public SelectionBias(Graph graph, int biasedEdges) {
        this.trueGraph = new EdgeListGraph(graph);
        this.biasedEdges = biasedEdges;
        this.biasGraph = BiasGraph();
    }

    public Graph BiasGraph() {

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

        // Joe's code
        for (int t = 0; t < this.biasedEdges; t++) {
            if (count > maxCount) break;
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

    public DataSet BiasDataCell(DataSet dataSet) {

        this.trueData = dataSet;
        this.biasData = dataSet.copy();
//        System.out.println("True dataSet: " + this.trueData);


        int vars = (biasData.getNumColumns()/2);
        for (int i = 0; i < biasData.getNumRows(); i++) {
            for (int j = 0; j < vars; j++) {
                if (biasData.getInt(i, j + vars) == 0) biasData.setInt(i, j, -99);
//                System.out.println("For: x(" + i + "," + j + ") = "+ biasData.getInt(i,j) + " U(" + i + "," + (j+vars) +") =" + biasData.getInt(i,j+vars) + "\n" );

//                System.out.println(biasData);
            }
        }

        int[] cols = new int[vars];
        for (int i = 0; i < vars; i++) cols[i] = (i+vars);

        biasData.removeCols(cols);

//        System.out.println("Bias dataSet: " + biasData);

        return biasData;
    }

    public DataSet BiasDataRow(DataSet dataSet) {

        this.trueData = dataSet;
        this.biasData = BiasDataCell(dataSet.copy());
//        System.out.println("Pre-dataSet: " + trueData);

        List<Integer> removed = new ArrayList<>();

        for (int i = 0; i < biasData.getNumRows(); i++) {
                for (int j = 0; j < biasData.getNumColumns(); j++) {
//                    System.out.println("For: x(" + i + "," + j + ") = " + biasData.getInt(i, j) + "\n");
                    if (!removed.contains(i))   if (biasData.getInt(i, j) == -99) removed.add(i);
            }
        }

//        public static DataSet getRow(int i) {
//            List<Object> row = new ArrayList<>();
//            for (int j = 0; j < dataSet.getNumColumns(); j++) {
//                row.add(dataSet.getObject(i, j));
//            }
//            return (DataSet) row;
//        }

//        System.out.println("Rows to remove: " + Arrays.toString(Ints.toArray(removed)) + "\n");

        biasData.removeRows(Ints.toArray(removed));

//        System.out.println("Post-dataSet: " + biasData);

        return biasData;
    }

    public DataSet BiasDataCellAlt(DataSet dataSet) {

        this.trueData = dataSet;
        this.biasData = dataSet.copy();
//        System.out.println("True dataSet: " + this.trueData);

        Graph graph = new EdgeListGraph(this.biasGraph);
        graph.removeEdges(this.trueGraph.getEdges());

        int vars = (biasData.getNumColumns()/2);
        for (int i = 0; i < biasData.getNumRows(); i++) {
            for (int j = 0; j < vars; j++) {
                if (biasData.getInt(i, j + vars) == 0 && graph.getIndegree(graph.getNode(biasData.getVariable(j+vars).getName())) > 0) biasData.setInt(i, j, -99);
//                System.out.println("For: x(" + i + "," + j + ") = "+ biasData.getInt(i,j) + " U(" + i + "," + (j+vars) +") =" + biasData.getInt(i,j+vars) + "\n" );

//                System.out.println(biasData);
            }
        }

        int[] cols = new int[vars];
        for (int i = 0; i < vars; i++) cols[i] = (i+vars);

        biasData.removeCols(cols);

//        System.out.println("Bias dataSet: " + biasData);

        return biasData;
    }

    public DataSet BiasDataRowAlt(DataSet dataSet) {

        this.trueData = dataSet;
        this.biasData = BiasDataCellAlt(dataSet.copy());
//        System.out.println("Pre-dataSet: " + trueData);

        List<Integer> removed = new ArrayList<>();

        for (int i = 0; i < biasData.getNumRows(); i++) {
            for (int j = 0; j < biasData.getNumColumns(); j++) {
//                    System.out.println("For: x(" + i + "," + j + ") = " + biasData.getInt(i, j) + "\n");
                if (!removed.contains(i))   if (biasData.getInt(i, j) == -99) removed.add(i);
            }
        }

//        System.out.println("Rows to remove: " + Arrays.toString(Ints.toArray(removed)) + "\n");

        biasData.removeRows(Ints.toArray(removed));

//        System.out.println("Post-dataSet: " + biasData);

        return biasData;
    }

}
