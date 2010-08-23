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
package org.alfresco.solr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;

/**
 * @author Andy
 *
 */
public class SolrDocumentLoader
{

    public static int count = 0;
    
    public static void main(String[] args) throws SolrServerException, IOException
    {
        int FOLDERS = 10000;
        int LEAVES = 100;
        
        SolrServer solr = getRemoteServer();
     
        solr.deleteByQuery("*:*");
        solr.commit(true, true);
        solr.optimize(true, true);
        
        SolrInputDocument root = createRootDocument();
        solr.add(root);
        
        SolrInputDocument rootPath = createRootPathDocument();
        solr.add(rootPath);
        
        solr.commit(true, true);
        solr.optimize(true, true);
        
        SolrQuery query = new SolrQuery("*:*");
        QueryResponse response = solr.query(query);
        
        assert(2 == response.getResults().size());
     
        long start = System.nanoTime();
        
        for(int i = 0; i < FOLDERS; i++)
        {
            Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
            docs.add(createContainerDocument(i));
            for(int j = 0; j < LEAVES; j++)
            {
                docs.add(createLeafDocument(i, j));
            }
            solr.add(docs);
        }
        
        solr.commit(true, true);
        
        solr.optimize(true, true);
        
        long end = System.nanoTime();
        
        query = new SolrQuery("*:*");
        response = solr.query(query);
        
        System.out.println("Time "+ ((end-start)/1000000000.0));
        
        assert((FOLDERS*(LEAVES+1) + 2) == response.getResults().size());
        
        System.out.println("Done "+ (FOLDERS*(LEAVES+1) + 2));
    }
    
    public static int getDbId()
    {
        return ++count;
    }
    
    public static SolrInputDocument createLeafDocument(int i, int j)
    {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("ISCATEGORY","F");
        doc.addField("PARENT", "ID-F-"+i); 
        doc.addField("QNAME","{http://www.alfresco.org/model/content/1.0}leaf-"+j);
        doc.addField("PRIMARYPARENT","ID-F-"+i);
        doc.addField("ASSOCTYPEQNAME","{http://www.alfresco.org/model/content/1.0}contains");
        doc.addField("PRIMARYASSOCTYPEQNAME","{http://www.alfresco.org/model/content/1.0}contains");
        doc.addField("ANCESTOR","ID-F-"+i);
        doc.addField("FTSSTATUS", "Clean"); 
        doc.addField("ID", "ID-L-"+i+"-"+j); 
        doc.addField("TX", "TX-1"); 
        doc.addField("ISROOT", "F"); 
        doc.addField("ISNODE", "T"); 
        doc.addField("DBID", getDbId());
        doc.addField("TYPE", "{http://www.alfresco.org/model/content/1.0}folder");
        doc.addField("ASPECT", "{http://www.alfresco.org/model/content/1.0}auditable");
        doc.addField("@{http://www.alfresco.org/model/system/1.0}locale", "en");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}title", "Leaf "+i+" "+j);
        doc.addField("@{http://www.alfresco.org/model/content/1.0}name", "Folder "+i+" "+j);
        doc.addField("@{http://www.alfresco.org/model/content/1.0}created", "2010-07-21T10:52:00.000Z");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}creator", "Andy");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}modified", "2010-07-21T10:52:00.000Z");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}modifier", "Andy");
        doc.addField("PATH", "/cm:folder_"+i+"/cm:leaf_"+j);
        return doc;
    }
    
    public static SolrInputDocument createContainerDocument(int i)
    {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("ISCATEGORY","F");
        doc.addField("PARENT","Root");
        doc.addField("QNAME","{http://www.alfresco.org/model/content/1.0}folder-"+i);
        doc.addField("PRIMARYPARENT","Root");
        doc.addField("ASSOCTYPEQNAME","{http://www.alfresco.org/model/system/1.0}children");
        doc.addField("PRIMARYASSOCTYPEQNAME","{http://www.alfresco.org/model/system/1.0}children");
        doc.addField("ANCESTOR","Root");
        doc.addField("FTSSTATUS", "Clean"); 
        doc.addField("ID", "ID-F-"+i); 
        doc.addField("TX", "TX-1"); 
        doc.addField("ISROOT", "T"); 
        doc.addField("ISNODE", "T"); 
        doc.addField("DBID", "0");
        doc.addField("TYPE", "{http://www.alfresco.org/model/content/1.0}folder");
        doc.addField("ASPECT", "{http://www.alfresco.org/model/content/1.0}auditable");
        doc.addField("@{http://www.alfresco.org/model/system/1.0}locale", "en");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}title", "Folder "+i);
        doc.addField("@{http://www.alfresco.org/model/content/1.0}name", "Folder "+i);
        doc.addField("@{http://www.alfresco.org/model/content/1.0}created", "2010-07-21T10:52:00.000Z");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}creator", "Andy");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}modified", "2010-07-21T10:52:00.000Z");
        doc.addField("@{http://www.alfresco.org/model/content/1.0}modifier", "Andy");
        doc.addField("PATH", "/cm:folder_"+i);
        return doc;
    }
    
    public static SolrInputDocument createRootDocument()
    {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("ISCATEGORY","F");
        doc.addField("FTSSTATUS", "Clean"); 
        doc.addField("ID", "Root"); 
        doc.addField("TX", "TX-1"); 
        doc.addField("ISROOT", "T"); 
        doc.addField("ISNODE", "T"); 
        doc.addField("DBID", "0");
        doc.addField("PATH","/");
        return doc;
    }
    
    public static SolrInputDocument createRootPathDocument()
    {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("PATH","/");
        doc.addField("ID", "Root-Path"); 
        doc.addField("TX", "TX-1"); 
        doc.addField("DBID", "0"); 
        return doc;
    }
    
    public static SolrServer getRemoteServer() throws MalformedURLException
    {
        CommonsHttpSolrServer solr = new CommonsHttpSolrServer("http://localhost:8080/solr/test");
        solr.setRequestWriter(new BinaryRequestWriter());
        return solr;
    }
}
