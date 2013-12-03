var asnn2 = asnn2 || {};


asnn2.setupStdntListTableParsers = function (){   
	    //apply orderers to table
    
	    $("table").tablesorter({ 
	    	//This is the default text extractor for each cell.  It looks for a <span> tag
	    	//and grabs the text inside that tag and uses it to sort the data
	    	//if span tag doesn't exist, it just replaces it with an empty string
	    	textExtraction: function(node) { 
	            // extract data from markup and return it  
	            var spanNode = $("span", node);
	            if(spanNode){
	            	if($(spanNode).html()){
	            		return $(spanNode).html();
	            	}else{
	            		return "";
	            	}
	            }else{
	            	return "";
	            } 
        	},
        	
        	//This is used to disable columns for sorting.  The number represents the column
        	//you are setting	    	
	    	headers: {
        	0: { 
                // disable it by setting the property sorter to false 
                sorter: false 
            },
        	3: { 
                // disable it by setting the property sorter to false 
                sorter: false 
            },
	    	4: { 
                // disable it by setting the property sorter to false 
                sorter: false 
                },
                5: { sorter: false }
	        }
	    });
	    
	    //this will adjust the sort images so they are right after the text
		//8px added for padding
		jQuery("th", jQuery("tr", $("table"))).each(function(){
			var spanNode = $("span", this);
			if(spanNode){
				$(this).css("background-position", ($(spanNode).width() + 8) + "px");
			}
		});	
   
	};

//asnn2.initIRubricTable = function(gradebookIds,gradebookUid,siteId,studentId) {
asnn2.initIRubricTable = function(gradebookIds,gradebookUid,siteId,studentId,gradebookPlacementId) {
    var launchIRubric = function(gradebookId) {
        return function(event) {
            //var urlPage = asnn2.makeIRubricUrlPrefix()+
            var urlPage = asnn2.makeIRubricUrlPrefix(gradebookPlacementId)+
                "/iRubricLink.jsp?p=v&tool=asnn2&gradebookUid="+gradebookUid+"&siteId="+siteId+"&rosterStudentId="+studentId+"&gradebookItemId="+gradebookId;
            window.open(urlPage,'_blank',
                        'width=800,height=600,top=20,left=100,menubar=yes,status=yes,location=no,toolbar=yes,scrollbars=yes,resizable=yes');
        };
    };
    //jQuery.getJSON(asnn2.makeIRubricUrlPrefix()+"/iRubricLink.jsp?p=ra&tool=asnn2&gradebookUid="+gradebookUid+"&siteId="+siteId+"&gradebookItemId="+gradebookIds.toString(),function(data){
    jQuery.getJSON(asnn2.makeIRubricUrlPrefix(gradebookPlacementId)+"/iRubricLink.jsp?p=ra&tool=asnn2&gradebookUid="+gradebookUid+"&siteId="+siteId+"&gradebookItemId="+gradebookIds.toString(),function(data){
        var showIRubricCol = false;

        for (var i in data) {
            if (data[i] === true) {
                if (showIRubricCol === false) {
                    jQuery(".irubric-col").show();
                    showIRubricCol = true;
                }
                jQuery(".gradebook-"+i+" a.irubric-link").click(launchIRubric(i)).show();
            }
        }
    });

};