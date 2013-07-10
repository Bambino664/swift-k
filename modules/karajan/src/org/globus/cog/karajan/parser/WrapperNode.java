// ----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Apr 16, 2005
 */
package org.globus.cog.karajan.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.globus.cog.karajan.analyzer.CompilationException;
import org.globus.cog.karajan.analyzer.Scope;
import org.globus.cog.karajan.compiled.nodes.Node;
import org.globus.cog.karajan.util.AdaptiveArrayList;
import org.globus.cog.karajan.util.AdaptiveMap;

public final class WrapperNode {
	public static final String UID = "_uid";
	public static final String LINE = "_line";
	public static final String FILENAME = "_filename";
	public static final String ANNOTATION = "_annotation";

	public static final String TEXT = "_text_";

	
	private WrapperNode parent;
	private List<WrapperNode> nodes;
	private List<WrapperNode> blocks;
	private Map<String, Object> properties;
	private Map<String, Object> staticArguments;
	private String type;
	private boolean compiled;

	private static AdaptiveMap.Context pc = new AdaptiveMap.Context(),
			sac = new AdaptiveMap.Context();
	private static AdaptiveArrayList.Context ec = new AdaptiveArrayList.Context();

	public WrapperNode() {
		properties = new AdaptiveMap<String, Object>(pc);
		staticArguments = new AdaptiveMap<String, Object>(sac);
		nodes = Collections.emptyList();
	}

	public void addNode(WrapperNode node) {
		if (nodes.isEmpty()) {
			nodes = new AdaptiveArrayList<WrapperNode>(ec);
		}
		node.setParent(this);
		nodes.add(node);
	}
	
	public void removeNode(WrapperNode node) {
		nodes.remove(node);
	}
	
	public WrapperNode removeNode(int index) {
		return nodes.remove(index);
	}
	
	public void setNodes(List<WrapperNode> l) {
		nodes = l;
		for (WrapperNode n : l) {
			n.setParent(this);
		}
	}

	public WrapperNode getNode(int index) {
		return nodes.get(index);
	}

	public int nodeCount() {
		return nodes.size();
	}

	public List<WrapperNode> nodes() {
		return nodes;
	}

	public void setNodeType(String type) {
		this.type = type;
	}

	public String getNodeType() {
		return type;
	}

	public void setProperty(String name, Object value) {
		properties.put(name.toLowerCase(), value);
	}

	public void removeProperty(String name) {
		properties.remove(name.toLowerCase());
	}

	public void setProperty(final String name, final int value) {
		setProperty(name, new Integer(value));
	}

	public Object getProperty(final String name) {
		return properties.get(name.toLowerCase());
	}

	public synchronized boolean hasProperty(final String name) {
		return properties.containsKey(name.toLowerCase());
	}

	public Collection<String> propertyNames() {
		return properties.keySet();
	}

	public void setParent(WrapperNode parent) {
		this.parent = parent;
	}

	public WrapperNode getParent() {
		return parent;
	}

	public boolean acceptsInlineText() {
		return true;
	}
		
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getNodeType());
		Object fileName = getTreeProperty(FILENAME, this);
		if (fileName instanceof String) {
			String fn = (String) fileName;
			fn = fn.substring(1 + fn.lastIndexOf('/'));
			sb.append(" @ ");
			sb.append(fn);

			if (hasProperty(LINE)) {
				sb.append(", line: ");
				sb.append(getProperty(LINE));
			}
		}
		return sb.toString();
	}
	
	public static Object getTreeProperty(final String name, final WrapperNode element) {
		if (element == null) {
			return null;
		}
		if (element.hasProperty(name)) {
			return element.getProperty(name);
		}
		else {
			if (element.getParent() != null) {
				return getTreeProperty(name, element.getParent());
			}
			else {
				return null;
			}
		}
	}

	public Node compile(Node parent, Scope scope) throws CompilationException {
		try {
			if (compiled) {
				throw new CompilationException(this ,"Already compiled");
			}
			compiled = true;
			Node self = scope.resolve(this.getNodeType());
			self.setParent(parent);
			self = self.compile(this, scope);
			return self;
		}
		catch (RuntimeException e) {
			throw new CompilationException(this, e.getMessage(), e);
		}
	}
	
	public String getText() {
		return (String) getProperty(TEXT);
	}
}
