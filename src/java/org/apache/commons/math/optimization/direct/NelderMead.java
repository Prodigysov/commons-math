/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.math.optimization.direct;

import java.util.Comparator;

import org.apache.commons.math.optimization.ObjectiveException;
import org.apache.commons.math.optimization.PointValuePair;

/** 
 * This class implements the Nelder-Mead direct search method.
 *
 * @version $Revision$ $Date$
 * @see MultiDirectional
 * @since 1.2
 */
public class NelderMead extends DirectSearchOptimizer {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -5810365844886183056L;

    /** Reflection coefficient. */
    private final double rho;

    /** Expansion coefficient. */
    private final double khi;

    /** Contraction coefficient. */
    private final double gamma;

    /** Shrinkage coefficient. */
    private final double sigma;

    /** Build a Nelder-Mead optimizer with default coefficients.
     * <p>The default coefficients are 1.0 for rho, 2.0 for khi and 0.5
     * for both gamma and sigma.</p>
     */
    public NelderMead() {
        this.rho   = 1.0;
        this.khi   = 2.0;
        this.gamma = 0.5;
        this.sigma = 0.5;
    }

    /** Build a Nelder-Mead optimizer with specified coefficients.
     * @param rho reflection coefficient
     * @param khi expansion coefficient
     * @param gamma contraction coefficient
     * @param sigma shrinkage coefficient
     */
    public NelderMead(final double rho, final double khi,
                      final double gamma, final double sigma) {
        this.rho   = rho;
        this.khi   = khi;
        this.gamma = gamma;
        this.sigma = sigma;
    }

    /** {@inheritDoc} */
    protected void iterateSimplex(final Comparator<PointValuePair> comparator)
        throws ObjectiveException {

        // the simplex has n+1 point if dimension is n
        final int n = simplex.length - 1;

        // interesting values
        final PointValuePair best       = simplex[0];
        final PointValuePair secondBest = simplex[n-1];
        final PointValuePair worst      = simplex[n];
        final double[] xWorst = worst.getPoint();

        // compute the centroid of the best vertices
        // (dismissing the worst point at index n)
        final double[] centroid = new double[n];
        for (int i = 0; i < n; ++i) {
            final double[] x = simplex[i].getPoint();
            for (int j = 0; j < n; ++j) {
                centroid[j] += x[j];
            }
        }
        final double scaling = 1.0 / n;
        for (int j = 0; j < n; ++j) {
            centroid[j] *= scaling;
        }

        // compute the reflection point
        final double[] xR = new double[n];
        for (int j = 0; j < n; ++j) {
            xR[j] = centroid[j] + rho * (centroid[j] - xWorst[j]);
        }
        final PointValuePair reflected = new PointValuePair(xR, evaluate(xR));

        if ((comparator.compare(best, reflected) <= 0) &&
            (comparator.compare(reflected, secondBest) < 0)) {

            // accept the reflected point
            replaceWorstPoint(reflected, comparator);

        } else if (comparator.compare(reflected, best) < 0) {

            // compute the expansion point
            final double[] xE = new double[n];
            for (int j = 0; j < n; ++j) {
                xE[j] = centroid[j] + khi * (xR[j] - centroid[j]);
            }
            final PointValuePair expanded = new PointValuePair(xE, evaluate(xE));

            if (comparator.compare(expanded, reflected) < 0) {
                // accept the expansion point
                replaceWorstPoint(expanded, comparator);
            } else {
                // accept the reflected point
                replaceWorstPoint(reflected, comparator);
            }

        } else {

            if (comparator.compare(reflected, worst) < 0) {

                // perform an outside contraction
                final double[] xC = new double[n];
                for (int j = 0; j < n; ++j) {
                    xC[j] = centroid[j] + gamma * (xR[j] - centroid[j]);
                }
                final PointValuePair outContracted = new PointValuePair(xC, evaluate(xC));

                if (comparator.compare(outContracted, reflected) <= 0) {
                    // accept the contraction point
                    replaceWorstPoint(outContracted, comparator);
                    return;
                }

            } else {

                // perform an inside contraction
                final double[] xC = new double[n];
                for (int j = 0; j < n; ++j) {
                    xC[j] = centroid[j] - gamma * (centroid[j] - xWorst[j]);
                }
                final PointValuePair inContracted = new PointValuePair(xC, evaluate(xC));

                if (comparator.compare(inContracted, worst) < 0) {
                    // accept the contraction point
                    replaceWorstPoint(inContracted, comparator);
                    return;
                }

            }

            // perform a shrink
            final double[] xSmallest = simplex[0].getPoint();
            for (int i = 1; i < simplex.length; ++i) {
                final double[] x = simplex[i].getPoint();
                for (int j = 0; j < n; ++j) {
                    x[j] = xSmallest[j] + sigma * (x[j] - xSmallest[j]);
                }
                simplex[i] = new PointValuePair(x, Double.NaN);
            }
            evaluateSimplex(comparator);

        }

    }

}
