<%@ page import="boiler.app.core.*" %>
<jsp:include page="init.jsp"/>

<html>
  <head></head>
  <body>
    <script src="calendar.js" type="text/javascript"></script>
    <form action="">
      <p><big>Enter date you want be in your application:</big><br>
        <input type="text" value="dd-mm-yy" name="inputDate" onfocus="this.select();lcs(this)"
          onclick="event.cancelBubble=true;this.select();lcs(this)">
      </p>
    </form>
  </body>
</html>

<%!
String date = request.getParameter("inputDate");
%>
