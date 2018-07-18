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
 */
public class SelectionBiasSimulationTESTWISE implements Simulation {

    static final long serialVersionUID = 23L;
    private final RandomGraph randomGraph;
    private BayesPm pm;
    private BayesIm im;
    private List<DataSet> dataSets = new ArrayList<>();
    private List<Graph> graphs = new ArrayList<>();
    private final List<BayesIm> ims = new ArrayList<>();
    private List<Double> datasetSize = new ArrayList<>();

    public SelectionBiasSimulationTESTWISE(RandomGraph graph) {
        this.randomGraph = graph;
    }

    @Override
    public void createData(Parameters parameters) {
        Graph graph = randomGraph.createGraph(parameters);
        dataSets = new ArrayList<>();
        graphs = new ArrayList<>();
        for (int i = 0; i < parameters.getInt("numRuns"); i++) {
            //noinspection SpellCheckingInspection,SpellCheckingInspection
            System.out.println("Simulating Test-wise dataset #" + (i + 1));
            if (parameters.getBoolean("differentGraphs") && i > 0) {
                graph = randomGraph.createGraph(parameters);
            }
            graphs.add(graph);
//            System.out.println("True graph: " + graph);
            int minCategories = parameters.getInt("minCategories");
            int maxCategories = parameters.getInt("maxCategories");
            pm = new BayesPm(graph, minCategories, maxCategories);
//            System.out.println(pm);
            SelectionBias selection = new SelectionBias(graph, pm, parameters.getInt("biasedEdges"));
            pm = selection.getPm();
//            System.out.println("Selection graph: " + selection.biasGraph);
            DataSet dataSet = simulate(selection.biasGraph, parameters);
            //noinspection SpellCheckingInspection
//            System.out.println("Bias Dataset: " + dataSet);
            dataSet = selection.BiasDataCellAlt(dataSet);
            dataSet.setName("" + (i + 1));
            //noinspection SpellCheckingInspection
//            System.out.println("Clean Dataset: " + dataSet);
            dataSets.add(dataSet);
            datasetSize.add(dataSet.getNumRows() * 1.0);
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
        return "Selection Bias simulation using " + randomGraph.getDescription() + " and test-wise deletion";
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

            im = new MlBayesIm(pm, MlBayesIm.RANDOM);
//            System.out.println("pre" + im);
            double lower = parameters.getDouble("minMissingness");
            double upper = parameters.getDouble("maxMissingness");
            int uvars = graph.getNumNodes() / 2;
            for (int i = uvars; i < graph.getNumNodes(); i++) {
//                double p = RandomUtil.getInstance().nextUniform(lower, upper);
                double p = RandomUtil.getInstance().nextUniform(lower, upper);
                p = p * p * p;
                for (int r = 0; r < im.getNumRows(i); r++) {
                    im.setProbability(i, r, 0, p);
                    im.setProbability(i, r, 1, 1 - p);
                }
            }
//            System.out.println("pos" + im);
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

    public List<Double> getDatasetSizes() {
        return this.datasetSize;
    }

}
