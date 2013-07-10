
// ----------------------------------------------------------------------
// This code is developed as part of the Java CoG Kit project
// The terms of the license can be found at http://www.cogkit.org/license
// This message may not be removed or altered.
// ----------------------------------------------------------------------

/*
 * 
 * Created on Jan 23, 2004
 */
package org.globus.cog.gui.grapheditor.targets.html;

import java.awt.Dimension;

import org.globus.cog.gui.grapheditor.canvas.AbstractCanvasRenderer;

public class GenericHTMLCanvasRenderer extends AbstractCanvasRenderer {
	
	public GenericHTMLCanvasRenderer() {
		setView(new HtmlGraphView());
	}
	
	public void setSize(Dimension dimension) {
	}

}
