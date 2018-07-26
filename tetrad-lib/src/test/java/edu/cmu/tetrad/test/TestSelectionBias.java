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

import edu.cmu.tetrad.algcomparison.statistic.*;
import edu.cmu.tetrad.bayes.BayesIm;
import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesEstimator;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.DagToPag2;
import edu.cmu.tetrad.search.Fci;
import edu.cmu.tetrad.search.IndTestChiSquare;
import edu.cmu.tetrad.search.Pc;
import edu.cmu.tetrad.util.RandomUtil;
import edu.cmu.tetrad.util.StatUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test the generation of datasets with selection bias
 * Eventually will Tests the PC search & Fci Search over datasets with missing values.
 *
 * @author Joseph Ramsey
 */
public class TestSelectionBias {

    public void testx() {
        /* Create graph for testing purposes */
        List<Node> myNodes = new ArrayList<>();
        Node y1 = new DiscreteVariable("Sex", 2);
        Node y2 = new DiscreteVariable("IQ", 3);
        Node y3 = new DiscreteVariable("College", 2);
        Node y4 = new DiscreteVariable("Job", 2);

        myNodes.add(y1);
        myNodes.add(y3);
        myNodes.add(y2);
        myNodes.add(y4);

        Graph myGraph = new EdgeListGraph(myNodes);
        myGraph.addDirectedEdge(y1, y3);
        myGraph.addDirectedEdge(y2, y3);
        myGraph.addDirectedEdge(y3, y4);


//        System.out.println(myGraph);
        BayesPm pm = new BayesPm(myGraph);
        pm.setCategories(y1, Arrays.asList("M", "F"));
        pm.setCategories(y2, Arrays.asList("70-89", "90-109", "110-140"));
        pm.setCategories(y3, Arrays.asList("no", "yes"));
        pm.setCategories(y4, Arrays.asList("no", "yes"));

//        System.out.println(pm);

        BayesIm im = new MlBayesIm(pm, MlBayesIm.RANDOM);

//                System.out.println("Random IM" + im);

//      Probability for sex
        im.setProbability(im.getNodeIndex(y1), 0, 0, 0.492);
        im.setProbability(im.getNodeIndex(y1), 0, 1, 0.508);

        //      Probability for IQ
        im.setProbability(im.getNodeIndex(y2), 0, 0, 0.16);
        im.setProbability(im.getNodeIndex(y2), 0, 1, 0.68);
        im.setProbability(im.getNodeIndex(y2), 0, 2, 0.16);

//      Probability for College
        im.setProbability(im.getNodeIndex(y3), 0, 0, 0.9992);
        im.setProbability(im.getNodeIndex(y3), 0, 1, 0.0008);
        im.setProbability(im.getNodeIndex(y3), 1, 0, 0.36);
        im.setProbability(im.getNodeIndex(y3), 1, 1, 0.64);
        im.setProbability(im.getNodeIndex(y3), 2, 0, 0.06);
        im.setProbability(im.getNodeIndex(y3), 2, 1, 0.93);

        im.setProbability(im.getNodeIndex(y3), 3, 0, 0.9992);
        im.setProbability(im.getNodeIndex(y3), 3, 1, 0.0008);
        im.setProbability(im.getNodeIndex(y3), 4, 0, 0.12);
        im.setProbability(im.getNodeIndex(y3), 4, 1, 0.88);
        im.setProbability(im.getNodeIndex(y3), 5, 0, 0.0002);
        im.setProbability(im.getNodeIndex(y3), 5, 1, 0.9998);

////      Probability for Interviewed
        im.setProbability(im.getNodeIndex(y4), 0, 0, 1.0);
        im.setProbability(im.getNodeIndex(y4), 0, 1, 0.0);
        im.setProbability(im.getNodeIndex(y4), 1, 0, 1.0);
        im.setProbability(im.getNodeIndex(y4), 1, 1, 0.0);


//        System.out.println("Original IM" + im);

        SelectionBias testing = new SelectionBias(myGraph, pm, 2);
        List<Node> w = testing.biasGraph.getNodes();
//        System.out.println(testing.biasGraph);
        testing.biasGraph = myGraph;
//        System.out.println(testing.biasGraph);
        for (Node x : w) {
            testing.biasGraph.addNode(x);
        }
//        testing.biasGraph.addDirectedEdge(y1, testing.biasGraph.getNode("UCollege"));
//        testing.biasGraph.addDirectedEdge(y2, testing.biasGraph.getNode("UCollege"));
        testing.biasGraph.addDirectedEdge(y1, testing.biasGraph.getNode("USex"));
        testing.biasGraph.addDirectedEdge(y2, testing.biasGraph.getNode("UIQ"));

//        System.out.println(testing.biasGraph.getEdges());

//        System.out.println(testing.getPm());

        BayesPm pm2 = new BayesPm(testing.biasGraph, testing.getPm());

        BayesIm biasIm = new MlBayesIm(pm2, im, MlBayesIm.RANDOM);

//        System.out.println("Bias IM" + biasIm);

        int uvars = testing.biasGraph.getNumNodes() / 2;
        for (int i = uvars; i < testing.biasGraph.getNumNodes(); i++) {
            for (int r = 0; r < biasIm.getNumRows(i); r++) {
                double p = Math.pow(RandomUtil.getInstance().nextUniform(0.33, 0.33), 2);
                biasIm.setProbability(i, r, 0, p);
                biasIm.setProbability(i, r, 1, 1 - p);
            }
        }

//        System.out.println("pos" + biasIm);
//        int[] Sizes = new int[]{5, 10, 50, 100, 500, 1000, 5000, 10000, 50000};
//        int[] Sizes = new int[]{5, 10, 50, 100, 500, 1000, 5000};
        int[] Sizes = new int[]{5, 10, 50, 100, 500, 1000};
        List<Double> Cell_SAM_final = new ArrayList<>();
        List<Double> Cell_aps_final = new ArrayList<>();
        List<Double> Cell_ars_final = new ArrayList<>();
        List<Double> Cell_ahps_final = new ArrayList<>();
        List<Double> Cell_ahrs_final = new ArrayList<>();
        List<Double> Cell_dnaps_final = new ArrayList<>();
        List<Double> Cell_dnars_final = new ArrayList<>();
        List<Double> Cell_SS_final = new ArrayList<>();
        List<Double> Cell_SHDs_final = new ArrayList<>();
        List<Double> Row_SAM_final = new ArrayList<>();
        List<Double> Row_aps_final = new ArrayList<>();
        List<Double> Row_ars_final = new ArrayList<>();
        List<Double> Row_ahps_final = new ArrayList<>();
        List<Double> Row_ahrs_final = new ArrayList<>();
        List<Double> Row_dnaps_final = new ArrayList<>();
        List<Double> Row_dnars_final = new ArrayList<>();
        List<Double> Row_SS_final = new ArrayList<>();
        List<Double> Row_SHDs_final = new ArrayList<>();

        for (int x : Sizes) {

            /* Generate Random dataset */
            int numRuns = 10;
            double[] Cell_aps = new double[numRuns];
            double[] Cell_ars = new double[numRuns];
            double[] Cell_ahps = new double[numRuns];
            double[] Cell_ahrs = new double[numRuns];
            double[] Cell_dnaps = new double[numRuns];
            double[] Cell_dnars = new double[numRuns];
            double[] Cell_SS = new double[numRuns];
            double[] Cell_SHDs = new double[numRuns];
            double[] Row_aps = new double[numRuns];
            double[] Row_ars = new double[numRuns];
            double[] Row_ahps = new double[numRuns];
            double[] Row_ahrs = new double[numRuns];
            double[] Row_dnaps = new double[numRuns];
            double[] Row_dnars = new double[numRuns];
            double[] Row_SS = new double[numRuns];
            double[] Row_SHDs = new double[numRuns];

            for (int i = 0; i < numRuns; i++) {
                DataSet myData = biasIm.simulateData(x, true);
//        System.out.println("Dataset: " + myData);
//
//        System.out.println("Cell wise: " + testing.BiasDataCell(myData));
//        System.out.println("Row wise: " + testing.BiasDataRow(myData));
//
//        System.out.println("Cell wise alt: " + testing.BiasDataCellAlt(myData));
//        System.out.println("Row wise alt: " + testing.BiasDataRowAlt(myData));
//
//            System.out.println("Cell wise: " + testing.BiasDataCell(myData).getNumRows());
//            System.out.println("Row wise: " + testing.BiasDataRow(myData).getNumRows());
//            System.out.println("Cell wise alt: " + testing.BiasDataCellAlt(myData).getNumRows());
//            System.out.println("Row wise alt: " + testing.BiasDataRowAlt(myData).getNumRows());
//            System.out.println();
                Cell_SS[i] = testing.BiasDataCellAlt(myData).getNumRows();
                Row_SS[i] = testing.BiasDataRowAlt(myData).getNumRows();

//            System.out.println("True graph:" + testing.trueGraph.getEdges());

                double alpha = 0.05;

//            System.out.println("\n\tChi_Square_Tests\n");

//        System.out.println("FCI cell " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataCell(myData),alpha)).search()).getEdges());
//        System.out.println("FCI row " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataRow(myData),alpha)).search()).getEdges());
//
//        Graph trueGraph = removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataCell(myData),alpha)).search());
//        Graph estGraph = removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataRow(myData),alpha)).search());
//            System.out.println(testing.trueGraph.getEdges());
//            System.out.println("FCI cellalt " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataCellAlt(myData), alpha)).search()).getEdges());
//            System.out.println("FCI rowalt " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataRowAlt(myData), alpha)).search()).getEdges());

                Graph Cell_estGraph = new Fci(new IndTestChiSquare(testing.BiasDataCellAlt(myData), alpha)).search();
                Graph Row_estGraph = new Fci(new IndTestChiSquare(testing.BiasDataRowAlt(myData), alpha)).search();

                testing.trueGraph = new DagToPag2(myGraph).convert();
                System.out.println("True: " + testing.trueGraph.getEdges());

//                Cell_estGraph.setPag(true);
//                Row_estGraph.setPag(true);
//                testing.trueGraph.setPag(true);

                System.out.println("Cell: " + Cell_estGraph.getEdges());
                System.out.println("Row: " + Row_estGraph.getEdges());

                AdjacencyPrecision ap = new AdjacencyPrecision();
                AdjacencyRecall ar = new AdjacencyRecall();
                ArrowheadPrecision ahp = new ArrowheadPrecision();
                ArrowheadRecall ahr = new ArrowheadRecall();
                DefiniteNonancestorPrecision dnap = new DefiniteNonancestorPrecision();
                DefiniteNonancestorRecall dnar = new DefiniteNonancestorRecall();
                SHD shd = new SHD();

                Cell_estGraph = GraphUtils.replaceNodes(Cell_estGraph, testing.trueGraph.getNodes());

                double Cell_ap = ap.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_ar = ar.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_ahp = ahp.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_ahr = ahr.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_dnap = dnap.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_dnar = dnar.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_shd = shd.getValue(testing.trueGraph, Cell_estGraph);

                double Row_ap = ap.getValue(testing.trueGraph, Row_estGraph);
                double Row_ar = ar.getValue(testing.trueGraph, Row_estGraph);
                double Row_ahp = ahp.getValue(testing.trueGraph, Row_estGraph);
                double Row_ahr = ahr.getValue(testing.trueGraph, Row_estGraph);
                double Row_dnap = dnap.getValue(testing.trueGraph, Row_estGraph);
                double Row_dnar = dnar.getValue(testing.trueGraph, Row_estGraph);
                double Row_shd = shd.getValue(testing.trueGraph, Row_estGraph);

                Cell_aps[i] = Cell_ap;
                Cell_ars[i] = Cell_ar;
                Cell_ahps[i] = Cell_ahp;
                Cell_ahrs[i] = Cell_ahr;
                Cell_dnaps[i] = Cell_dnap;
                Cell_dnars[i] = Cell_dnar;
                Cell_SHDs[i] = Cell_shd;

                Row_aps[i] = Row_ap;
                Row_ars[i] = Row_ar;
                Row_ahps[i] = Row_ahp;
                Row_ahrs[i] = Row_ahr;
                Row_dnaps[i] = Row_dnap;
                Row_dnars[i] = Row_dnar;
                Row_SHDs[i] = Row_shd;
            }

            Cell_SAM_final.add((double) x);
            Cell_aps_final.add(StatUtils.mean(Cell_aps));
            Cell_ars_final.add(StatUtils.mean(Cell_ars));
            Cell_ahps_final.add(StatUtils.mean(Cell_ahps));
            Cell_ahrs_final.add(StatUtils.mean(Cell_ahrs));
            Cell_dnaps_final.add(StatUtils.mean(Cell_dnaps));
            Cell_dnars_final.add(StatUtils.mean(Cell_dnars));
            Cell_SS_final.add(StatUtils.mean(Cell_SS));
            Cell_SHDs_final.add(StatUtils.mean(Cell_SHDs));

            Row_SAM_final.add((double) x);
            Row_aps_final.add(StatUtils.mean(Row_aps));
            Row_ars_final.add(StatUtils.mean(Row_ars));
            Row_ahps_final.add(StatUtils.mean(Row_ahps));
            Row_ahrs_final.add(StatUtils.mean(Row_ahrs));
            Row_dnaps_final.add(StatUtils.mean(Row_dnaps));
            Row_dnars_final.add(StatUtils.mean(Row_dnars));
            Row_SS_final.add(StatUtils.mean(Row_SS));
            Row_SHDs_final.add(StatUtils.mean(Row_SHDs));

//            System.out.println("\nCell-wise: " + x);
//
//            System.out.println("Cell_AP: " + StatUtils.mean(Cell_aps));
//            System.out.println("Cell_AR: " + StatUtils.mean(Cell_ars));
//            System.out.println("Cell_AHP: " + StatUtils.mean(Cell_ahps));
//            System.out.println("Cell_AHR: " + StatUtils.mean(Cell_ahrs));
//            System.out.println("Cell_DNAP: " + StatUtils.mean(Cell_dnaps));
//            System.out.println("Cell_DNAR: " + StatUtils.mean(Cell_dnars));
//            System.out.println("Cell_SS: " + StatUtils.mean(Cell_SS));
//            System.out.println("Cell_SHD: " + StatUtils.mean(Cell_SHDs));
//
//            System.out.println("\nRow-wise: " + x);
//
//            System.out.println("Row_AP: " + StatUtils.mean(Row_aps));
//            System.out.println("Row_AR: " + StatUtils.mean(Row_ars));
//            System.out.println("Row_AHP: " + StatUtils.mean(Row_ahps));
//            System.out.println("Row_AHR: " + StatUtils.mean(Row_ahrs));
//            System.out.println("Row_DNAP: " + StatUtils.mean(Row_dnaps));
//            System.out.println("Row_DNAR: " + StatUtils.mean(Row_dnars));
//            System.out.println("Row_SS: " + StatUtils.mean(Row_SS));
//            System.out.println("Row_SHD: " + StatUtils.mean(Row_SHDs));
        }
        System.out.println(
                "Test = {\"Samples\": " + Cell_SAM_final +
                        ", \"AP\": " + Cell_aps_final +
                        ", \"AR\": " + Cell_ars_final +
                        ", \"AHP\": " + Cell_ahps_final +
                        ", \"AHR\": " + Cell_ahrs_final +
                        ", \"DNAP\": " + Cell_dnaps_final +
                        ", \"DNAR\": " + Cell_dnars_final +
                        ", \"SS\": " + Cell_SS_final +
                        ", \"SHD\": " + Cell_SHDs_final + "}");

        System.out.println(
                "Row = {\"Samples\": " + Row_SAM_final +
                        ", \"AP\": " + Row_aps_final +
                        ", \"AR\": " + Row_ars_final +
                        ", \"AHP\": " + Row_ahps_final +
                        ", \"AHR\": " + Row_ahrs_final +
                        ", \"DNAP\": " + Row_dnaps_final +
                        ", \"DNAR\": " + Row_dnars_final +
                        ", \"SS\": " + Row_SS_final +
                        ", \"SHD\": " + Row_SHDs_final + "}");
    }

