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


package org.griphyn.vdl.mapping.file;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.griphyn.vdl.mapping.AbsFile;
import org.griphyn.vdl.mapping.AbstractMapper;
import org.griphyn.vdl.mapping.DSHandle;
import org.griphyn.vdl.mapping.GeneralizedFileFormat;
import org.griphyn.vdl.mapping.HandleOpenException;
import org.griphyn.vdl.mapping.InvalidMappingParameterException;
import org.griphyn.vdl.mapping.Mapper;
import org.griphyn.vdl.mapping.MappingParam;
import org.griphyn.vdl.mapping.MappingParamSet;
import org.griphyn.vdl.mapping.Path;
import org.griphyn.vdl.mapping.PhysicalFormat;
import org.griphyn.vdl.type.Types;

public class CSVMapper extends AbstractMapper {
	public static final MappingParam PARAM_FILE = new MappingParam("file");

	/** whether the file has a line describing header info. default is true. */
	public static final MappingParam PARAM_HEADER = new MappingParam("header", "true");

	/** the number of lines to skip at the start of the file. default is 0. */
	public static final MappingParam PARAM_SKIP = new MappingParam("skip", "0");

	/** delimiter between header fields. defaults to the value of the
	"delim" field. */
	public static final MappingParam PARAM_HDELIMITER = new MappingParam("hdelim");

	/** delimiters between content fields. default is space, tab, comma */
	public static final MappingParam PARAM_DELIMITER = new MappingParam("delim", " \t,");
	
	
	@Override
    protected void getValidMappingParams(Set<String> s) {
	    addParams(s, PARAM_FILE, PARAM_HEADER, PARAM_SKIP, PARAM_HDELIMITER, PARAM_DELIMITER);
	    super.getValidMappingParams(s);
    }

	/** list of column names */
	private List cols = new ArrayList();

	/** column name to index map */
	private Map colindex = new HashMap();

	/** the content of the CSV file */
	private List content = new ArrayList();

	/** whether the CSV file has been read already. */
	private boolean read = false;
	
	private String delim, hdelim;
	private boolean header;
	private int skip;

	public void setParams(MappingParamSet params) throws HandleOpenException {
		super.setParams(params);
		if (!PARAM_FILE.isPresent(this)) {
			throw new InvalidMappingParameterException("CSV mapper must have a file parameter.");
		}
		if (!PARAM_HDELIMITER.isPresent(this)) {
		    Object raw = PARAM_DELIMITER.getRawValue(this);
		    if (raw != null) {
		        params.set(PARAM_HDELIMITER, PARAM_DELIMITER.getRawValue(this));
		    }
		    else {
		        params.set(PARAM_HDELIMITER, PARAM_DELIMITER.getValue(this));
		    }
		}
		delim = PARAM_DELIMITER.getStringValue(this);
        hdelim = PARAM_HDELIMITER.getStringValue(this);
        header = PARAM_HEADER.getBooleanValue(this);
        skip = PARAM_SKIP.getIntValue(this);
	}

	private synchronized void readFile() {
		if (read) {
			return;
		}
		
		String file = getCSVFile(); 
		
		try {
			BufferedReader br = 
			    new BufferedReader(new FileReader(file));

			String line;
			StringTokenizer st;

			if (header) {
				line = br.readLine();
				if (line == null) {
				    throw new RuntimeException("Invalid CSV file (" + file + "): missing header.");
				}
				st = new StringTokenizer(line, hdelim);
				int ix = 0;
				while (st.hasMoreTokens()) {
					String column = st.nextToken();
					column.replaceAll("\\s", "_");
					cols.add(column);
					colindex.put(column, new Integer(ix));
					++ix;
				}
			}
			while (skip > 0) {
				br.readLine();
				--skip;
			}

			int i = 0;
			line = br.readLine();
			if (line != null && !header) {
				st = new StringTokenizer(line, delim);
				int j = 0;
				while (j < st.countTokens()) {
					String colname = "column" + j;
					cols.add(colname);
					colindex.put(colname, new Integer(j));
					j++;
				}
			}

			while (line != null) {
				st = new StringTokenizer(line, delim);
				List colContent = new ArrayList();
				while (st.hasMoreTokens()) {
					String tok = st.nextToken();
					colContent.add(tok);
				}
				line = br.readLine();
				++i;
				content.add(colContent);
			}
			read = true;
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private String getCSVFile() {
	    String result = null;
	    Object object = PARAM_FILE.getRawValue(this);
        DSHandle handle = (DSHandle) object;
        GeneralizedFileFormat fileFormat;
        if (handle.getType().equals(Types.STRING)) {
            String path = (String) handle.getValue();
            fileFormat = new AbsFile(path);
        }
        else {
            Mapper mapper = handle.getMapper();
            PhysicalFormat format = mapper.map(Path.EMPTY_PATH);
            fileFormat = (GeneralizedFileFormat) format;
        }
        result  = fileFormat.getPath();
        return result;
    }

    public Collection existing() {
		readFile();
		List l = new ArrayList();
		Iterator itl = content.iterator();
		int ii = 0;
		while (itl.hasNext()) {
			Path path = Path.EMPTY_PATH;
			path = path.addFirst(ii, true);
			List colContent = (List) itl.next();
			Iterator itc = colContent.iterator();
			int j = 0;
			while (itc.hasNext()) {
				Path p = path.addLast((String)cols.get(j));
				l.add(p);
				itc.next();
				j++;
			}
			ii++;
		}
		return l;
	}

	public boolean isStatic() {
		return false;
	}

	public PhysicalFormat map(Path path) {
		if (path == null || path == Path.EMPTY_PATH) {
			return null;
		}

		readFile();

		Iterator<Path.Entry> pi = path.iterator();
		Path.Entry pe = pi.next();
		if (!pe.isIndex()) {
			return null;
		}
		int i = 0;
		if (pe.getKey() instanceof Integer) {
		    i = ((Integer) pe.getKey()).intValue();
		}
		else {
			return null;
		}
		if (i > content.size()) {
			return null;
		}
		List cl = (List) content.get(i);
		if (cl == null) {
			return null;
		}

		if (!pi.hasNext()) {
			return new AbsFile((String) cl.get(0));
		}

		pe = pi.next();
		String col = String.valueOf(pe.getKey());
		if (!colindex.containsKey(col)) {
			return null;
		}
		int ci = ((Integer) colindex.get(col)).intValue();
		return new AbsFile((String) cl.get(ci));
	}
}
