<%
Cookie cookie = new Cookie("JSESSIONID", request.getParameter("JSESSIONID"));
cookie.setPath("/");
response.addCookie(cookie);
response.sendRedirect("/client/BigBlueButton.html");
%>