    public void test1() {

//        System.out.println("pos" + biasIm);
        int[] Sizes = new int[]{5, 10, 50, 100, 500, 1000, 5000, 10000};
//        int[] Sizes = new int[]{1000};
        List<Double> Cell_SAM_final = new ArrayList<>();
        List<Double> Cell_aps_final = new ArrayList<>();
        List<Double> Cell_ars_final = new ArrayList<>();
        List<Double> Cell_ahps_final = new ArrayList<>();
        List<Double> Cell_ahrs_final = new ArrayList<>();
        List<Double> Cell_dnaps_final = new ArrayList<>();
        List<Double> Cell_dnars_final = new ArrayList<>();
        List<Double> Cell_SS_final = new ArrayList<>();
        List<Double> Cell_SHDs_final = new ArrayList<>();
        List<Double> Row_SAM_final = new ArrayList<>();
        List<Double> Row_aps_final = new ArrayList<>();
        List<Double> Row_ars_final = new ArrayList<>();
        List<Double> Row_ahps_final = new ArrayList<>();
        List<Double> Row_ahrs_final = new ArrayList<>();
        List<Double> Row_dnaps_final = new ArrayList<>();
        List<Double> Row_dnars_final = new ArrayList<>();
        List<Double> Row_SS_final = new ArrayList<>();
        List<Double> Row_SHDs_final = new ArrayList<>();
        for (int x : Sizes) {

            /* Generate Random dataset */
            int numRuns = 100;
            double[] Cell_aps = new double[numRuns];
            double[] Cell_ars = new double[numRuns];
            double[] Cell_ahps = new double[numRuns];
            double[] Cell_ahrs = new double[numRuns];
            double[] Cell_dnaps = new double[numRuns];
            double[] Cell_dnars = new double[numRuns];
            double[] Cell_SS = new double[numRuns];
            double[] Cell_SHDs = new double[numRuns];
            double[] Row_aps = new double[numRuns];
            double[] Row_ars = new double[numRuns];
            double[] Row_ahps = new double[numRuns];
            double[] Row_ahrs = new double[numRuns];
            double[] Row_dnaps = new double[numRuns];
            double[] Row_dnars = new double[numRuns];
            double[] Row_SS = new double[numRuns];
            double[] Row_SHDs = new double[numRuns];


            for (int i = 0; i < numRuns; i++) {
                /* Create graph for testing purposes */
                Graph myGraph = GraphUtils.randomGraph(10, 0, 9, 3,4,4, false);


                //        System.out.println(myGraph);
                BayesPm pm = new BayesPm(myGraph,2,6);

                BayesIm im = new MlBayesIm(pm, MlBayesIm.RANDOM);

                SelectionBias testing = new SelectionBias(myGraph, pm, 3);

                BayesIm biasIm = new MlBayesIm(testing.getPm(), im, MlBayesIm.RANDOM);

                //        System.out.println("Bias IM" + biasIm);

                int uvars = testing.biasGraph.getNumNodes() / 2;
                for (int z = uvars; z < testing.biasGraph.getNumNodes(); z++) {
                    for (int r = 0; r < biasIm.getNumRows(z); r++) {
                        double p = Math.pow(RandomUtil.getInstance().nextUniform(0.05, 0.50), 2);
                        biasIm.setProbability(z, r, 0, p);
                        biasIm.setProbability(z, r, 1, 1 - p);
                    }
                }

                DataSet myData = biasIm.simulateData(x, true);
//        System.out.println("Dataset: " + myData);
//        System.out.println("Biasset: " + testing.BiasDataCell(myData));
//
//        System.out.println("Cell wise: " + testing.BiasDataCell(myData));
//        System.out.println("Row wise: " + testing.BiasDataRow(myData));
//
//        System.out.println("Cell wise alt: " + testing.BiasDataCellAlt(myData));
//        System.out.println("Row wise alt: " + testing.BiasDataRowAlt(myData));
//
//            System.out.println("Cell wise: " + testing.BiasDataCell(myData).getNumRows());
//            System.out.println("Row wise: " + testing.BiasDataRow(myData).getNumRows());
//            System.out.println("Cell wise alt: " + testing.BiasDataCellAlt(myData).getNumRows());
//            System.out.println("Row wise alt: " + testing.BiasDataRowAlt(myData).getNumRows());
//            System.out.println();
                Cell_SS[i] = testing.BiasDataCell(myData).getNumRows();
                Row_SS[i] = testing.BiasDataRow(myData).getNumRows();

//            System.out.println("True graph:" + testing.trueGraph.getEdges());

                double alpha = 0.05;

//            System.out.println("\n\tChi_Square_Tests\n");

//        System.out.println("FCI cell " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataCell(myData),alpha)).search()).getEdges());
//        System.out.println("FCI row " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataRow(myData),alpha)).search()).getEdges());
//
//        Graph trueGraph = removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataCell(myData),alpha)).search());
//        Graph estGraph = removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataRow(myData),alpha)).search());
//            System.out.println(testing.trueGraph.getEdges());
//            System.out.println("FCI cellalt " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataCellAlt(myData), alpha)).search()).getEdges());
//            System.out.println("FCI rowalt " + removeCircles(new Fci(new IndTestChiSquare(testing.BiasDataRowAlt(myData), alpha)).search()).getEdges());

                Graph Cell_estGraph = new Fci(new IndTestChiSquare(testing.BiasDataCellAlt(myData), alpha)).search();
                Graph Row_estGraph = new Fci(new IndTestChiSquare(testing.BiasDataRowAlt(myData), alpha)).search();

                testing.trueGraph = new DagToPag2(myGraph).convert();
//                System.out.println("True: " + testing.trueGraph.getEdges());


                AdjacencyPrecision ap = new AdjacencyPrecision();
                AdjacencyRecall ar = new AdjacencyRecall();
                ArrowheadPrecision ahp = new ArrowheadPrecision();
                ArrowheadRecall ahr = new ArrowheadRecall();
                DefiniteNonancestorPrecision dnap = new DefiniteNonancestorPrecision();
                DefiniteNonancestorRecall dnar = new DefiniteNonancestorRecall();
                SHD shd = new SHD();

                Cell_estGraph = GraphUtils.replaceNodes(Cell_estGraph, testing.trueGraph.getNodes());


//                System.out.println(testing.trueGraph);
//                testing.trueGraph.setPag(true);
//                System.out.println(testing.trueGraph);
//                Cell_estGraph.setPag(true);
//                Row_estGraph.setPag(true);
//                testing.trueGraph.setPag(true);

                double Cell_ap = ap.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_ar = ar.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_ahp = ahp.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_ahr = ahr.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_dnap = dnap.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_dnar = dnar.getValue(testing.trueGraph, Cell_estGraph);
                double Cell_shd = shd.getValue(testing.trueGraph, Cell_estGraph);

                double Row_ap = ap.getValue(testing.trueGraph, Row_estGraph);
                double Row_ar = ar.getValue(testing.trueGraph, Row_estGraph);
                double Row_ahp = ahp.getValue(testing.trueGraph, Row_estGraph);
                double Row_ahr = ahr.getValue(testing.trueGraph, Row_estGraph);
                double Row_dnap = dnap.getValue(testing.trueGraph, Row_estGraph);
                double Row_dnar = dnar.getValue(testing.trueGraph, Row_estGraph);
                double Row_shd = shd.getValue(testing.trueGraph, Row_estGraph);

                Cell_aps[i] = Cell_ap;
                Cell_ars[i] = Cell_ar;
                Cell_ahps[i] = Cell_ahp;
                Cell_ahrs[i] = Cell_ahr;
                Cell_dnaps[i] = Cell_dnap;
                Cell_dnars[i] = Cell_dnar;
                Cell_SHDs[i] = Cell_shd;

                Row_aps[i] = Row_ap;
                Row_ars[i] = Row_ar;
                Row_ahps[i] = Row_ahp;
                Row_ahrs[i] = Row_ahr;
                Row_dnaps[i] = Row_dnap;
                Row_dnars[i] = Row_dnar;
                Row_SHDs[i] = Row_shd;
            }

            Cell_SAM_final.add((double) x);
            Cell_aps_final.add(StatUtils.mean(Cell_aps));
            Cell_ars_final.add(StatUtils.mean(Cell_ars));
            Cell_ahps_final.add(StatUtils.mean(Cell_ahps));
            Cell_ahrs_final.add(StatUtils.mean(Cell_ahrs));
            Cell_dnaps_final.add(StatUtils.mean(Cell_dnaps));
            Cell_dnars_final.add(StatUtils.mean(Cell_dnars));
            Cell_SS_final.add(StatUtils.mean(Cell_SS));
            Cell_SHDs_final.add(StatUtils.mean(Cell_SHDs));

            Row_SAM_final.add((double) x);
            Row_aps_final.add(StatUtils.mean(Row_aps));
            Row_ars_final.add(StatUtils.mean(Row_ars));
            Row_ahps_final.add(StatUtils.mean(Row_ahps));
            Row_ahrs_final.add(StatUtils.mean(Row_ahrs));
            Row_dnaps_final.add(StatUtils.mean(Row_dnaps));
            Row_dnars_final.add(StatUtils.mean(Row_dnars));
            Row_SS_final.add(StatUtils.mean(Row_SS));
            Row_SHDs_final.add(StatUtils.mean(Row_SHDs));

//            System.out.println("\nCell_wise: " + x);
//
//            System.out.println("Cell_AP: " + StatUtils.mean(Cell_aps));
//            System.out.println("Cell_AR: " + StatUtils.mean(Cell_ars));
//            System.out.println("Cell_AHP: " + StatUtils.mean(Cell_ahps));
//            System.out.println("Cell_AHR: " + StatUtils.mean(Cell_ahrs));
//            System.out.println("Cell_DNAP: " + StatUtils.mean(Cell_dnaps));
//            System.out.println("Cell_DNAR: " + StatUtils.mean(Cell_dnars));
//            System.out.println("Cell_SS: " + StatUtils.mean(Cell_SS));
//            System.out.println("Cell_SHD: " + StatUtils.mean(Cell_SHDs));
//
//            System.out.println("\nRow-wise: " + x);
//
//            System.out.println("Row_AP: " + StatUtils.mean(Row_aps));
//            System.out.println("Row_AR: " + StatUtils.mean(Row_ars));
//            System.out.println("Row_AHP: " + StatUtils.mean(Row_ahps));
//            System.out.println("Row_AHR: " + StatUtils.mean(Row_ahrs));
//            System.out.println("Row_DNAP: " + StatUtils.mean(Row_dnaps));
//            System.out.println("Row_DNAR: " + StatUtils.mean(Row_dnars));
//            System.out.println("Row_SS: " + StatUtils.mean(Row_SS));
//            System.out.println("Row_SHD: " + StatUtils.mean(Row_SHDs));

//        System.out.println("PC cell " + new Pc(new IndTestChiSquare(testing.BiasDataCell(myData),alpha)).search().getEdges());
//        System.out.println("PC row " + new Pc(new IndTestChiSquare(testing.BiasDataRow(myData),alpha)).search().getEdges());
//
//        System.out.println("PC cellalt " + new Pc(new IndTestChiSquare(testing.BiasDataCellAlt(myData),alpha)).search().getEdges());
//        System.out.println("PC rowalt " + new Pc(new IndTestChiSquare(testing.BiasDataRowAlt(myData),alpha)).search().getEdges());
//
//        AdjacencyPrecision ap = new AdjacencyPrecision();
//        AdjacencyRecall ar = new AdjacencyRecall();
//
//        double _ap = ap.getValue(trueGraph, estGraph);
//        double _ar = ar.getValue(trueGraph, estGraph);
//
//        System.out.println("AP: " + _ap);
//        System.out.println("AR: " + _ar);


//        System.out.println("Special PC cell " + new PcStableMax(new IndTestChiSquare(testing.BiasDataCell(myData),alpha)).search().getEdges());
//
//        System.out.println("\n\tG_Square_Test\n");
//
//        System.out.println("FCI cell " + removeCircles(new Fci(new IndTestGSquare(testing.BiasDataCell(myData), alpha)).search()).getEdges());
//        System.out.println("FCI row " + removeCircles(new Fci(new IndTestGSquare(testing.BiasDataRow(myData), alpha)).search()).getEdges());
//
//        System.out.println("FCI cellalt " + removeCircles(new Fci(new IndTestGSquare(testing.BiasDataCellAlt(myData), alpha)).search()).getEdges());
//        System.out.println("FCI rowalt " + removeCircles(new Fci(new IndTestGSquare(testing.BiasDataRowAlt(myData), alpha)).search()).getEdges());
//
//        System.out.println("PC cell " + new Pc(new IndTestGSquare(testing.BiasDataCell(myData), alpha)).search().getEdges());
//        System.out.println("PC row " + new Pc(new IndTestGSquare(testing.BiasDataRow(myData), alpha)).search().getEdges());
//
//        System.out.println("PC cellalt " + new Pc(new IndTestGSquare(testing.BiasDataCellAlt(myData), alpha)).search().getEdges());
//        System.out.println("PC rowalt " + new Pc(new IndTestGSquare(testing.BiasDataRowAlt(myData), alpha)).search().getEdges());
//
//        System.out.println("Special PC cell " + new PcStableMax(new IndTestGSquare(testing.BiasDataCell(myData),alpha)).search().getEdges());
//
//        System.out.println("\n\tRegression_Test\n");
//
//        System.out.println("FCI cell " + new Fci(new IndTestRegression(testing.BiasDataCell(myData),alpha)).search().getEdges());
//        System.out.println("FCI row " + new Fci(new IndTestRegression(testing.BiasDataRow(myData),alpha)).search().getEdges());
//
//        System.out.println("FCI cellalt " + new Fci(new IndTestRegression(testing.BiasDataCellAlt(myData),alpha)).search().getEdges());
//        System.out.println("FCI rowalt " + new Fci(new IndTestRegression(testing.BiasDataRowAlt(myData),alpha)).search().getEdges());
//
//        System.out.println("PC cell " + new Pc(new IndTestRegression(testing.BiasDataCell(myData),alpha)).search().getEdges());
//        System.out.println("PC row " + new Pc(new IndTestRegression(testing.BiasDataRow(myData),alpha)).search().getEdges());
//
//        System.out.println("PC cellalt " + new Pc(new IndTestRegression(testing.BiasDataCellAlt(myData),alpha)).search().getEdges());
//        System.out.println("PC rowalt " + new Pc(new IndTestRegression(testing.BiasDataRowAlt(myData),alpha)).search().getEdges());
//        AdjacencyPrecision ap = new AdjacencyPrecision();
//        double ap = ap.getValue(trueDag, est)
        }
        System.out.println(
                "Test = {\"Samples\": " + Cell_SAM_final +
                        ", \"AP\": " + Cell_aps_final +
                        ", \"AR\": " + Cell_ars_final +
                        ", \"AHP\": " + Cell_ahps_final +
                        ", \"AHR\": " + Cell_ahrs_final +
                        ", \"DNAP\": " + Cell_dnaps_final +
                        ", \"DNAR\": " + Cell_dnars_final +
                        ", \"SS\": " + Cell_SS_final +
                        ", \"SHD\": " + Cell_SHDs_final + "}");

        System.out.println(
                "Row = {\"Samples\": " + Row_SAM_final +
                        ", \"AP\": " + Row_aps_final +
                        ", \"AR\": " + Row_ars_final +
                        ", \"AHP\": " + Row_ahps_final +
                        ", \"AHR\": " + Row_ahrs_final +
                        ", \"DNAP\": " + Row_dnaps_final +
                        ", \"DNAR\": " + Row_dnars_final +
                        ", \"SS\": " + Row_SS_final +
                        ", \"SHD\": " + Row_SHDs_final + "}");
    }

