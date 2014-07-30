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
 * Created on Jul 27, 2013
 */
package org.globus.cog.abstraction.interfaces;


/**
 * Something to fill the gap between a URI and URL. URIs cannot seem to 
 * allow spaces in the name and URLs cannot be a path-only.
 * 
 * The accepted syntax is:
 * [protocol://host[:port]/]path
 *
 */
public class RemoteFile {
    private String protocol;
    private String host;
    private String dir, name;
    private int port;
    
    public RemoteFile(String protocol, String host, int port, String dir, String name) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.dir = dir;
        this.name = name;
    }
    
    public RemoteFile(RemoteFile dir, String name) {
        this.protocol = dir.protocol;
        this.host = dir.host;
        this.port = dir.port;
        this.dir = dir.getPath();
        this.name = name;
    }
    
    public RemoteFile(RemoteFile copy) {
        this.protocol = copy.protocol;
        this.host = copy.host;
        this.port = copy.port;
        this.dir = copy.dir;
        this.name = copy.name;
    }
    
    public RemoteFile(String protocol, String host, int port, String path) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.parseDirAndName(path, 0);
    }
    
    public RemoteFile(String protocol, String host, String path) {
        this.protocol = protocol;
        this.host = host;
        this.port = -1;
        this.parseDirAndName(path, 0);
    }
    
    public RemoteFile(String s) {
        port = -1;
        parse(s);
    }

    protected void parse(String str) {
        int pp = 0, sp = 0, state = 0;
        /*
         * state: 
         *  0000 - nothing found yet; next is either protocol or path
         *         scan for:
         *            "://" -> 1000, protocol = str(pp:sp)
         *            EOL   -> path = str
         *  1000 - host[:port] part; scan for:
         *            ":"   -> 2000, host = str(pp:sp)
         *            "/"   -> host = str(pp:sp), path = str(sp:)
         *            EOL   -> error (missing host) 
         *  2000 - port part; scan for:
         *            "/"   -> port = str(pp:sp), path = str(sp + 1:)
         *            EOL   -> error (missing path)
         *  3000 - STOP
         */
        int len = str.length();
        outer:
        while (sp < len) {
            char c = str.charAt(sp);
            if (c > 255) {
                throw new IllegalArgumentException("Illegal character '" + c + "' in path at column " + sp + ": '" + str + "'");
            }
            int ms = state + c;
            switch (ms) {
                case ':':
                    if (match(str, "//", sp + 1)) {
                        state = 1000;
                        protocol = str.substring(pp, sp).intern();
                        pp = sp + 3;
                        sp = pp;
                    }
                    break;
                case 1000 + ':':
                    host = str.substring(pp, sp).intern();
                    pp = sp + 1;
                    state = 2000;
                    break;
                case 1000 + '/':
                    port = -1;
                    host = str.substring(pp, sp).intern();
                    parseDirAndName(str, sp + 1);
                    state = 3000;
                    break outer;
                case 2000 + '/':
                    try {
                        port = Integer.parseInt(str.substring(pp, sp));
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid port: '" + str.substring(pp, sp) + "'");
                    }
                    parseDirAndName(str, sp + 1);
                    state = 3000;
                    break outer;
            }
            
            sp++;
        }
        // EOL
        switch (state) {
            case 0000:
                protocol = null;
                host = null;
                parseDirAndName(str, 0);
                port = -1;
                break;
            case 1000:
                throw new IllegalArgumentException("Host not found while scanning '" + str + "'");
            case 2000:
                throw new IllegalArgumentException("Path not found while scanning '" + str + "'");
            default:
                // STOP
        }
    }
    
    private void parseDirAndName(String str, int pos) {
        int lastSep = str.lastIndexOf('/');
        if (lastSep < pos) {
            dir = null;
            name = str.substring(pos);
        }
        else if (lastSep == pos) {
            dir = "/";
            name = str.substring(pos + 1);
        }
        else {
            dir = normalize(str, pos, lastSep).intern();
            name = str.substring(lastSep + 1);
        }
    }

    private boolean match(String str, String sub, int pos) {
        if (sub.length() + pos > str.length()) {
            return false;
        }
        for (int i = 0; i < sub.length(); i++) {
            if (str.charAt(pos + i) != sub.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    private String normalize(String path, int start, int end) {
        // there is a slight performance penalty here, but it makes things
        // cleaner
        StringBuilder sb = new StringBuilder();
        while (start < end) {
            char c = path.charAt(start);
            if (c == '/') {
                if (start + 2 < end && match(path, "./", start + 1)) {
                    start += 2;
                }
                else if (start + 2 == end && path.charAt(start + 1) == '.') {
                    break;
                }
                else if (start + 1 < end && path.charAt(start + 1) == '/') {
                    start += 1;
                }
            }
            sb.append(c);
            
            start++;
        }
        return sb.toString();
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectory() {
        return dir;
    }
    
    protected void setDirectory(String dir) {
        this.dir = dir;
    }

    public String getProtocol() {
        return protocol;
    }
    
    protected void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }
    
    protected void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }

    public String getPath() {
        if (dir == null) {
            return name;
        }
        else {
            return dir + '/' + name;
        }
    }
    
    public boolean isAbsolute() {
        return dir != null && dir.startsWith("/");
    }
        
    public String getURIAsString() {
        StringBuilder sb = new StringBuilder();
        if (protocol != null) {
            sb.append(protocol);
            sb.append("://");
        }
        if (host != null) {
            sb.append(host);
            if (port != -1) {
                sb.append(':');
                sb.append(port);
            }
            sb.append('/');
        }
        if (dir != null) {
            // special case when the dir is just a slash
            // (indicating a file in the root dir): there
            // is no explicit separator
            if (dir.equals("/")) {
                sb.append(dir);
            }
            else {
                sb.append(dir);
                sb.append('/');
            }
            sb.append(name);
        }
        else {
            sb.append(name);
        }
        return sb.toString();
    }
    
    public String toString() {
        return getURIAsString();
    }
    
    public String toDebugString() {
        return "RemoteFile[protocol: '" + protocol + "', host: '" + host + 
            "', port: " + port + ", dir: " + (dir == null ? "null" : "'" + dir + "'") + ", name: '" + name + "']"; 
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteFile) {
            RemoteFile a = (RemoteFile) obj;
            return name.equals(a.name) && (dir == null ? a.dir == null : dir.equals(a.dir)) && 
                host.equals(a.host) && port == a.port && protocol.equals(a.protocol);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return protocol.hashCode() + host.hashCode() + port + (dir == null ? 0 : dir.hashCode()) + name.hashCode();
    }
    
    public static void main(String[] args) {
        test("http://ll.com/name", "http://ll.com/name", false);
        test("http://ll.com:30/name", "http://ll.com:30/name", false);
        test("http://ll.com:/name", "http://ll.com:/name", true);
        test("http://www.example.com/dir/name", "http://www.example.com/dir/name", false);
        test("http://ll.com//name", "http://ll.com//name", false);
        test("http://ll.com/dir/./name", "http://ll.com/dir/name", false);
        test("http://ll.com/dir1/./dir2/name", "http://ll.com/dir1/dir2/name", false);
    }

    private static void test(String s, String e, boolean err) {
        RemoteFile rf;
        if (err) {
            try {
                rf = new RemoteFile(s);
                System.err.println("Missing error: " + s + " -> " + rf.toDebugString());
            }
            catch (Exception ex) {
                System.out.println(s);
                System.out.println("\tError: " + ex.getMessage());
                return;
            }
        }
        else {
            try {
                rf = new RemoteFile(s);
            }
            catch (Exception ex) {
                System.err.println(s);
                System.err.println("\tError");
                return;
            }
        }
        if (!rf.toString().equals(e)) {
            System.out.println("Error: " + e + " -> " + rf.toString() + " -> " + rf.toDebugString());
        }
        else {
            System.out.println(s);
            System.out.println("\t" + rf.toDebugString());
        }
    }
}
