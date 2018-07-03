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

import edu.cmu.tetrad.bayes.BayesIm;
import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesEstimator;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the generation of datasets with selection bias
 * Eventually will Tests the PC search & Fci Search over datasets with missing values.
 *
 * @author Joseph Ramsey
 */
public class TestSelectionBias {

    public void test1() {

        /* Create graph for testing purposes */
        List<Node> myNodes = new ArrayList<>();
        Node y1 = new DiscreteVariable("y1", 4);
        Node y2 = new DiscreteVariable("y2", 4);
        Node y3 = new DiscreteVariable("y3", 4);

        myNodes.add(y1);
        myNodes.add(y2);
        myNodes.add(y3);

        Graph myGraph = new EdgeListGraph(myNodes);
        myGraph.addDirectedEdge(y1, y2);
        myGraph.addDirectedEdge(y2, y3);

        System.out.println(myGraph);

        SelectionBias testing = new SelectionBias(myGraph,1);
        Graph biasGraph = testing.biasGraph;

        System.out.println(biasGraph);

        BayesPm pm = new BayesPm(biasGraph);
        BayesIm im = new MlBayesIm(pm, MlBayesIm.RANDOM);

        /* Generate Random dataset */
        DataSet myData = im.simulateData(3, false);

        System.out.println("Dataset: " + myData);

        System.out.println("Cell wise: " + testing.BiasDataCell(myData));
        System.out.println("Row wise: " + testing.BiasDataRow(myData));

        System.out.println("Cell wise alt: " + testing.BiasDataCellAlt(myData));
        System.out.println("Row wise alt: " + testing.BiasDataRowAlt(myData));

    }

    public void test2() {

        /* Create graph for testing purposes */
        Graph graph = GraphUtils.randomGraph(5,0,5,4,4,4,true);

        System.out.println("True Graph: " + graph.getEdges());

        SelectionBias testing = new SelectionBias(graph,1);

        System.out.println("Bias Graph: " + testing.biasGraph.getEdges());

        BayesPm pm = new BayesPm(testing.biasGraph);
        BayesIm im = new MlBayesIm(pm, MlBayesIm.RANDOM);

        /* Generate Random dataset */
        DataSet myData = im.simulateData(10, false);

        System.out.println("Data-set: " + myData);

        System.out.println("Clean Data: " + testing.BiasDataRow(myData));
        System.out.println("Clean Data Alt: " + testing.BiasDataRowAlt(myData));

    }

    public void test3() {
        /* Load Dataset from file */
        DataReader dataReader = new DataReader() ;
        dataReader.setVariablesSupplied(true);
        dataReader.setDelimiter(DelimiterType.TAB);
        dataReader.setMaxIntegralDiscrete(3);
        dataReader.setCommentMarker("//");
        dataReader.setMissingValueMarker("*");
        DataSet myData = null;
        try {
            myData = dataReader.parseTabular(new File("/home/israel/Desktop/IBRIC_RESEARCH/Simulated_Data2/save/data/data.1.txt" ));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(myData);

        /* Create graph for testing purposes */
        List<Node> myNodes = new ArrayList<>();
        Node y1 = myData.getVariable("y1");
        Node y2 = myData.getVariable("y2");
        Node y3 = myData.getVariable("y3");
        Node u2 = myData.getVariable("u2");

        myNodes.add(y1);
        myNodes.add(y2);
        myNodes.add(y3);
        myNodes.add(u2);

        Graph myGraph = new EdgeListGraph(myNodes);
        myGraph.addDirectedEdge(y1, y2);
        myGraph.addDirectedEdge(y2, y3);

        BayesPm pm = new BayesPm(myGraph);
        BayesIm im = new MlBayesEstimator().estimate(pm, myData);

        for (int i = 0; i < myData.getNumRows(); i ++) {
            if (myData.getInt(i, myData.getColumn(u2)) == 0) {
                myData.setInt(i, myData.getColumn(y3),-99) ;
            }
        }

//        System.out.println(myData);

        myData.removeColumn(u2);

//        DataSet BiasedData = SelectionBias.selectiveRemoval(myGraph, 1);
//        System.out.println(BiasedData);
    }



    public static void main(String...args) {
//        new TestSelectionBias().test1();
        new TestSelectionBias().test2();
    }


}