    public Graph removeCircles(Graph pag) {
        for (Edge x : pag.getEdges()) {
            if (x.getEndpoint1() == Endpoint.CIRCLE && x.getEndpoint2() != Endpoint.CIRCLE) {
                x.setEndpoint1(Endpoint.TAIL);
            }
            if (x.getEndpoint2() == Endpoint.CIRCLE && x.getEndpoint2() != Endpoint.CIRCLE) {
                x.setEndpoint2(Endpoint.TAIL);
            }
        }
        return pag;
    }

    public void test2() {

        /* Create graph for testing purposes */
        Graph graph = GraphUtils.randomGraph(3, 0, 2, 2, 2, 2, true);

        System.out.println("True Graph: " + graph.getEdges());

        BayesPm pm = new BayesPm(graph, 2, 6);
        SelectionBias testing = new SelectionBias(graph, pm, 2);

        System.out.println("Bias Graph: " + testing.biasGraph.getEdges());

        BayesPm biasPm = new BayesPm(testing.biasGraph, pm);
        BayesIm im = new MlBayesIm(biasPm, MlBayesIm.RANDOM);

        /* Generate Random dataset */
        DataSet myData = im.simulateData(100, false);

//        System.out.println("Data-set: " + myData);
//
//        System.out.println("Clean Data: " + testing.BiasDataRow(myData));
//        System.out.println("Clean Data Alt: " + testing.BiasDataRowAlt(myData));
        double alpha = 0.1;
        System.out.println(new Fci(new IndTestChiSquare(testing.BiasDataRowAlt(myData), alpha)).search());
        System.out.println(new Fci(new IndTestChiSquare(testing.BiasDataCellAlt(myData), alpha)).search());
        System.out.println(new Pc(new IndTestChiSquare(testing.BiasDataCellAlt(myData), alpha)).search());


    }

    public void test3() {
        /* Load Dataset from file */
        DataReader dataReader = new DataReader();
        dataReader.setVariablesSupplied(true);
        dataReader.setDelimiter(DelimiterType.TAB);
        dataReader.setMaxIntegralDiscrete(3);
        dataReader.setCommentMarker("//");
        dataReader.setMissingValueMarker("*");
        DataSet myData = null;
        try {
            myData = dataReader.parseTabular(new File("/home/israel/Desktop/IBRIC_RESEARCH/Simulated_Data2/save/data/data.1.txt"));
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

        for (int i = 0; i < myData.getNumRows(); i++) {
            if (myData.getInt(i, myData.getColumn(u2)) == 0) {
                myData.setInt(i, myData.getColumn(y3), -99);
            }
        }

//        System.out.println(myData);

        myData.removeColumn(u2);

//        DataSet BiasedData = SelectionBias.selectiveRemoval(myGraph, 1);
//        System.out.println(BiasedData);
    }


    public static void main(String... args) {
        new TestSelectionBias().testx();
//        new TestSelectionBias().test1();
//        new TestSelectionBias().test2();
    }
}