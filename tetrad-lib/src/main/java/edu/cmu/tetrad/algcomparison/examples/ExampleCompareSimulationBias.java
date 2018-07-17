///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines,    //
// Joseph Ramsey, and Clark Glymour.                                         //
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

package edu.cmu.tetrad.algcomparison.examples;

import edu.cmu.tetrad.algcomparison.Comparison;
import edu.cmu.tetrad.algcomparison.algorithm.Algorithms;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pag.Fci;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pattern.Pc;
import edu.cmu.tetrad.algcomparison.graph.RandomForward;
import edu.cmu.tetrad.algcomparison.graph.RandomGraph;
import edu.cmu.tetrad.algcomparison.independence.ChiSquare;
import edu.cmu.tetrad.algcomparison.simulation.SelectionBiasSimulationROWWISE;
import edu.cmu.tetrad.algcomparison.simulation.SelectionBiasSimulationTESTWISE;
import edu.cmu.tetrad.algcomparison.simulation.Simulations;
import edu.cmu.tetrad.algcomparison.statistic.ElapsedTime;
import edu.cmu.tetrad.algcomparison.statistic.SHD;
import edu.cmu.tetrad.algcomparison.statistic.Statistics;
import edu.cmu.tetrad.util.Parameters;

/**
 * An example script to simulate data and run a comparison analysis on it.
 *
 * @author jdramsey
 */

//        https://arxiv.org/abs/1607.08110

public class ExampleCompareSimulationBias {
    public static void main(String... args) {
        Parameters parameters = new Parameters();
        /* Graph Params */
        parameters.set("numMeasures", 100);
        parameters.set("numLatents", 0);
//        parameters.set("numMeasures", 10);
//        parameters.set("minOutdegree", 1);
//        parameters.set("maxOutdegree", 2);
//        parameters.set("minIndegree", 1);
//        parameters.set("maxIndegree", 2);
//        parameters.set("avgDegree", 2);
//        parameters.set("maxDegree", 2);
//        parameters.set("numCategories", 4);
        parameters.set("minCategories", 2);
        parameters.set("maxCategories", 6);
        parameters.set("saveLatentVars", false);

        /* Data Params*/
        parameters.set("sampleSize", 1000);
        parameters.set("biasedEdges", 33);
        parameters.set("minMissingness", 0.05); // , 0.26, 0.51, 0.76);
        parameters.set("maxMissingness", 0.15); // , 0.50, 0.75, 0.99);

        /* Simulation params*/
        parameters.set("differentGraphs", true);
        parameters.set("numRuns", 20);
        parameters.set("alpha", 1e-4);

        /* We should assign parameters here */
        /* Ideally 3 Variables, each with 3 categories, 1000 multinomial samples per var */

        Statistics statistics = new Statistics();

//        statistics.add(new AdjacencyPrecision());
//        statistics.add(new AdjacencyRecall());
//        statistics.add(new ArrowheadPrecision());
//        statistics.add(new ArrowheadRecall());
//        statistics.add(new MathewsCorrAdj());
//        statistics.add(new MathewsCorrArrow());
//        statistics.add(new F1Adj());
//        statistics.add(new F1Arrow());
        statistics.add(new SHD());
        statistics.add(new ElapsedTime());

//        statistics.setWeight("AP", 1.0);
//        statistics.setWeight("AR", 0.5);

        Algorithms algorithms = new Algorithms();

        algorithms.add(new Pc(new ChiSquare()));
        algorithms.add(new Fci(new ChiSquare()));
//        algorithms.add(new Gfci(new ChiSquare(), new BdeuScore()));
//        algorithms.add(new Cpc(new FisherZ(), new Fges(new SemBicScore(), false)));
//        algorithms.add(new PcStable(new FisherZ()));
//        algorithms.add(new CpcStable(new FisherZ()));

        Simulations simulations = new Simulations();

        RandomGraph rGraph = new RandomForward();
        simulations.add(new SelectionBiasSimulationTESTWISE(rGraph));
        simulations.add(new SelectionBiasSimulationROWWISE(rGraph));

        Comparison comparison = new Comparison();

        comparison.setShowAlgorithmIndices(true);
        comparison.setShowSimulationIndices(true);
        comparison.setSortByUtility(false);
        comparison.setShowUtilities(false);
        comparison.setParallelized(true);

        comparison.compareFromSimulations("/home/israel/Documents/Gitstuff/Tetrad/Results", simulations, algorithms, statistics, parameters);
    }
}




