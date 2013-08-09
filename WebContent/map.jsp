<!--
	@author Yayang Tian
 	http://cis.upenn.edu/~yaytian
-->

 
 
<!DOCTYPE html>
<html lang="en">
    <head>
        <title>How are you</title>
        <script src="http://code.jquery.com/jquery-latest.min.js"></script>
        <script>
            $(document).ready(function() {                        // When the HTML DOM is ready loading, then execute the following function...
                $('#feedbackBtn').click(function() {               // Locate HTML DOM element with ID "somebutton" and assign the following function to its "click" event...
                    
                    $.get('feedback?q=new', function(jsonList) { // Execute Ajax GET request on URL of "someservlet" and execute the following function with Ajax response text...
                        var $table = $('#table1');
                    $.each(jsonList, function(index, result){
                        $('<tr>').appendTo($table)
                        .append($('<td>').text(result.title))
                        .append($('<td>').text(result.URL))
                        .append($('<td>').text(result.date))
                        .append($('<td>').text(result.snippet));                  
                    });
                    
                        $('#result').text(responseText);         // Locate HTML DOM element with ID "somediv" and set its text content with the response text.
                    });
                });
            });
        </script>
    </head>
    <body>
        <h2>This is the title</h2>
        <button id="feedbackBtn">press here</button>
        <div id="result"></div>
        
        <table id='table1'>
        </table>
        
    </body>
</html>