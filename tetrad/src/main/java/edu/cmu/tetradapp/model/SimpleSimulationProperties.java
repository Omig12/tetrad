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

package edu.cmu.tetradapp.model;

import edu.cmu.tetrad.util.TetradSerializable;
import edu.cmu.tetrad.util.TetradSerializableUtils;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Represents simulation properties for a simple simulation from an IM. Sample
 * size is the only property.
 *
 * @author Joseph Ramsey jdramsey@andrew.cmu.edu
 */
public class SimpleSimulationProperties implements TetradSerializable {
    static final long serialVersionUID = 23L;

    /**
     * The sample size.
     *
     * @serial Range greater than 0.
     */
    private int sampleSize = 1;

    /**
     * Whether data for latents should be saved out.
     */
    private boolean latentDataSaved;

    //=============================CONSTRUCTORS========================//

    /**
     * The required default constructor.
     */
    public SimpleSimulationProperties() {
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @see edu.cmu.TestSerialization
     * @see TetradSerializableUtils
     */
    public static SimpleSimulationProperties serializableInstance() {
        return new SimpleSimulationProperties();
    }

    //=============================PUBLIC METHODS=======================//

    /**
     * Retrieves the sample size.
     */
    public int getSampleSize() {
        return this.sampleSize;
    }

    /**
     * Sets the sample size.
     */
    public void setSampleSize(int sampleSize) {
        if (sampleSize < 0) {
            throw new IllegalArgumentException();
        }

        this.sampleSize = sampleSize;
    }


    public boolean isLatentDataSaved() {
        return latentDataSaved;
    }

    public void setLatentDataSaved(boolean latentDataSaved) {
        this.latentDataSaved = latentDataSaved;
    }

    /**
     * Adds semantic checks to the default deserialization method. This method
     * must have the standard signature for a readObject method, and the body of
     * the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from
     * version to version. A readObject method of this form may be added to any
     * class, even if Tetrad sessions were previously saved out using a version
     * of the class that didn't include it. (That's what the
     * "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for help.
     *
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (sampleSize < 1) {
            throw new IllegalStateException("Sample size < 1: " + sampleSize);
        }
    }
}





