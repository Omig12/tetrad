package edu.cmu.tetrad.intervention.algorithms;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.algcomparison.utils.TakesInitialGraph;
import edu.cmu.tetrad.algcomparison.utils.UsesScoreWrapper;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.intervention.CleanInterventions;
import edu.cmu.tetrad.intervention.InterventionalKnowledge;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.pitt.dbmi.algo.bootstrap.BootstrapEdgeEnsemble;
import edu.pitt.dbmi.algo.bootstrap.GeneralBootstrapTest;

import java.io.PrintStream;
import java.util.List;

/**
 * FGES (the heuristic version).
 *
 * @author jdramsey
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "FGES",
        command = "fges",
        algoType = AlgType.forbid_latent_common_causes
)
public class IFges implements Algorithm, TakesInitialGraph, HasKnowledge, UsesScoreWrapper {

    static final long serialVersionUID = 23L;
    private boolean compareToTrue = false;
    private ScoreWrapper score;
    private Algorithm algorithm = null;
    private Graph initialGraph = null;
    private IKnowledge knowledge = new Knowledge2();

    public IFges() {

    }

    public IFges(ScoreWrapper score) {
        this.score = score;
        this.compareToTrue = false;
    }

    public IFges(ScoreWrapper score, boolean compareToTrueGraph) {
        this.score = score;
        this.compareToTrue = compareToTrueGraph;
    }

    public IFges(ScoreWrapper score, Algorithm algorithm) {
        this.score = score;
        this.algorithm = algorithm;
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        if (!parameters.getBoolean("bootstrapping")) {
            if (algorithm != null) {
//                initialGraph = algorithm.search(dataSet, parameters);
            }

            //REMOVE EXTRA OBSERVATIONS
            CleanInterventions ci = new CleanInterventions();
            dataSet = ci.removeExtra(dataSet);

            //REMOVE INTERVENTIONAL CONTEXT
            dataSet = ci.removeInterventionContext(dataSet);

            //REMOVE CONTEXT
            if(parameters.getBoolean("removeContext")) {
                dataSet = ci.removeContext(dataSet);
            }

            //KNOWLEDGE ADDED
            Graph nodes = GraphUtils.emptyGraph(0);
            for (Node node : dataSet.getVariables()) {
                nodes.addNode(node);
            }
            InterventionalKnowledge k = new InterventionalKnowledge(nodes, parameters.getInt("hand"));
            this.knowledge = k.getKnowledge();

            edu.cmu.tetrad.search.Fges search = new edu.cmu.tetrad.search.Fges(score.getScore(dataSet, parameters));
            search.setFaithfulnessAssumed(parameters.getBoolean("faithfulnessAssumed"));
            search.setKnowledge(knowledge);
            search.setVerbose(parameters.getBoolean("verbose"));
            search.setMaxDegree(parameters.getInt("maxDegree"));
            search.setSymmetricFirstStep(parameters.getBoolean("symmetricFirstStep"));

            Object obj = parameters.get("printStream");
            if (obj instanceof PrintStream) {
                search.setOut((PrintStream) obj);
            }

            if (initialGraph != null) {
                search.setInitialGraph(initialGraph);
            }

            return search.search();
        } else {
            IFges fges = new IFges(score, algorithm);

            fges.setKnowledge(knowledge);
            DataSet data = (DataSet) dataSet;
            GeneralBootstrapTest search = new GeneralBootstrapTest(data, fges, parameters.getInt("bootstrapSampleSize"));

            BootstrapEdgeEnsemble edgeEnsemble = BootstrapEdgeEnsemble.Highest;
            switch (parameters.getInt("bootstrapEnsemble", 1)) {
                case 0:
                    edgeEnsemble = BootstrapEdgeEnsemble.Preserved;
                    break;
                case 1:
                    edgeEnsemble = BootstrapEdgeEnsemble.Highest;
                    break;
                case 2:
                    edgeEnsemble = BootstrapEdgeEnsemble.Majority;
            }
            search.setEdgeEnsemble(edgeEnsemble);
            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean("verbose"));
            return search.search();
        }

    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        if (compareToTrue) {
            return new EdgeListGraph(graph);
        } else {
            return SearchGraphUtils.patternForDag(new EdgeListGraph(graph));
        }
    }

    @Override
    public String getDescription() {
        return "IFGES (Fast Greedy Equivalence Search) using " + score.getDescription();
    }

    @Override
    public DataType getDataType() {
        return score.getDataType();
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = score.getParameters();
        parameters.add("faithfulnessAssumed");
        parameters.add("symmetricFirstStep");
        parameters.add("maxDegree");
        parameters.add("verbose");
        parameters.add("hand");
        parameters.add("removeContext");
        // Bootstrapping
        parameters.add("bootstrapping");
        parameters.add("bootstrapSampleSize");
        parameters.add("bootstrapEnsemble");
        return parameters;
    }

    @Override
    public IKnowledge getKnowledge() {
        return knowledge;
    }

    @Override
    public void setKnowledge(IKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    public void setCompareToTrue(boolean compareToTrue) {
        this.compareToTrue = compareToTrue;
    }

    @Override
    public Graph getInitialGraph() {
        return initialGraph;
    }

    @Override
    public void setInitialGraph(Graph initialGraph) {
        this.initialGraph = initialGraph;
    }

    @Override
    public void setInitialGraph(Algorithm algorithm) {
        this.algorithm = algorithm;

    }

    @Override
    public void setScoreWrapper(ScoreWrapper score) {
        this.score = score;
    }

}
