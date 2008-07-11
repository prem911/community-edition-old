<#import "/org/alfresco/modules/blog/blogpost.lib.ftl" as blogpostLib/>

<script type="text/javascript">//<![CDATA[
   new Alfresco.BlogPost("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site}",
      mode: "<#if editMode>edit<#else>view</#if>",
      postId: "${page.url.args["postId"]}",
      postRef: "${item.nodeRef}"
   }).setMessages(
      ${messages}
   );
//]]></script>

<div id="discussionsBlogHeader2">
	<div class="leftDiscussionBlogHeader listTitle">
		<span class="backLink">
			<a href="${url.context}/page/site/${page.url.templateArgs.site}/blog-postlist">
				${msg("header.back")}
			</a>
		</span>
	</div>
</div>

<div id="${args.htmlid}-post">
   <div id="${args.htmlid}-viewDiv">
      <@blogpostLib.blogpostViewHTML htmlid=args.htmlid post=item/>
   </div>
</div>
