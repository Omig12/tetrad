package edu.cmu.tetrad.search;

import edu.cmu.tetrad.algcomparison.independence.ChiSquare;
import edu.cmu.tetrad.data.BootstrapSampler;
import edu.cmu.tetrad.data.CorrelationMatrixOnTheFly;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.ConcurrencyUtils;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.TetradSerializable;
import edu.cmu.tetrad.util.TextTable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An adaptation of the CStaR algorithm (Steckoven et al., 2012).
 * <p>
 * Stekhoven, D. J., Moraes, I., Sveinbjörnsson, G., Hennig, L., Maathuis, M. H., & Bühlmann, P. (2012). Causal stability ranking. Bioinformatics, 28(21), 2819-2823.
 * <p>
 * Meinshausen, N., & Bühlmann, P. (2010). Stability selection. Journal of the Royal Statistical Society: Series B (Statistical Methodology), 72(4), 417-473.
 * <p>
 * Colombo, D., & Maathuis, M. H. (2014). Order-independent constraint-based causal structure learning. The Journal of Machine Learning Research, 15(1), 3741-3782.
 *
 * @author jdramsey@andrew.cmu.edu
 */
public class CStaSMulti {

    private int maxTrekLength = 15;
    private int numSubsamples = 30;
    private int parallelism = Runtime.getRuntime().availableProcessors() * 10;
    private Graph trueDag = null;
    private IndependenceTest test;
    private double lift = 2.0;

    // A single record in the returned table.
    public static class Record implements TetradSerializable {
        private Node predictor;
        private Node target;
        private double pi;
        private double effect;
        private double pcer;
        private double er;
        private boolean ancestor;
        private boolean existsTrekToTarget;

        Record(Node predictor, Node target, double pi, double minEffect, double pcer, double er, boolean ancestor, boolean existsTrekToTarget) {
            this.predictor = predictor;
            this.target = target;
            this.pi = pi;
            this.effect = minEffect;
            this.pcer = pcer;
            this.er = er;
            this.ancestor = ancestor;
            this.existsTrekToTarget = existsTrekToTarget;
        }

        public Node getPredictor() {
            return predictor;
        }

        public Node getTarget() {
            return target;
        }

        public double getPi() {
            return pi;
        }

        public double getEffect() {
            return effect;
        }

        double getPcer() {
            return pcer;
        }

        double getEr() {
            return er;
        }

        public boolean isAncestor() {
            return ancestor;
        }

        boolean isExistsTrekToTarget() {
            return existsTrekToTarget;
        }
    }

    public CStaSMulti() {
    }

