/*
 * Copyright 2012 University of Chicago
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * Created on Dec 26, 2006
 */
package org.griphyn.vdl.karajan.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import k.rt.ExecutionException;
import k.rt.Stack;
import k.thr.LWThread;

import org.apache.log4j.Logger;
import org.globus.cog.karajan.analyzer.ArgRef;
import org.globus.cog.karajan.analyzer.CompilationException;
import org.globus.cog.karajan.analyzer.Scope;
import org.globus.cog.karajan.analyzer.Signature;
import org.globus.cog.karajan.analyzer.VarRef;
import org.globus.cog.karajan.analyzer.VariableNotFoundException;
import org.globus.cog.karajan.compiled.nodes.Node;
import org.globus.cog.karajan.futures.FutureFault;
import org.globus.cog.karajan.futures.FutureNotYetAvailable;
import org.globus.cog.karajan.parser.WrapperNode;
import org.griphyn.vdl.karajan.PairSet;
import org.griphyn.vdl.karajan.WaitingThreadsMonitor;
import org.griphyn.vdl.mapping.AbstractDataNode;
import org.griphyn.vdl.mapping.DSHandle;
import org.griphyn.vdl.mapping.InvalidPathException;
import org.griphyn.vdl.mapping.Mapper;
import org.griphyn.vdl.mapping.Path;
import org.griphyn.vdl.type.Type;

public class SetFieldValue extends SwiftFunction {
	public static final Logger logger = Logger.getLogger(SetFieldValue.class);

	protected ArgRef<DSHandle> var;
	protected ArgRef<Object> path;
	protected ArgRef<AbstractDataNode> value;
	
	protected ArgRef<String> _traceline;

	@Override
    protected Signature getSignature() {
        return new Signature(params("var", "value", optional("path", Path.EMPTY_PATH), optional("_traceline", null)));
    }

    private String src, dest;
	private Tracer tracer;
	
	protected VarRef<State> state;
	
	private static class State {
	    public final List<StateEntry> l = new ArrayList<StateEntry>();
	}
	
	private static class StateEntry {
	    private Object value;
	    private Object it;
	    
	    @SuppressWarnings("unchecked")
        public <T> T it() {
	        return (T) it;
	    }
	    
	    public void it(Object it) {
	        this.it= it;
	    }
	    
	    @SuppressWarnings("unchecked")
        public <T> T value() {
            return (T) value;
        }
        
        public void value(Object value) {
            this.value= value;
        }
	}
	
	@Override
    protected void addLocals(Scope scope) {
        super.addLocals(scope);
        state = scope.getVarRef(scope.addVar("#state"));
    }

    @Override
    public Node compile(WrapperNode w, Scope scope)
            throws CompilationException {
        Node fn = super.compile(w, scope);
        if (_traceline.getValue() != null) {
        	setLine(Integer.parseInt(_traceline.getValue()));
        }
        tracer = Tracer.getTracer(this);
        return fn;
    }
    
    

    @Override
    protected void initializeArgs(Stack stack) {
        super.initializeArgs(stack);
        this.state.setValue(stack, null);
    }

    @Override
    public Object function(Stack stack) {
		DSHandle var = this.var.getValue(stack);
		try {
		    Path path = parsePath(this.path.getValue(stack));
			DSHandle leaf = var.getField(path);
			AbstractDataNode value = this.value.getValue(stack);
			
			if (src == null) {
			    dest = Tracer.getVarName(var);
			    src = Tracer.getVarName(value);
			}
			
			if (tracer.isEnabled()) {
			    log(leaf, value, LWThread.currentThread());
			}
			    
            // TODO want to do a type check here, for runtime type checking
            // and pull out the appropriate internal value from value if it
            // is a DSHandle. There is no need (I think? maybe numerical casting?)
            // for type conversion here; but would be useful to have
            // type checking.
			
   			deepCopy(leaf, value, stack);
   			
			return null;
		}
		catch (FutureFault f) {
		    LWThread thr = LWThread.currentThread();
		    if (tracer.isEnabled()) {
		        tracer.trace(thr, var + " waiting for " + Tracer.getFutureName(f.getFuture()));
		    }
		    WaitingThreadsMonitor.addOutput(thr, Collections.singletonList(var));
			throw f;
		}
		catch (Exception e) { // TODO tighten this
			throw new ExecutionException(this, e);
		}
	}

    private void log(DSHandle leaf, DSHandle value, LWThread thr) throws VariableNotFoundException {
        tracer.trace(thr, dest + " = " + Tracer.unwrapHandle(value));
    }

	String unpackHandles(DSHandle handle, Map<Comparable<?>, DSHandle> handles) { 
	    StringBuilder sb = new StringBuilder();
	    sb.append("{");
	    synchronized(handle) {
    	    Iterator<Map.Entry<Comparable<?>, DSHandle>> it = 
    	        handles.entrySet().iterator();
    	    while (it.hasNext()) { 
    	        Map.Entry<Comparable<?>, DSHandle> entry = it.next();
    	        sb.append(entry.getKey());
    	        sb.append('=');
    	        sb.append(entry.getValue().getValue());
    	        if (it.hasNext())
    	            sb.append(", ");
    	    }
	    }
	    sb.append("}");
	    return sb.toString();
	}
	
