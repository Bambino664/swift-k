/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 * Code from Java CoG Kit Project (see notice below) with modifications.
 *
 * Copyright 2005-2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Mar 21, 2005
 */
package org.globus.cog.karajan.compiled.nodes.user;

import org.apache.log4j.Logger;
import org.globus.cog.karajan.analyzer.NamedValue;
import org.globus.cog.karajan.analyzer.Scope;
import org.globus.cog.karajan.analyzer.Signature;
import org.globus.cog.karajan.analyzer.Var;

/**
 * Call-by-future function
 */
public class CBFFunction extends Function {
	public static final Logger logger = Logger.getLogger(CBFFunction.class);

	@Override
	protected void _return(Scope scope, Signature sig) {
		Var.Channel ret = scope.parent.lookupChannel("...");
		
		ret.append(new NamedValue(null, null, new Scope.CBFUserDef(this)));
	}
}