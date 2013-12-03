package org.sakaiproject.gradebook.gwt.client.util;

import com.google.gwt.user.client.Window;

public class OpenWindow  {  
	   
	public OpenWindow(String url){
		
		Window.open(url,"irubric","menubar=no," + 
	           "location=false," + 
	           "resizable=yes," + 
	           "scrollbars=yes," + 
	           "status=no," + 
	           "width=800,"+ 
	           "height=600,"+
	           "top=20,"+
	           "left=100,"+
	           "dependent=true");
		
	}
	
}  