<?xml version="1.0" encoding="utf-8"?>
<!-- JNLP File for Bungee View Application -->
<jnlp
  spec="1.0+"
  codebase="http://cityscape.inf.cs.cmu.edu/bungee/"
  >
  <information>
    <title>Bungee View</title>
    <vendor>Carnegie-Mellon University</vendor>
    <homepage href="Abo.html"/>
    <description>Bungee View Image Collection Browser</description>
    <description kind="one-line">Bungee View Image Collection Browser</description>
    <description kind="tooltip">Bungee View</description>
    <description kind="short">Search, browse, and data-mine image collections based on their meta-data.</description>
    <icon href="bungee.gif"/>
  </information>
  <security>
  </security>
  <resources>
    <j2se version="1.4+" initial-heap-size="64m"/>
    <jar href="art_shrunk.jar"/>
  </resources>
  <application-desc main-class="viz.Art">
    
<%
String query = request.getQueryString();
if (query == null) query = "";
if (query.length() > 0) query = query + "&";
query = query + "server=http://cityscape.inf.cs.cmu.edu/hp/HistoricPittsburgh";

response.setContentType("application/x-java-jnlp-file");
response.setHeader("Content-Disposition", "inline; filename=bungee.jnlp");
out.write ("<argument>" + query + "</argument>");
%>
     
  </application-desc>
</jnlp> 

