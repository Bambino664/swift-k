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
 * Created on Jul 18, 2010
 */
package org.griphyn.vdl.karajan.lib;

import org.apache.log4j.Logger;
import org.globus.cog.karajan.arguments.Arg;
import org.globus.cog.karajan.stack.VariableStack;
import org.globus.cog.karajan.workflow.ExecutionException;
import org.globus.cog.karajan.workflow.nodes.AbstractSequentialWithArguments;
import org.griphyn.vdl.karajan.functions.ConfigProperty;

public class Parameterlog extends AbstractSequentialWithArguments {
    public static final Logger logger = Logger.getLogger(Parameterlog.class);

    public static final Arg DIRECTION = new Arg.Positional("direction");
    public static final Arg VAR = new Arg.Positional("variable");
    public static final Arg ID = new Arg.Positional("id");
    public static final Arg THREAD = new Arg.Positional("thread");

    static {
        setArguments(Parameterlog.class, new Arg[] { DIRECTION, VAR, ID, THREAD });
    }

    private Boolean enabled;
    
    @Override
    public void pre(VariableStack stack) throws ExecutionException {
        if (enabled == null) {
            enabled = "true".equals(ConfigProperty.getProperty("provenance.log", true, stack));
        }
    }
    
    

    @Override
    protected void executeChildren(VariableStack stack) throws ExecutionException {
    	if (enabled) {
    	    super.executeChildren(stack);
    	}
    	else {
    		complete(stack);
    	}
    }

    @Override
    protected void post(VariableStack stack) throws ExecutionException {
        if (enabled) {
            logger.info("PARAM thread=" + THREAD.getValue(stack) + " direction="
                    + DIRECTION.getValue(stack) + " variable=" + VAR.getValue(stack)
                    + " provenanceid=" + ID.getValue(stack));
        }
    }
}
