/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.module.org_alfresco_module_wcmquickstart.util.contextparser;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Context parser service.
 * 
 * @author Roy Wetherall
 */
public class ContextParserService
{
	/** Pattern matcher */
	private static final Pattern MATCH_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}"); 
	
	/** Context parser map */
	private Map<String, ContextParser> contextParsers = new TreeMap<String, ContextParser>();
	
	/**
	 * Registers a new context parser
	 * @param queryParser query parser
	 */
	public void register(ContextParser queryParser)
	{
		contextParsers.put(queryParser.getName(), queryParser);
	}
	
	/**
	 * 
	 * @param context
	 * @param value
	 * @return
	 */
	public String parse(NodeRef context, String value)
	{		
		String result = value;	
		
		// Get a Matcher based on the target string. 
		Matcher matcher = MATCH_PATTERN.matcher(value); 

		// Find all the matches. 
		while (matcher.find() == true) 
		{ 
			String contextParserName = matcher.group(1);
			ContextParser qp = contextParsers.get(contextParserName.trim());
			if (qp != null)
			{
				String temp = qp.execute(context);
				if (temp != null)
				{
					result = result.replace(matcher.group(), temp);
				}
			}
		}	
		
		return result;
	}
}
