package edu.cmu.tetrad.algcomparison.simulation;

import edu.cmu.tetrad.algcomparison.graph.RandomGraph;
import edu.cmu.tetrad.algcomparison.graph.SingleGraph;
import edu.cmu.tetrad.bayes.BayesIm;
import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SelectionBias;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jdramsey
 *
 */
public class SelectionBiasSimulation implements Simulation {

    static final long serialVersionUID = 23L;
    private final RandomGraph randomGraph;
    private BayesPm pm;
    private BayesIm im;
    private List<DataSet> dataSets = new ArrayList<>();
    private List<Graph> graphs = new ArrayList<>();
    private final List<BayesIm> ims = new ArrayList<>();

    public SelectionBiasSimulation(RandomGraph graph) {
        this.randomGraph = graph;
    }

    @Override
    public void createData(Parameters parameters) {
        Graph graph = randomGraph.createGraph(parameters);
        dataSets = new ArrayList<>();
        graphs = new ArrayList<>();
        for (int i = 0; i < parameters.getInt("numRuns"); i++) {
            //noinspection SpellCheckingInspection,SpellCheckingInspection
            System.out.println("Simulating dataset #" + (i + 1));
            if (parameters.getBoolean("differentGraphs") && i > 0) {
                graph = randomGraph.createGraph(parameters);
            }
            graphs.add(graph);
            System.out.println("True graph: " + graph);
            SelectionBias selection = new SelectionBias(graph, parameters.getInt("biasedEdges"));
            System.out.println("Selection graph: " + selection.biasGraph);
            DataSet dataSet = simulate(selection.biasGraph, parameters);
            //noinspection SpellCheckingInspection
            System.out.println("Bias Dataset: " + dataSet);
            dataSet = selection.BiasDataCell(dataSet);
            dataSet.setName("" + (i + 1));
            //noinspection SpellCheckingInspection
            System.out.println("Clean Dataset: " + dataSet);
            dataSets.add(dataSet);
        }
    }


    @Override
    public DataModel getDataModel(int index) {
        return dataSets.get(index);
    }

    @Override
    public Graph getTrueGraph(int index) {
        if (graphs.isEmpty()) {
            return new EdgeListGraph();
        } else {
            return graphs.get(index);
        }
    }

    @Override
    public String getDescription() {
        return "Selection Bias simulation using " + randomGraph.getDescription();
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();

        if (!(randomGraph instanceof SingleGraph)) {
            parameters.addAll(randomGraph.getParameters());
        }


        if (pm == null) {
            parameters.addAll(BayesPm.getParameterNames());
        }

        if (im == null) {
            parameters.addAll(MlBayesIm.getParameterNames());
        }

        parameters.add("numRuns");
        parameters.add("differentGraphs");
        parameters.add("sampleSize");
        parameters.add("saveLatentVars");

        return parameters;
    }

    @Override
    public int getNumDataModels() {
        return dataSets.size();
    }

    @Override
    public DataType getDataType() {
        return DataType.Discrete;
    }


    private DataSet simulate(Graph graph, Parameters parameters) {
        boolean saveLatentVars = parameters.getBoolean("saveLatentVars");
        try {
            int minCategories = parameters.getInt("minCategories");
            int maxCategories = parameters.getInt("maxCategories");
            pm = new BayesPm(graph, minCategories, maxCategories);
            im = new MlBayesIm(pm, MlBayesIm.RANDOM);
            double lower = parameters.getDouble("minMissingness");
            double upper = parameters.getDouble("maxMissingness");
            for (int i = 0; i < im.getNumNodes(); i++) {
                if (im.getNode(i).getName().startsWith("U")) {
                    double p = RandomUtil.getInstance().nextUniform(lower, upper);
                    for (int r = 0; r < im.getNumRows(i); r++) {
                        im.setProbability(i, r, 0, p);
                        im.setProbability(i, r, 1, 1 - p);
                    }
                }
            }
            ims.add(im);
            return im.simulateData(parameters.getInt("sampleSize"), saveLatentVars);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Sorry, I couldn't simulate from that Bayes IM; perhaps not all of\n"
                    + "the parameters have been specified.");
        }
    }

    public List<BayesIm> getBayesIms() {
        return this.ims;
    }
}
