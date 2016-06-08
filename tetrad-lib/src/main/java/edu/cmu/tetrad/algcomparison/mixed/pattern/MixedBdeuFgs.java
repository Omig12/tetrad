package edu.cmu.tetrad.algcomparison.mixed.pattern;

import edu.cmu.tetrad.algcomparison.Algorithm;
import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.Discretizer;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.BDeuScore;
import edu.cmu.tetrad.search.Fgs;
import edu.cmu.tetrad.search.SearchGraphUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by jdramsey on 6/4/16.
 */
public class MixedBdeuFgs implements Algorithm {
    public Graph search(DataSet Dk, Map<String, Number> parameters) {
        Discretizer discretizer = new Discretizer(Dk);
        List<Node> nodes = Dk.getVariables();

        for (Node node : nodes) {
            if (node instanceof ContinuousVariable) {
                discretizer.equalIntervals(node, parameters.get("numCategories").intValue());
            }
        }

        Dk = discretizer.discretize();

        BDeuScore score = new BDeuScore(Dk);
        score.setSamplePrior(1.0);
        score.setStructurePrior(1.0);
        Fgs fgs = new Fgs(score);
        Graph p = fgs.search();
        return convertBack(Dk, p);
    }


    @Override
    public Graph getComparisonGraph(Graph dag) {
        return SearchGraphUtils.patternForDag(dag);
    }

    @Override
    public String getDescription() {
        return "Discretizes the continuous variables in the data set, then runs FGS using the BDeu score.";
    }

    private Graph convertBack(DataSet Dk, Graph p) {
        Graph p2 = new EdgeListGraph(Dk.getVariables());

        for (int i = 0; i < p.getNodes().size(); i++) {
            for (int j = i + 1; j < p.getNodes().size(); j++) {
                Node v1 = p.getNodes().get(i);
                Node v2 = p.getNodes().get(j);

                Edge e = p.getEdge(v1, v2);

                if (e != null) {
                    Node w1 = Dk.getVariable(e.getNode1().getName());
                    Node w2 = Dk.getVariable(e.getNode2().getName());

                    Edge e2 = new Edge(w1, w2, e.getEndpoint1(), e.getEndpoint2());

                    p2.addEdge(e2);
                }
            }
        }
        return p2;
    }
}