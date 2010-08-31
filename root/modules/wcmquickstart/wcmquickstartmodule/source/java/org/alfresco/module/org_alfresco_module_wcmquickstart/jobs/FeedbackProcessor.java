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
package org.alfresco.module.org_alfresco_module_wcmquickstart.jobs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_wcmquickstart.model.WebSiteModel;
import org.alfresco.module.org_alfresco_module_wcmquickstart.util.SiteHelper;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is designed to be run periodically. It finds any visitor feedback
 * that has not been processed yet, and incorporates it into the summary
 * information for each asset (total comment count and average rating).
 * 
 * @author Brian
 * 
 */
public class FeedbackProcessor
{
    private static final String FEEDBACK_SUMMARIES_CONTAINER_NAME = "feedbackSummaries";

    private static final Log log = LogFactory.getLog(FeedbackProcessor.class);

    private RetryingTransactionHelper txHelper;
    private SearchService searchService;
    private NodeService nodeService;
    private SiteHelper siteHelper;

    public void run()
    {
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            @Override
            public Object doWork() throws Exception
            {
                txHelper.doInTransaction(new RetryingTransactionCallback<Object>()
                {
                    @Override
                    public Object execute() throws Throwable
                    {
                        HashMap<NodeRef,SummaryInfo> nodeSummaryMap = new HashMap<NodeRef, SummaryInfo>(89);

                        //Find all visitor feedback nodes that have not yet been processed
                        ResultSet rs = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 
                                SearchService.LANGUAGE_LUCENE, "@ws\\:ratingProcessed:\"false\"");
                     
                        if (log.isDebugEnabled())
                        {
                            log.debug("Running feedback processor across " + rs.length() + " feedback nodes");
                        }
                        for (ResultSetRow row : rs)
                        {
                            //Get the asset to which this feedback relates
                            NodeRef relatedAsset = (NodeRef)row.getValue(WebSiteModel.PROP_RELEVANT_ASSET); 
                            if (relatedAsset != null)
                            {
                                //and check whether it has a feedback summary node associated with it
                                SummaryInfo info = nodeSummaryMap.get(relatedAsset);
                                if (info == null &&
                                	nodeService.exists(relatedAsset) == true)
                                {
                                    //We haven't come across this asset previously in this run, so we need to look for a summary node for it
                                    List<AssociationRef> assocs = nodeService.getSourceAssocs(relatedAsset, WebSiteModel.ASSOC_SUMMARISED_ASSET);
                                    NodeRef summaryNode = null;
                                    if (assocs.isEmpty())
                                    {
                                        //There is no summary node currently. Create one.
                                        //If the asset is in a Share site (which it probably is) then place the summary node in a
                                        //specific container named "feedbackSummaries"...
                                        NodeRef summaryParent = siteHelper.getWebSiteContainer(relatedAsset, FEEDBACK_SUMMARIES_CONTAINER_NAME);
                                        
                                        String name = "FeedbackSummary_" + relatedAsset.getId();
                                        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
                                        props.put(ContentModel.PROP_NAME, name);
                                        props.put(WebSiteModel.PROP_AVERAGE_RATING, 0.0);
                                        props.put(WebSiteModel.PROP_PROCESSED_RATINGS, 0);
                                        props.put(WebSiteModel.PROP_COMMENT_COUNT, 0);
                                        props.put(WebSiteModel.PROP_SUMMARISED_ASSET, relatedAsset);
                                        summaryNode = nodeService.createNode(summaryParent, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name), 
                                                WebSiteModel.TYPE_VISITOR_FEEDBACK_SUMMARY, props).getChildRef();
                                        nodeService.createAssociation(summaryNode, relatedAsset, WebSiteModel.ASSOC_SUMMARISED_ASSET);
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("Created a new feedback summary node for asset " + relatedAsset);
                                        }
                                    }
                                    else
                                    {
                                        //There is an existing summary node to use
                                        summaryNode = assocs.get(0).getSourceRef();
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("Found an existing feedback summary node for asset " + relatedAsset);
                                        }
                                    }
                                    //Create and record a SummaryInfo object in which to gather data for this asset
                                    info = new SummaryInfo(summaryNode);
                                    nodeSummaryMap.put(relatedAsset, info);
                                }
                                if (row.getValue(WebSiteModel.PROP_COMMENT) != null)
                                {
                                    info.commentCount++;
                                }
                                Integer rating = (Integer)row.getValue(WebSiteModel.PROP_RATING);
                                if (rating != null)
                                {
                                    info.totalRating += rating;
                                    info.ratingCount++;
                                }
                            }
                            else
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("Skipping a piece of feedback that is related to no asset: " + row.getNodeRef());
                                }
                            }
                            //Set the "ratingProcessed" flag to true on this feedback node so we don't process it again
                            nodeService.setProperty(row.getNodeRef(), WebSiteModel.PROP_RATING_PROCESSED, Boolean.TRUE);
                        }
                        
                        //Now we can work through the records that we've recorded in memory and update the necessary summary nodes
                        for (Map.Entry<NodeRef, SummaryInfo> entry : nodeSummaryMap.entrySet())
                        {
                            SummaryInfo summaryInfo = entry.getValue();
                            NodeRef summaryNode = summaryInfo.summaryNode;
                            
                            Map<QName,Serializable> props = nodeService.getProperties(summaryNode);
                            
                            //Get the current values from the summary node
                            Integer commentCountObj = (Integer)nodeService.getProperty(summaryNode, WebSiteModel.PROP_COMMENT_COUNT);
                            Integer processedRatingsObj = (Integer)nodeService.getProperty(summaryNode, WebSiteModel.PROP_PROCESSED_RATINGS);
                            Float averageRatingObj = (Float)nodeService.getProperty(summaryNode, WebSiteModel.PROP_AVERAGE_RATING);
                            
                            int commentCount = commentCountObj == null ? 0 : commentCountObj.intValue();
                            int processedRatings = processedRatingsObj == null ? 0 : processedRatingsObj.intValue();
                            float averageRating = averageRatingObj == null ? 0 : averageRatingObj.floatValue();
                            
                            if (log.isDebugEnabled())
                            {
                                log.debug("About to update feedback summary for asset " + entry.getKey() + ". Current values are: " +
                                        "commentCount = " + commentCount + "; processedRatings = " + processedRatings + "; averageRating = " + averageRating);
                            }
                            
                            //Update the values with the information gathered in the SummaryInfo object...
                            commentCount += summaryInfo.commentCount;
                            float totalRatingSoFar = averageRating * processedRatings;
                            if (summaryInfo.ratingCount > 0)
                            {
                                processedRatings += summaryInfo.ratingCount;
                                totalRatingSoFar += summaryInfo.totalRating;
                                averageRating = totalRatingSoFar / processedRatings;
                            }
                            if (log.isDebugEnabled())
                            {
                                log.debug("About to update feedback summary for asset " + entry.getKey() + ". New values are: " +
                                        "commentCount = " + commentCount + "; processedRatings = " + processedRatings + "; averageRating = " + averageRating);
                            }
                            props.put(WebSiteModel.PROP_COMMENT_COUNT, commentCount);
                            props.put(WebSiteModel.PROP_PROCESSED_RATINGS, processedRatings);
                            props.put(WebSiteModel.PROP_AVERAGE_RATING, averageRating);
                            // ... and write the new values back to the repo.
                            nodeService.setProperties(summaryNode, props);
                        }
                        return null;
                    }   
                });
                return null;
            }
        }, AuthenticationUtil.SYSTEM_USER_NAME);
    }
    

    private static class SummaryInfo
    {
        public int commentCount = 0;
        public int totalRating = 0;
        public int ratingCount = 0;
        public final NodeRef summaryNode;
        
        public SummaryInfo(NodeRef summaryNode)
        {
            this.summaryNode = summaryNode;
        }
    }

    public void setTxHelper(RetryingTransactionHelper txHelper)
    {
        this.txHelper = txHelper;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setSiteHelper(SiteHelper siteHelper)
    {
        this.siteHelper = siteHelper;
    }
}
