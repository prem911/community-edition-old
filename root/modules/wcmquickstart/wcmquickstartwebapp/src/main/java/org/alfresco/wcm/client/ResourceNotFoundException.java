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
package org.alfresco.wcm.client;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class ResourceNotFoundException extends RuntimeException
{
    private static final long serialVersionUID = -30710456153210458L;
    
    Set<String> ids = new TreeSet<String>();
    
    public ResourceNotFoundException(String id)
    {
        ids.add(id);
    }
    
    public ResourceNotFoundException(Collection<String> ids)
    {
        ids.addAll(ids);
    }

    public Set<String> getIds()
    {
        return ids;
    }
}
