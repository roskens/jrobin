/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org)
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package org.jrobin.graph;

import org.jrobin.core.RrdException;
import org.jrobin.core.Util;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Class used to interpolate datasource values from the collection of (timestamp, values)
 * points. This class is suitable for linear interpolation only. <p>
 *
 * Interpolation algorithm returns different values based on the value passed to
 * {@link #setInterpolationMethod(int) setInterpolationMethod()}. If not set, interpolation
 * method defaults to standard linear interpolation. Interpolation method handles NaN datasource
 * values gracefully.<p>
 *
 * Pass instances of this class to {@link RrdGraphDef#datasource(String, Plottable)
 * RrdGraphDef.datasource()} to provide interpolated datasource values to JRobin grapher.<p>
 */
public class LinearInterpolator extends Plottable {
	/** constant used to specify LEFT interpolation.
	 * See {@link #setInterpolationMethod(int) setInterpolationMethod()} for explanation. */
	public static final int INTERPOLATE_LEFT = 0;
	/** constant used to specify RIGHT interpolation.
	 * See {@link #setInterpolationMethod(int) setInterpolationMethod()} for explanation. */
	public static final int INTERPOLATE_RIGHT = 1;
	/** constant used to specify LINEAR interpolation (default interpolation method).
	 * See {@link #setInterpolationMethod(int) setInterpolationMethod()} for explanation. */
	public static final int INTERPOLATE_LINEAR = 2;

	private int lastIndexUsed = 0;

	private int interpolationMethod = INTERPOLATE_LINEAR;

	private long[] timestamps;
	private double[] values;

	/**
	 * Creates LinearInterpolator from arrays of timestamps and corresponding datasource values.
	 * @param timestamps timestamps in seconds
	 * @param values corresponding datasource values
	 * @throws RrdException Thrown if supplied arrays do not contain at least two values, or if
	 * timestamps are not ordered, or array lengths are not equal.
	 */
	public LinearInterpolator(long[] timestamps, double[] values) throws RrdException {
		this.timestamps = timestamps;
		this.values = values;
		validate();
	}

	/**
	 * Creates LinearInterpolator from arrays of timestamps and corresponding datasource values.
	 * @param dates Array of Date objects
	 * @param values corresponding datasource values
	 * @throws RrdException Thrown if supplied arrays do not contain at least two values, or if
	 * timestamps are not ordered, or array lengths are not equal.
	 */
	public LinearInterpolator(Date[] dates, double[] values) throws RrdException {
		this.values = values;
		timestamps = new long[dates.length];
		for(int i = 0; i < dates.length; i++) {
			timestamps[i] = Util.getTimestamp(dates[i]);
		}
		validate();
	}

	/**
	 * Creates LinearInterpolator from arrays of timestamps and corresponding datasource values.
	 * @param dates array of GregorianCalendar objects
	 * @param values corresponding datasource values
	 * @throws RrdException Thrown if supplied arrays do not contain at least two values, or if
	 * timestamps are not ordered, or array lengths are not equal.
	 */
	public LinearInterpolator(GregorianCalendar[] dates, double[] values) throws RrdException {
		this.values = values;
		timestamps = new long[dates.length];
		for(int i = 0; i < dates.length; i++) {
			timestamps[i] = Util.getTimestamp(dates[i]);
		}
		validate();
	}

	private void validate() throws RrdException {
		boolean ok = true;
		if(timestamps.length != values.length || timestamps.length < 2) {
			ok = false;
		}
		for(int i = 0; i < timestamps.length - 1 && ok; i++) {
			if(timestamps[i] >= timestamps[i + 1]) {
				ok = false;
			}
		}
		if(!ok) {
			throw new RrdException("Invalid plottable data supplied");
		}
	}

	/**
	 * Sets interpolation method to be used. Suppose that we have two timestamp/value pairs:<br>
	 * <code>(t, 100)</code> and <code>(t + 100, 300)</code>. Here are the results interpolator
	 * returns for t + 50 seconds, for various <code>interpolationMethods</code>:<p>
	 * <ul>
	 * <li><code>INTERPOLATE_LEFT:   100</code>
	 * <li><code>INTERPOLATE_RIGHT:  300</code>
	 * <li><code>INTERPOLATE_LINEAR: 200</code>
	 * </ul>
	 * If not set, interpolation method defaults to <code>INTERPOLATE_LINEAR</code>.
	 * @param interpolationMethod Should be <code>INTERPOLATE_LEFT</code>,
	 * <code>INTERPOLATE_RIGHT</code> or <code>INTERPOLATE_LINEAR</code>.
	 */
	public void setInterpolationMethod(int interpolationMethod) {
		this.interpolationMethod = interpolationMethod;
	}

	/**
	 * Method overriden from the base class. This method will be called by the framework. Call
	 * this method only if you need interpolated values in your code.
	 * @param timestamp timestamp in seconds
	 * @return inteprolated datasource value
	 */
	public double getValue(long timestamp) {
		int count = timestamps.length;
		// check if out of range
		if(timestamp < timestamps[0] || timestamp > timestamps[count - 1]) {
			return Double.NaN;
		}
		// find matching segment
		int startIndex = lastIndexUsed;
		if(timestamp < timestamps[lastIndexUsed]) {
			// backward reading, shift to the first timestamp
			startIndex = 0;
		}
		for(int i = startIndex; i < count; i++) {
			if(timestamps[i] == timestamp) {
				return values[i];
			}
			if(i < count - 1 && timestamps[i] < timestamp && timestamp < timestamps[i + 1]) {
				// matching segment found
				lastIndexUsed = i;
				switch(interpolationMethod) {
					case INTERPOLATE_LEFT:
						return values[i];
					case INTERPOLATE_RIGHT:
						return values[i + 1];
					case INTERPOLATE_LINEAR:
						double slope = (values[i + 1] - values[i]) /
							(timestamps[i + 1] - timestamps[i]);
						return values[i] + slope * (timestamp - timestamps[i]);
					default:
						return Double.NaN;
				}
			}
		}
		// should not be here ever, but let's satisfy the compiler
		return Double.NaN;
	}
}