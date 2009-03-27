<?xml version="1.0" encoding="utf-8"?>
<!-- JNLP File for Bungee View Application -->
<jnlp
  spec="1.0+"
  codebase="http://localhost/bungee"
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
<%
String query = request.getQueryString();
if (query == null) query = "";
boolean isSocket = query.indexOf("socket=")>=0;
boolean isAssertions = query.indexOf("enableassertions")>=0;
if (isSocket){
out.write (" <all-permissions/> ");
}
%>
  </security>

  <resources>
    
<%
if (isAssertions){
	out.write ("<j2se version=\"1.4+\" initial-heap-size=\"256m\" max-heap-size=\"256m\" java-vm-args=\"-enableassertions\"/>");
} else {
	out.write ("<j2se version=\"1.4+\" initial-heap-size=\"256m\" max-heap-size=\"256m\"/>");
}
if (isSocket){
	out.write ("<jar href=\"bungeeClientSigned.jar\"/>");
} else {
	out.write ("<jar href=\"bungeeClient.jar\"/>");
}
%>

  </resources>
  <application-desc main-class="edu.cmu.cs.bungee.client.viz.Bungee">
    
<%
if (query.length() > 0) query = query + "&";
query = query + "server=http://localhost/bungee/Bungee";

response.setContentType("application/x-java-jnlp-file");
response.setHeader("Content-Disposition", "inline; filename=bungee.jnlp");
out.write ("<argument>" + query + "</argument>");
%>
     
  </application-desc>
</jnlp> 
