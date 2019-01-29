<%@page import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.resource.ResourceUtil,
                com.day.cq.wcm.webservicesupport.Configuration,
                com.day.cq.wcm.webservicesupport.ConfigurationManager" %>
<%@include file="/libs/foundation/global.jsp" %><%
 
String[] services = pageProperties.getInherited("cq:cloudserviceconfigs", new String[]{});
ConfigurationManager cfgMgr = resource.getResourceResolver().adaptTo(ConfigurationManager.class);
if(cfgMgr != null) {
    String accountID = null;
    Configuration cfg = cfgMgr.getConfiguration("styla", services);
    if(cfg != null) {
        accountID = cfg.get("accountID", null);
    }
 
    if(accountID != null) {
    %>
<script type="text/javascript">
 
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', '<%= accountID %>']);
  _gaq.push(['_trackPageview']);
 
  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();
 
</script><%
    }
}
%>