	protected void deepCopy(DSHandle dest, DSHandle source, Stack stack) {
	    // don't create a state if only a non-composite is copied
	    ((AbstractDataNode) source).waitFor(this);
        if (source.getType().isPrimitive()) {
            dest.setValue(source.getValue());
        }
        else {
    	    State state = this.state.getValue(stack);
            if (state == null) {
                state = new State();
                this.state.setValue(stack, state);
            }
            
            deepCopy(dest, source, state, 0);
            
            this.state.setValue(stack, null);
        }
	}
	
    /** make dest look like source - if its a simple value, copy that
	    and if its an array then recursively copy */
	public void deepCopy(DSHandle dest, DSHandle source, State state, int level) {
	    ((AbstractDataNode) source).waitFor(this);
        if (source.getType().isPrimitive()) {
            dest.setValue(source.getValue());
        }
        else if (source.getType().isArray()) {
		    copyArray(dest, source, state, level);
		}
		else if (source.getType().isComposite()) {
		    copyStructure(dest, source, state, level);
		}
		else {
		    copyNonComposite(dest, source, state, level);
		}
	}

    @Override
    public String getTextualName() {
        if (src == null) {
            return "assignment";
        }
        else {
            return dest + " = " + src;
        }
    }

    private void copyStructure(DSHandle dest, DSHandle source, State state, int level) {
        Type type = dest.getType();
        StateEntry se = getStateEntry(state, level);
        Iterator<String> fni = se.it();
        if (fni == null) {
            fni = type.getFieldNames().iterator();
            se.it = fni;
        }
        String fname = se.value();
        while (fni.hasNext() || fname != null) {
            if (fname == null) {
                fname = fni.next();
                se.value(fname);
            }
            try {
                DSHandle dstf = dest.getField(fname);
                try {
                    DSHandle srcf = source.getField(fname);
                    deepCopy(dstf, srcf, state, level + 1);
                }
                catch (NoSuchFieldException e) {
                    // do nothing. It's an unused field in the source.
                }
            }
            catch (NoSuchFieldException e) {
                throw new ExecutionException("Internal type inconsistency detected. " + 
                    dest + " claims not to have a " + fname + " field");
            }
            se.value(null);
            fname = null;
        }
        popStateEntry(state);
        dest.closeShallow();
    }

    private static StateEntry getStateEntry(State state, int level) {
        if (state.l.size() == level) {
            StateEntry e = new StateEntry();
            state.l.add(e);
            return e;
        }
        else {
            return state.l.get(level);
        }
    }
    
    private static void popStateEntry(State state) {
        state.l.remove(state.l.size() - 1);
    }

    private static void copyNonComposite(DSHandle dest, DSHandle source, State state, int level) {
        Path dpath = dest.getPathFromRoot();
        Mapper dmapper = dest.getRoot().getMapper();
        if (dmapper.canBeRemapped(dpath)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Remapping " + dest + " to " + source);
            }
            dmapper.remap(dpath, source.getMapper(), source.getPathFromRoot());
            dest.setValue(AbstractDataNode.FILE_VALUE);
        }
        else {
            StateEntry se = getStateEntry(state, level);
            FileCopier fc = se.value();
            if (fc != null) {
                if (!fc.isClosed()) {
                    throw new FutureNotYetAvailable(fc);
                }
                else {
                    if (fc.getException() != null) {
                        throw new ExecutionException("Failed to copy " + source + " to " + dest, fc.getException());
                    }
                }
                dest.setValue(AbstractDataNode.FILE_VALUE);
            }
            else {
                fc = new FileCopier(source.getMapper().map(source.getPathFromRoot()), 
                    dest.getMapper().map(dpath));
                se.value(fc);
                try {
                    fc.start();
                }
                catch (Exception e) {
                    throw new ExecutionException("Failed to start file copy", e);
                }
                throw new FutureNotYetAvailable(fc);
            }
            popStateEntry(state);
        }
    }

    private void copyArray(DSHandle dest, DSHandle source, State state, int level) {
        StateEntry se = getStateEntry(state, level);
        Iterator<List<?>> it = se.it();
        if (it == null) {
            it = new PairSet(source.getArrayValue()).iterator();
            se.it(it);
        }
        List<?> pair = se.value();
        while (it.hasNext() || pair != null) {
            if (pair == null) {
                pair = it.next();
                se.value(pair);
            }
            Comparable<?> lhs = (Comparable<?>) pair.get(0);
            DSHandle rhs = (DSHandle) pair.get(1);
            
            DSHandle field;
            try {
                field = dest.getField(lhs);
            }
            catch (NoSuchFieldException ipe) {
                throw new ExecutionException("Could not get destination field",ipe);
            }
            deepCopy(field, rhs, state, level + 1);
            se.value(null);
            pair = null;
        }
        popStateEntry(state);
        dest.closeShallow();
    }
}

