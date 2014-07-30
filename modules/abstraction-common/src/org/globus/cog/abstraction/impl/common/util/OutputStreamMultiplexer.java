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

//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Aug 20, 2007
 */
package org.globus.cog.abstraction.impl.common.util;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamMultiplexer extends OutputStream {
    private OutputStream os1, os2;
    
    public OutputStreamMultiplexer(OutputStream os1, OutputStream os2) {
        this.os1 = os1;
        this.os2 = os2;
    }
    
    public void write(int b) throws IOException {
        os1.write(b);
        os2.write(b);
    }
    
    public static OutputStream multiplex(OutputStream os1, OutputStream os2) {
        if (os1 == null) {
            return os2;
        }
        if (os2 == null) {
            return os1;
        }
        return new OutputStreamMultiplexer(os1, os2);
    }
}
