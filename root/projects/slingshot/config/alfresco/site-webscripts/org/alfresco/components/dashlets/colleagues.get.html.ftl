<script type="text/javascript">//<![CDATA[
   new Alfresco.widget.DashletResizer("${args.htmlid}", "${instance.object.id}");
//]]></script>
<div class="dashlet">
   <div class="title">${msg("header.colleagues")}</div>
   <div class="body scrollableList" <#if args.height??>style="height: ${args.height}px;"</#if>>
<#if (memberships?size > 0)>
   <#list memberships as m>
      <div class="detail-list-item <#if m_index = 0>first-item<#elseif !m_has_next>last-item</#if>">
         <div class="avatar">
            <img src="${url.context}<#if m.avatar??>/proxy/alfresco/api/node/${m.avatar?replace(':/','')}/content/thumbnails/avatar?c=force<#else>/components/images/no-user-photo-64.png</#if>" alt="Avatar" />
         </div>
         <div class="person">
            <h4><a href="${url.context}/page/user/${m.person.userName?url}/profile" class="theme-color-1">${m.person.firstName?html} <#if m.person.lastName??>${m.person.lastName?html}</#if></a></h4>
            <span>${m.role}</span>
         </div>
      </div>
   </#list>
<#else>
      <div class="detail-list-item first-item last-item">
         <h3>${msg("label.noMembers")}</h3>
      </div>
</#if>
   </div>
</div>