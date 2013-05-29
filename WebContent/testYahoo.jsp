<script type="text/javascript"
	src="http://ajax.googleapis.com/
ajax/libs/jquery/1.4.2/jquery.min.js"></script>
<script type="text/javascript">
	$(document)
			.ready(
					function() {

						$(".search_input")
								.keyup(
										function() {
											var search_input = $(this).val();
											var keyword = encodeURIComponent(search_input);
											// Yahoo Search API 
											var yahoo_url = 'http://boss.yahooapis.com/ysearch/web/v1/'
													+ keyword+'?appid=dj0yJmk9TzZxelZHOTRhaFdaJmQ9WVdrOWIzRm5lV3N6TjJzbWNHbzlOalkzTXpNNU5EWXkmcz1jb25zdW1lcnNlY3JldCZ4PTkx&format=json&callback=myData'; 

$.ajax
({
																										type : "GET",
														url : yahoo_url,
														dataType : "jsonp",
														success : function(
																response) {
															$("#result").html(
																	'');
															if (response.ysearchresponse.resultset_web) {
																$
																		.each(
																				response.ysearchresponse.resultset_web,
																				function(
																						i,
																						data) {
																					var title = data.title;
																					var dis = data.abstract;
																					var url = data.url;

																					var final = "<div class='webresult'><div class='title'><a href='"+url+"'>"
																							+ title
																							+ "</a></div><div class='desc'>"
																							+ dis
																							+ "</div><div class='url'>"
																							+ url
																							+ "</div></div&gt;";

																					$(
																							"#result")
																							.append(
																									final); // Result

																				});
															} else {
																$("#result")
																		.html(
																				"<div id='no'>No Results</div>");
															}
														}
													});
										});
					});
</script>
// HTML code
<input type="text" class='search_input' />
<div id="result"></div>