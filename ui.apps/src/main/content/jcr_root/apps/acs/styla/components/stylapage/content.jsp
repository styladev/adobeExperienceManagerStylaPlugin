<%@page contentType="text/html"
            pageEncoding="utf-8"%><%
%><%@include file="/libs/foundation/global.jsp"%><div>

<div>
    <h3>Styla Settings</h3>
    <ul>
        <li><div class="li-bullet"><strong>Client: </strong><%= xssAPI.encodeForHTML(properties.get("client", "")) %></div></li>
        <li><div class="li-bullet"><strong>Seo Api Url: </strong><%= xssAPI.encodeForHTML(properties.get("seoApiUrl", "")) %></div></li>
        <li><div class="li-bullet"><strong>Script: </strong><%= xssAPI.encodeForHTML(properties.get("script", "")) %></div></li>
    </ul>
</div>
