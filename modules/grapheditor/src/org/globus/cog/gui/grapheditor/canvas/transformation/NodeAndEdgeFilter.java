
// ----------------------------------------------------------------------
// This code is developed as part of the Java CoG Kit project
// The terms of the license can be found at http://www.cogkit.org/license
// This message may not be removed or altered.
// ----------------------------------------------------------------------

    
package org.globus.cog.gui.grapheditor.canvas.transformation;

import java.util.Iterator;

import org.globus.cog.util.graph.Edge;
import org.globus.cog.util.graph.Graph;
import org.globus.cog.util.graph.GraphInterface;
import org.globus.cog.util.graph.Node;

public class NodeAndEdgeFilter implements GraphTransformation {

	private Class nodeFilter;
	
	private Class edgeFilter;
	
	public NodeAndEdgeFilter(Class nodeFilter, Class edgeFilter){
		this.nodeFilter = nodeFilter;
		this.edgeFilter = edgeFilter;
	}
	
	public GraphInterface transform(GraphInterface graph) {
		Graph newGraph = (Graph) graph.clone();
		Iterator i = newGraph.getNodesIterator();
		while (i.hasNext()){
			Node n = (Node) i.next();
			if (!nodeFilter.isAssignableFrom(n.getContents().getClass())){
				i.remove();
			}
		}
		i = newGraph.getEdgesIterator();
		while (i.hasNext()){
			Edge e = (Edge) i.next();
			if (!edgeFilter.isAssignableFrom(e.getContents().getClass())){
				i.remove();
			}
		}
		return newGraph;
	}
}
