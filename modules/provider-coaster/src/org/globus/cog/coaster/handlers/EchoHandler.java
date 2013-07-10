//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jul 21, 2005
 */
package org.globus.cog.coaster.handlers;

import org.globus.cog.coaster.ProtocolException;


public class EchoHandler extends RequestHandler {
	
	
	public void requestComplete() throws ProtocolException {
		sendReply(getInData(0));
	}
}
