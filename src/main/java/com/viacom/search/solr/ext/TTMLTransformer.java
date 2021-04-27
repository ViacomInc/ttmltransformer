/**
 * Copyright 2015 Viacom, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/ 
package com.viacom.search.solr.ext;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * DataImportHandler Transformer to extract closed-caption data from TTML files.
 * Two modes of indexing -- "text" and "payloads".
 * 
 * Text mode will extract only the text of captions. Payloads mode will attach
 * the caption's "begin" attribute to each token. Payloaded captions should be
 * stored in a field using the solr.DelimitedPayloadTokenFilterFactory.
 * 
 * 
 */

public class TTMLTransformer extends Transformer {

	public static final String TTML_CMD = "TTML";
	public static final String DELIM_CMD = "delimiter";
	
	
	public Object transformRow(Map<String, Object> row, Context context) {
		
		String delimiter = "|";	

		for (Map<String, String> fld : context.getAllEntityFields()) {

			//if TTML isn't set for the field, move along.
			String ttmlCmd = fld.get(TTML_CMD);
			if(ttmlCmd == null)
				continue;
			
			//payload or plain text?
			boolean payloads = false;
			if("payloads".equals(ttmlCmd)){
				payloads = true;
			}
			
			//payloads need a delimiter. "|" is a fine choice, but who am I to judge?
			String delimCmd = fld.get(DELIM_CMD);
			if(delimCmd != null)
				delimiter = delimCmd;
			
			//get the field(s) we're dealing with.
			String column = fld.get(DataImporter.COLUMN);
			String srcColumn = fld.get(RegexTransformer.SRC_COL_NAME);

			//overwrite existing column or create new field via sourceColName
			if (srcColumn == null)
				srcColumn = column;

			
			//with single-valued fields, val will be a string, and a list for multi-valued fields.
			List<String> inputs = new ArrayList<>();
			Object val = row.get(srcColumn);
			if(val != null){
				if(val instanceof List){
					inputs = (List<String>) val;
                                } else {
					inputs.add(String.valueOf(val));
                                }
                        
				List<Object> results = new ArrayList<>();
				for (String input : inputs) {
					results.add(getCaptions(input, payloads, delimiter));
				}
				row.put(column, results);
			}
		}
		return row;
	}

	private String getCaptions(String input, boolean payloads, String delimiter) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		}

		Document doc = null;
                if( builder!=null ){
			try {
				doc = builder.parse(new URL(input).openStream());
			} catch (SAXException se) {
				se.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
                }

		StringBuilder sb = new StringBuilder();
		if( doc!=null ){
			NodeList ps = doc.getElementsByTagName("p");
			for (int i = 0; i < ps.getLength(); i++) {
				Node p = ps.item(i);
				String begin = ((Element)p).getAttributeNode("begin").getValue();
				String txt = getNodeText(p);
				if(payloads)
					txt = formatPayload(txt, begin, delimiter);
				sb.append(txt);
			}
                }
		return sb.toString();
	}
	
	private String getNodeText( Node node ) {
        StringBuilder results = new StringBuilder();
        if (node.getNodeType() == Node.TEXT_NODE) {
        	results.append(node.getTextContent());
        	if(results.lastIndexOf(" ")!=results.length()-1)
        		results.append( " " );
        }
        
        if ( node.getChildNodes().getLength() > 0 ) {
        	for ( int i = 0; i < node.getChildNodes().getLength(); i++ )
        		results.append(getNodeText(node.getChildNodes().item(i)));
        }	
        return results.toString();
    }
	
	private String formatPayload(String txt, String begin, String delim){
		StringBuilder sb = new StringBuilder();
		String[] words = txt.split(" ");
        for (String word: words) {
			sb.append(word);
			sb.append(delim);
			sb.append(begin);
			sb.append(" ");
		}
		return sb.toString();
	}

}
