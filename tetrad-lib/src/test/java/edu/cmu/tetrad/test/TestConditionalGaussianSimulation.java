///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.test;

import edu.cmu.tetrad.algcomparison.Comparison;
import edu.cmu.tetrad.algcomparison.algorithm.Algorithms;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pattern.Fgs;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pattern.PcMax;
import edu.cmu.tetrad.algcomparison.graph.RandomForward;
import edu.cmu.tetrad.algcomparison.independence.ConditionalGaussianLRT;
import edu.cmu.tetrad.algcomparison.score.ConditionalGaussianBicScore;
import edu.cmu.tetrad.algcomparison.score.SemBicScore;
import edu.cmu.tetrad.algcomparison.simulation.*;
import edu.cmu.tetrad.algcomparison.statistic.*;
import edu.cmu.tetrad.algcomparison.statistic.utils.AdjacencyConfusion;
import edu.cmu.tetrad.algcomparison.statistic.utils.ArrowConfusion;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Parameters;

import java.util.List;

/**
 * An example script to simulate data and run a comparison analysis on it.
 *
 * @author jdramsey
 */
public class TestConditionalGaussianSimulation {

    public void test1() {
        Parameters parameters = new Parameters();

        parameters.set("numRuns", 10);
        parameters.set("numMeasures", 100);
        parameters.set("avgDegree", 6);
        parameters.set("sampleSize", 500);
        parameters.set("penaltyDiscount", 2);
        parameters.set("alpha", 0.001);

        parameters.set("maxDegree", 8);

        parameters.set("minCategories", 2);
        parameters.set("maxCategories", 5);
        parameters.set("percentDiscrete", 0);

        parameters.set("numCategoriesToDiscretize", 6);

        parameters.set("intervalBetweenRecordings", 20);

        parameters.set("varLow", 1.);
        parameters.set("varHigh", 2.);
        parameters.set("coefLow", .5);
        parameters.set("coefHigh", 1.5);
        parameters.set("coefSymmetric", true);
        parameters.set("meanLow", -1);
        parameters.set("meanHigh", 1);

        parameters.set("scaleFreeAlpha", .9);
        parameters.set("scaleFreeBeta", .05);
        parameters.set("scaleFreeDeltaIn", 3);
        parameters.set("scaleFreeDeltaOut", .1);

        Statistics statistics = new Statistics();

        statistics.add(new ParameterColumn("avgDegree"));
        statistics.add(new AdjacencyPrecision());
        statistics.add(new AdjacencyRecall());
        statistics.add(new ArrowheadPrecision());
        statistics.add(new ArrowheadRecall());
        statistics.add(new ElapsedTime());

        statistics.setWeight("AP", 1.0);
        statistics.setWeight("AR", 0.5);

        Simulations simulations = new Simulations();

        RandomForward graph = new RandomForward();
        simulations.add(new LinearFisherModel(graph));
//        simulations.add(new ConditionalGaussianSimulation2(graph));
//        simulations.add(new LeeHastieSimulation(graph));

        Algorithms algorithms = new Algorithms();

        algorithms.add(new Fgs(new SemBicScore()));
//        algorithms.add(new Fgs(new ConditionalGaussianBicScore()));
//        algorithms.add(new PcMax(new ConditionalGaussianLRT()));

        Comparison comparison = new Comparison();

        comparison.setShowAlgorithmIndices(true);
        comparison.setShowSimulationIndices(true);
        comparison.setSortByUtility(false);
        comparison.setShowUtilities(false);
        comparison.setParallelized(false);
        comparison.setSaveGraphs(true);

        comparison.setTabDelimitedTables(false);

        comparison.compareFromSimulations("comparison", simulations, algorithms, statistics, parameters);
    }

    public void test2() {
        Parameters parameters = new Parameters();

        parameters.set("numRuns", 1);
        parameters.set("numMeasures", 100);
        parameters.set("sampleSize", 1000);
        parameters.set("penaltyDiscount", 1);
        parameters.set("alpha", 0.001);

        parameters.set("avgDegree", 2);
        parameters.set("maxDegree", 8);

        parameters.set("minCategories", 2);
        parameters.set("maxCategories", 5);
        parameters.set("percentDiscrete", 50);

        parameters.set("numCategoriesToDiscretize", 6);

        parameters.set("intervalBetweenRecordings", 20);

        parameters.set("varLow", 1.);
        parameters.set("varHigh", 2.);
        parameters.set("coefLow", .5);
        parameters.set("coefHigh", 1.5);
        parameters.set("coefSymmetric", true);
        parameters.set("meanLow", -1);
        parameters.set("meanHigh", 1);


        Simulation simulation = new ConditionalGaussianSimulation(new RandomForward());

        simulation.createData(parameters);
        DataSet dataSet = simulation.getDataSet(0);

        Fgs fgs = new Fgs(new ConditionalGaussianBicScore());
        Graph graph = fgs.search(dataSet, parameters);

        IKnowledge knowledge = new Knowledge2();

        for (Node node : graph.getNodes()) {
            if (node instanceof ContinuousVariable) {
                List<Node> adj = graph.getAdjacentNodes(node);

                for (Node _adj : adj) {
                    if (_adj instanceof DiscreteVariable) {
                        knowledge.setRequired(_adj.getName(), node.getName());
                    }
                }
            }
        }

        Fgs fgs2 = new Fgs(new ConditionalGaussianBicScore());
        fgs2.setKnowledge(knowledge);

        Graph graph2 = fgs2.search(dataSet, parameters);

        Graph truth = simulation.getTrueGraph(0);
        graph2 = GraphUtils.replaceNodes(graph2, truth.getNodes());

        AdjacencyConfusion confusion = new AdjacencyConfusion(truth, graph2);
        ArrowConfusion confusion2 = new ArrowConfusion(truth, graph2);

        System.out.println(confusion.getAdjFp());
        System.out.println(confusion.getAdjFn());
        System.out.println(confusion2.getArrowsFp());
        System.out.println(confusion2.getArrowsFn());
    }

    public static void main(String...args) {
        new TestConditionalGaussianSimulation().test1();
    }
}




