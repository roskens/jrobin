/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.sourceforge.net/projects/jrobin
 * Project Lead:  Sasa Markovic (saxon@eunet.yu);
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

package jrobin.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;

/**
 *
 */
class ArcState implements RrdUpdater {
	private Archive parentArc;

	private RrdDouble accumValue;
	private RrdLong nanSteps;

	// create for the first time
	ArcState(Archive parentArc, boolean newState) throws IOException {
		this.parentArc = parentArc;
		accumValue = new RrdDouble(this);
		nanSteps = new RrdLong(this);
		if(newState) {
			// should initialize
			Header header = parentArc.getParentDb().getHeader();
			long step = header.getStep();
			long lastUpdateTime = header.getLastUpdateTime();
			long arcStep = parentArc.getArcStep();
			long nan = (Util.normalize(lastUpdateTime, step) -
				Util.normalize(lastUpdateTime, arcStep)) / step;
			nanSteps.set(nan);
			accumValue.set(Double.NaN);
		}
	}

	public RrdFile getRrdFile() {
		return parentArc.getParentDb().getRrdFile();
	}

	String dump() throws IOException {
		return "accumValue:" + accumValue.get() + " nanSteps:" + nanSteps.get() + "\n";
	}

	void setNanSteps(long value) throws IOException {
		nanSteps.set(value);
	}

	long getNanSteps() throws IOException {
		return nanSteps.get();
	}

	void setAccumValue(double value) throws IOException {
		accumValue.set(value);
	}

	double getAccumValue() throws IOException {
		return accumValue.get();
	}

	void appendXml(Element parent) throws IOException {
        Document doc = parent.getOwnerDocument();
		Element dsElem = doc.createElement("ds");
        Element valueElem = doc.createElement("value");
		valueElem.appendChild(doc.createTextNode(Util.formatDoubleXml(accumValue.get())));
		Element unknownElem = doc.createElement("unknown_datapoints");
		unknownElem.appendChild(doc.createTextNode("" + nanSteps.get()));
		parent.appendChild(dsElem);
        dsElem.appendChild(valueElem);
		dsElem.appendChild(unknownElem);
	}

}