    /**
     * Returns records for a set of variables with expected number of false positives bounded by q.
     *
     * @param dataSet            The full datasets to search over.
     * @param possiblePredictors A set of variables in the datasets over which to search.
     * @param targets            The target variables.
     * @param test               This test is only used to make more tests like it for subsamples.
     */
    public List<Record> getRecords(DataSet dataSet, List<Node> possiblePredictors, List<Node> targets, IndependenceTest test) {
        if (test instanceof IndTestScore && ((IndTestScore) test).getWrappedScore() instanceof SemBicScore) {
            this.test = test;
        } else if (test instanceof IndTestFisherZ) {
            this.test = test;
        } else if (test instanceof ChiSquare) {
            this.test = test;
        } else if (test instanceof IndTestScore && ((IndTestScore) test).getWrappedScore() instanceof ConditionalGaussianScore) {
            this.test = test;
        } else {
            throw new IllegalArgumentException("That test is not configured.");
        }

        class Tuple {
            private Node predictor;
            private Node target;
            private double pi;

            public Tuple(Node predictor, Node target, double pi) {
                this.predictor = predictor;
                this.target = target;
                this.pi = pi;
            }

            public Node getPredictor() {
                return predictor;
            }

            public Node getTarget() {
                return target;
            }

            public double getPi() {
                return pi;
            }
        }

        targets = GraphUtils.replaceNodes(targets, dataSet.getVariables());
        possiblePredictors = GraphUtils.replaceNodes(possiblePredictors, dataSet.getVariables());
        final DataSet selection = dataSet.subsetColumns(possiblePredictors);

        final List<Node> variables = selection.getVariables();
        variables.removeAll(targets);

        final List<Map<Integer, Map<Node, Double>>> minimalEffects = new ArrayList<>();

        for (int t = 0; t < targets.size(); t++) {
            minimalEffects.add(new ConcurrentHashMap<>());

            for (int b = 0; b < getNumSubsamples(); b++) {
                final Map<Node, Double> map = new ConcurrentHashMap<>();
                for (Node node : possiblePredictors) map.put(node, 0.0);
                minimalEffects.get(t).put(b, map);
            }
        }

        final List<List<Ida.NodeEffects>> effects = new ArrayList<>();
        final List<Node> _targets = new ArrayList<>(targets);

        for (int t = 0; t < targets.size(); t++) {
            effects.add(new ArrayList<>());
        }

        class Task implements Callable<Boolean> {
            private Task() {
            }

            public Boolean call() {
                try {
                    BootstrapSampler sampler = new BootstrapSampler();
                    sampler.setWithoutReplacements(false);
                    DataSet sample = sampler.sample(selection, selection.getNumRows());
                    Graph pattern = getPatternFges(sample);
                    Ida ida = new Ida(sample, pattern);

                    for (int t = 0; t < _targets.size(); t++) {
                        effects.get(t).add(ida.getSortedMinEffects(_targets.get(t)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            }
        }

        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int b = 0; b < getNumSubsamples(); b++) {
            tasks.add(new Task());
        }

        ConcurrencyUtils.runCallables(tasks, getParallelism());

        List<Tuple> outTuples = new ArrayList<>();
        int bestQ = -1;

        final List<Map<Node, Integer>> counts = new ArrayList<>();

        for (int t = 0; t < targets.size(); t++) {
            counts.add(new HashMap<>());
            for (Node node : possiblePredictors) counts.get(t).put(node, 0);
        }

        final int p = possiblePredictors.size() * targets.size();

        double maxEv = 0.0;

        for (int q = 1; q <= p / 2; q++) {
            for (int t = 0; t < targets.size(); t++) {
                for (Ida.NodeEffects _effects : effects.get(t)) {
                    if (q - 1 < _effects.getNodes().size()) {
                        final Node key = _effects.getNodes().get(q - 1);
                        counts.get(t).put(key, counts.get(t).get(key) + 1);
                    }
                }
            }

            for (int t = 0; t < targets.size(); t++) {
                for (int b = 0; b < effects.get(t).size(); b++) {
                    Ida.NodeEffects _effects = effects.get(t).get(b);

                    for (int r = 0; r < _effects.getNodes().size(); r++) {
                        Node n = _effects.getNodes().get(r);
                        Double e = _effects.getEffects().get(r);
                        minimalEffects.get(t).get(b).put(n, e);
                    }
                }
            }

            List<List<Double>> _pi = new ArrayList<>();
            List<List<Node>> _sortedVariables = new ArrayList<>();

            for (int t = 0; t < targets.size(); t++) {
                final int _t = t;
                List<Node> sortedVariables = new ArrayList<>(possiblePredictors);
                sortedVariables.sort((o1, o2) -> Integer.compare(counts.get(_t).get(o2), counts.get(_t).get(o1)));
                _sortedVariables.add(sortedVariables);

                List<Double> pi = new ArrayList<>();

                for (Node v : sortedVariables) {
                    final Integer count = counts.get(t).get(v);
                    pi.add(count / ((double) getNumSubsamples()));
                }

                _pi.add(pi);
            }

            // Need to put all of the lists together.

            List<Tuple> tuples = new ArrayList<>();

            for (int t = 0; t < targets.size(); t++) {
                for (int v = 0; v < _sortedVariables.get(t).size(); v++) {
                    tuples.add(new Tuple(_sortedVariables.get(t).get(v), targets.get(t), _pi.get(t).get(v)));
                }
            }

            tuples.sort((o1, o2) -> Double.compare(o2.getPi(), o1.getPi()));

            double sum = 0.0;

            for (int g = 0; g < q; g++) {
                sum += tuples.get(g).getPi();
            }

            if (sum >= getLift() * q * q / (double) p) {
                List<Tuple> _outTuples = new ArrayList<>();

                for (int i = 0; i < q; i++) {
                    _outTuples.add(tuples.get(i));
                }

                if (_outTuples.size() > outTuples.size()) {
                    outTuples = _outTuples;
                    bestQ = q;
                }

                double ev =  q - sum;

                if (ev > maxEv) {
                    maxEv = ev;
                }
            } else {
                break;
            }
        }

        System.out.println("q = " + bestQ);

        trueDag = GraphUtils.replaceNodes(trueDag, possiblePredictors);
        trueDag = GraphUtils.replaceNodes(trueDag, targets);

        List<Record> records = new ArrayList<>();

        for (Tuple tuple : outTuples) {
            //            double er = er(outPis.get(i), outTuples.size(), p);
            final double pcer = pcer(tuple.getPi(), bestQ, p);

            List<Double> e = new ArrayList<>();

            for (int b = 0; b < getNumSubsamples(); b++) {
                final double m = minimalEffects.get(targets.indexOf(tuple.getTarget())).get(b).get(tuple.getPredictor());
                e.add(m);
            }

            double[] _e = new double[e.size()];
            for (int t = 0; t < e.size(); t++) _e[t] = e.get(t);
            double avg = StatUtils.mean(_e);
            boolean ancestor = false;

            if (trueDag != null) {
                ancestor = trueDag.isAncestorOf(tuple.getPredictor(), tuple.getTarget());
            }

            boolean trekToTarget = false;

            if (trueDag != null) {
                List<List<Node>> treks = GraphUtils.treks(trueDag, tuple.getPredictor(), tuple.getTarget(), maxTrekLength);
                trekToTarget = !treks.isEmpty();
            }

            records.add(new Record(tuple.getPredictor(), tuple.getTarget(), tuple.getPi(), avg, pcer, maxEv, ancestor, trekToTarget));
        }

        records.sort((o1, o2) -> {
            if (o1.getPi() == o2.getPi()) {
                return Double.compare(o2.effect, o1.effect);
            } else {
                return 0;
            }
        });

        return records;
    }

    /**
     * Returns a text table from the given records
     */
    public String makeTable(List<Record> records) {
        TextTable table = new TextTable(records.size() + 1, 9);
        NumberFormat nf = new DecimalFormat("0.0000");

        table.setToken(0, 0, "Index");
        table.setToken(0, 1, "Predictor");
        table.setToken(0, 2, "Target");
        table.setToken(0, 3, "Type");
        table.setToken(0, 4, "A");
        table.setToken(0, 5, "T");
        table.setToken(0, 6, "PI");
        table.setToken(0, 7, "Average Effect");
        table.setToken(0, 8, "PCER");
//        table.setToken(0, 8, "ER");

        int fp = 0;

        for (int i = 0; i < records.size(); i++) {
            final Node predictor = records.get(i).getPredictor();
            final Node target = records.get(i).getTarget();
            final boolean ancestor = records.get(i).isAncestor();
            final boolean existsTrekToTarget = records.get(i).isExistsTrekToTarget();
            if (!(ancestor)) fp++;

            table.setToken(i + 1, 0, "" + (i + 1));
            table.setToken(i + 1, 1, predictor.getName());
            table.setToken(i + 1, 2, target.getName());
            table.setToken(i + 1, 3, predictor instanceof DiscreteVariable ? "D" : "C");
            table.setToken(i + 1, 4, ancestor ? "A" : "");
            table.setToken(i + 1, 5, existsTrekToTarget ? "T" : "");
            table.setToken(i + 1, 6, nf.format(records.get(i).getPi()));
            table.setToken(i + 1, 7, nf.format(records.get(i).getEffect()));
            table.setToken(i + 1, 8, nf.format(records.get(i).getPcer()));
//            table.setToken(i + 1, 8, nf.format(records.get(i).getEr()));
        }
        final double er = !records.isEmpty() ? records.get(0).getEr() : Double.NaN;

        return "\n" + table + "\n" + "# FP = " + fp + " E(V) = " + nf.format(er) +
                "\n\nT = exists a trek of length no more than " + maxTrekLength + " to the target" +
                "\nA = ancestor of the target" +
                "\nType: C = continuous, D = discrete\n";
    }

    /**
     * Makes a graph of the estimated predictors to the target.
     */
    public Graph makeGraph(Node y, List<Record> records) {
        List<Node> outNodes = new ArrayList<>();
        for (Record record : records) outNodes.add(record.getPredictor());

        Graph graph = new EdgeListGraph(outNodes);
        graph.addNode(y);

        for (int i = 0; i < new ArrayList<>(outNodes).size(); i++) {
            graph.addDirectedEdge(outNodes.get(i), y);
        }

        return graph;
    }

    public void setNumSubsamples(int numSubsamples) {
        this.numSubsamples = numSubsamples;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public void setTrueDag(Graph trueDag) {
        this.trueDag = trueDag;
    }

    public void setLift(double lift) {
        this.lift = lift;
    }

    //=============================PRIVATE==============================//

    private int getNumSubsamples() {
        return numSubsamples;
    }

    private int getParallelism() {
        return parallelism;
    }

    private double getLift() {
        return lift;
    }

    private Graph getPattern(DataSet sample) {
        IndependenceTest test = getIndependenceTest(sample, this.test);
        PcAll pc = new PcAll(test, null);
        pc.setFasRule(PcAll.FasRule.FAS_STABLE);
        pc.setConflictRule(PcAll.ConflictRule.OVERWRITE);
        pc.setColliderDiscovery(PcAll.ColliderDiscovery.FAS_SEPSETS);
        return pc.search();
    }

    private Graph getPatternFges(DataSet sample) {
        Score score = new ScoredIndTest(getIndependenceTest(sample, this.test));
        Fges fges = new Fges(score);
        return fges.search();
    }

    private Graph getPatternFgesMb(DataSet sample, Node target) {
        Score score = new ScoredIndTest(getIndependenceTest(sample, this.test));
        FgesMb fges = new FgesMb(score);
        return fges.search(target);
    }

    private IndependenceTest getIndependenceTest(DataSet sample, IndependenceTest test) {
        if (test instanceof IndTestScore && ((IndTestScore) test).getWrappedScore() instanceof SemBicScore) {
            SemBicScore score = new SemBicScore(new CorrelationMatrixOnTheFly(sample));
            score.setPenaltyDiscount(((SemBicScore) ((IndTestScore) test).getWrappedScore()).getPenaltyDiscount());
            return new IndTestScore(score);
        } else if (test instanceof IndTestFisherZ) {
            double alpha = test.getAlpha();
            return new IndTestFisherZ(new CorrelationMatrixOnTheFly(sample), alpha);
        } else if (test instanceof ChiSquare) {
            double alpha = test.getAlpha();
            return new IndTestFisherZ(sample, alpha);
        } else if (test instanceof IndTestScore && ((IndTestScore) test).getWrappedScore() instanceof ConditionalGaussianScore) {
            ConditionalGaussianScore score = (ConditionalGaussianScore) ((IndTestScore) test).getWrappedScore();
            double penaltyDiscount = score.getPenaltyDiscount();
            ConditionalGaussianScore _score = new ConditionalGaussianScore(sample, 1, false);
            _score.setPenaltyDiscount(penaltyDiscount);
            return new IndTestScore(_score);
        } else {
            throw new IllegalArgumentException("That test is not configured: " + test);
        }
    }

    // E(|V|) bound
    private static double er(double pi, double q, double p) {
        return q * q / (p * (2 * pi - 1));
    }

    // Per comparison error rate.
    private static double pcer(double pi, double q, double p) {
        double v = (q * q) / (p * p * (2 * pi - 1));
        if (v < 0 || v > 1) v = 1;
        return v;
    }

    public List<Node> selectVariables(DataSet dataSet, List<Node> _y, double alpha, int parallelism) {
        IndependenceTest test = new IndTestFisherZ(dataSet, alpha);
        List<Node> selection = new CopyOnWriteArrayList<>();

        final List<Node> variables = dataSet.getVariables();
        _y = GraphUtils.replaceNodes(_y, test.getVariables());

        final List<Node> y = new ArrayList<>(_y);

        {
            class Task implements Callable<Boolean> {
                private int from;
                private int to;
                private List<Node> y;

                private Task(int from, int to, List<Node> y) {
                    this.from = from;
                    this.to = to;
                    this.y = y;
                }

                @Override
                public Boolean call() {
                    for (int n = from; n < to; n++) {
                        final Node node = variables.get(n);
                        if (!y.contains(node)) {
                            for (Node target : y) {
                                if (!test.isIndependent(node, target)) {
                                    if (!selection.contains(node)) {
                                        selection.add(node);
                                    }
                                }
                            }
                        }
                    }

                    return true;
                }
            }

            final int chunk = 50;
            List<Callable<Boolean>> tasks;

            {
                tasks = new ArrayList<>();

                for (int from = 0; from < variables.size(); from += chunk) {
                    final int to = Math.min(variables.size(), from + chunk);
                    tasks.add(new Task(from, to, y));
                }

                ConcurrencyUtils.runCallables(tasks, parallelism);
            }

            test.setAlpha(0.00001);

            {
                tasks = new ArrayList<>();

                for (int from = 0; from < variables.size(); from += chunk) {
                    final int to = Math.min(variables.size(), from + chunk);
                    tasks.add(new Task(from, to, new ArrayList<>(selection)));
                }

                ConcurrencyUtils.runCallables(tasks, parallelism);
            }
        }

        for (Node target : y) {
            if (!selection.contains(target)) selection.add(target);
        }

        System.out.println("# selected variables = " + selection.size());

        return selection;
    }
}