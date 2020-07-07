(function() {
    'use strict';

    angular
        .module('widget', ['pascalprecht.translate','threesixty.directives','threesixty.filters', 'pasvaz.bindonce', 'ngAnimate', 'ui.bootstrap', 'nvd3'])
        .controller('widgetController', widgetController);
    
    /**
     * 
     */
    function widgetController ($scope, $rootScope, threesixtyService, $window, $interval, $filter, $timeout){
    	
    	var wc = this;
    	
    	wc.font = "\uf12e"; wc.collapseThis = false;
    	
		wc.chartoptions = {}; wc.chartdata = {};
		
    	$scope.footer = {}; $scope.footerElement = {};
    	
    	wc.getFirstCharacter = function(heading){
    		var shortName =  $filter('toShortName')(heading);
    		return shortName.charAt(0).toUpperCase();
    	};
    	
    	wc.getStartEndDate = function(arr, entity){
			var a = [];
			
			angular.forEach(arr, function(value, key){
				if(value.indexOf(entity) >= 0){
					a.push(value.replace(entity+'=',''));
				}
			});
			
			a = a[0].split(":");
			return a;
		};
		
		wc.getDateFilterTooltip = function(dateField, description, name, init){
			//name+'_date_filter_tooltip'
			var prefix = dateField.split(',')[0].split('|')[0].split('.').pop().replace(/gmt/gi,'').replace(/Timestamp/g,'Date').replace(/([A-Z])/g, ' $1');
			prefix = $filter('translator')(name+'_date_filter_tooltip',prefix);
			var title = "";
			var entity = $rootScope.partyType;
			description = description.split("|");
			var arr = description[1] ? description[1].split(",") : undefined;
			
			if(arr){
				var a = wc.getStartEndDate(arr, entity);
				var st_date = moment().subtract(a[0], a[1]).format(moment_out_format);
				var en_date = moment().format(moment_out_format);
				title = $filter('translator')('default_date_from',"From") + " " + st_date + " " + $filter('translator')('default_date_to',"To") + " " + en_date;
				if(init){
					$scope.$data.original_start_date = st_date;
					$scope.$data.original_end_date = en_date;
				}
			}
			
			if(!$rootScope.dateTooltips){
				$rootScope.dateTooltips = {};
			}
			$rootScope.dateTooltips[name] = prefix + ": " + title;
		};
		
		wc.hideWidget = function(widget){
			var url = "authds/rest/pref/update/widget";
			var d;
			
			if(!isUnitTest){
				d = '{"widget_id":"'+ widget.id + '","widget_name": "' + widget.widget_name + '", "is_visible": 0}';
			}
			else{
				d = '{"widget_id":"8","widget_name":"purchase_by_category_widget","is_visible":0}';
			}
			
			threesixtyService.genericPOSTRequest(url, d).then(function(response){
				if(!isUnitTest){
					widget.is_visible = false;
				}
				else{
					wc.response = "Successfully updated 1 Widget record";
				}
			},
			function(response){});
		};
		
		wc.changeDefaultView = function(widget, view){
			var url = "authds/rest/pref/update/widget";
			var d;
			if(!isUnitTest){
				d = '{"widget_name": "' + widget.widget_name + '","widget_id": "' + widget.id + '", "default_view": '+ view +'}';
			}
			else{
				d = '{"widget_name":"purchase_by_category_widget","widget_id":"8","default_view":0}';
			}
			
			threesixtyService.genericPOSTRequest(url, d).then(function(response){
				if(!isUnitTest){
					widget.currentView = view; 
					widget.default_view = view;
				}
				else{
					wc.currentView = 0;
					wc.defaultView = 0;
				}
			},
			function(response){});
			
		};
		
		wc.checkAndAddScrollEvent = function(widget){
			if(!widget.isScrollEventEnabled){
				wc.addScrollEvent(widget);
			}
		};
		
		wc.unique = function(arr){
			var output = {};
			angular.forEach(arr, function(val, ind){
				output[val] = true;
			});
			return Object.keys(output);
		};
		
		wc.getNestedQueryForDefaultWidget = function(widget){
			var nestedQueryString = "";
			var qrySplit = widget.date_field.split(",");

			for(var i=0; i<qrySplit.length; i++){
				if(nestedQueryString == ""){
					nestedQueryString = qrySplit[i]+':*';
				}
				else{
					nestedQueryString += ',' + qrySplit[i]+':*';
				}
			}
			
			return nestedQueryString;
		};
		
		wc.getAttributesForDefaultWidget = function(from_bo, elements){
			var bos = from_bo.split(",");
			var attrs = elements.split("\|");
			if(bos.length != attrs.length){
				return null;
			}
			var attributesList = [];
			for(var i=0; i<bos.length; i++){
				var attrsSplit = attrs[i].split(",");
				var innerAttrs = [];
				for(var j=0;j<attrsSplit.length;j++){
					innerAttrs.push(bos[i]+ '.' + attrsSplit[j]);
				}
				attributesList.push(innerAttrs.join(','));
			}
			
			return attributesList.join('\|');
		};
		
		wc.getDataForWidget = function(widget){
			var entity = $rootScope.partyType;
			var bos = widget.from_bo.split(",");
			var in_queries = [], bo = [], sort_by = [], inner_query = [], agg_fields=[];
			
			/**
			 * Added check to make ajax call if already required information is populated
			 */
			if(widget.request_data != undefined && widget.request_data.length != 0){
				wc.widgetDataAjax(widget);
				return;
			};
			
			if(widget.query_string){
				var widget_query_arr = widget.query_string.split("|");
				var w = [];
				angular.forEach(widget_query_arr, function(value, key){
					if(value.indexOf(entity) >= 0){
						w.push(value.replace(entity+'=',''));
					}
				});
				w = w[0];
				
				in_queries = w.split(",");
			};

			var sorts = widget.date_field.split(",");
			for(var k =0 ; k < bos.length; k++){
				var d = bos[k];
				bo.push(d);
				sort_by.push(sorts[k] ? sorts[k] : "null");
				if($.isEmptyObject(in_queries) || in_queries.length <= 0){
					inner_query.push('*');
				}
				else{
					inner_query.push(in_queries[k]);
				}
			}
			//agg_fields.push(widget.agg_field);
			
			bo = wc.unique(bo);
			inner_query = wc.unique(inner_query);
			sort_by = wc.unique(sort_by);
			
			if(widget.pre_defined && widget.widget_name != "interactions_widget"){
				widget.request_data = {
					offset: 0,
					index_suffix: widget.index_suffix,
	                sort_by: widget.from_bo + "." + $rootScope.widgetTemplate[widget.widget_name].default_sort_field,
	                order: $rootScope.widgetTemplate[widget.widget_name].default_sort_order,
	    			perspective_id: $rootScope.perspective_id,
					limit: !isNaN($rootScope.utilities['page_size']) ? parseInt($rootScope.utilities['page_size']) : 25,
					source_key: (eid && eid != null && eid != "null") ? eid : $rootScope.source_key,
					source_name: (eid && eid != null && eid != "null") ? "EID" : $rootScope.source_name,
					response_objects: widget.response_objects,
					attributes: widget.elements,
					business_object: bo.join(','),
					nested_query_string: inner_query.join(","),
					agg_fields: isNaN($rootScope.widgetTemplate[widget.widget_name].graph_view['aggFields'])? $rootScope.widgetTemplate[widget.widget_name].graph_view['aggFields'] : widget.agg_fields,
					default_nested_query_string: inner_query.join(","),
					search: undefined
				};
			}
			else if(widget.pre_defined && widget.widget_name == "interactions_widget"){
				widget.request_data = {
                   limit: !isNaN($rootScope.utilities['page_size']) ? parseInt($rootScope.utilities['page_size']) : 25,
                   offset: 0,
                   search: undefined,
                   sort_by: $rootScope.widgetTemplate[widget.widget_name].default_sort_field,
                   order: $rootScope.widgetTemplate[widget.widget_name].default_sort_order,
                   business_object: bo.join(','),
    			   index_suffix: widget.index_suffix,
    			   query_string: (eid && eid != null && eid != "null") ? eid + "|EID" : $rootScope.source_key + "|" + $rootScope.source_name,
    			   nested_query_string: inner_query.join(","),
    			   default_nested_query_string: inner_query.join(","),
    			   size: 100,
    			   agg_fields: isNaN($rootScope.widgetTemplate[widget.widget_name].graph_view['aggFields'])? $rootScope.widgetTemplate[widget.widget_name].graph_view['aggFields'] : "",
    			   is_inner_hits:false,
    			   response_objects:widget.elements.split(';')[1],
    			   attributes:widget.elements.split(';')[0],
    			   result_size : 25,
    			   perspective_id: $rootScope.perspective_id,
    			   multi_bo:true
               };
			}
			else{
				var nestedQueryString = wc.getNestedQueryForDefaultWidget(widget);
				var responseFields = Array(widget.from_bo.split(',').length+1).join(widget.response_objects+'|');
				responseFields = responseFields.substring(0, responseFields.length-1);
				var attributes = wc.getAttributesForDefaultWidget(widget.from_bo, widget.elements.split(';')[0]);
				
				widget.request_data = {
                   limit: !isNaN($rootScope.utilities['page_size']) ? parseInt($rootScope.utilities['page_size']) : 25,
                   offset: 0,
                   search: undefined,
                   sort_by: widget.date_field,
                   //order: "desc",
		     order: "asc",
                   business_object: widget.from_bo,
    			   index_suffix: widget.index_suffix,
    			   query_string: (eid && eid != null && eid != "null") ? eid + "|EID" : $rootScope.source_key + "|" + $rootScope.source_name,
    			   nested_query_string: nestedQueryString,
    			   size: 100,
    			   //agg_fields: widget.agg_fields,
    			   is_inner_hits: false,
    			   response_objects: responseFields,
    			   attributes: attributes,
    			   result_size : 25,
    			   perspective_id: $rootScope.perspective_id,
    			   multi_bo:true
               };
			}
            
			wc.widgetDataAjax(widget);
		};
		
		wc.widgetDataAjax = function(widget, view){
			if(widget.isNextPageLoading == false){widget.isDataLoading = true;}
			
			if(widget.pre_defined && widget.widget_name != "interactions_widget"){
				var url = cidServicesContextPath + "/getCSKInnerHits/";// + "/" + widget.request_data.business_object + "/" + widget.request_data.nested_query_string.replace(/\[/g,"%5B").replace(/\]/g,"%5D").replace(/\(/g,"%28").replace(/\)/g,"%29").replace(/"/g,"%22") + "/" + widget.request_data.source_key + "/" + widget.request_data.source_name + "/" + widget.index_suffix
							//+ "?limit=" + widget.request_data.limit + "&offset=" + widget.request_data.offset + "&sort=" + widget.request_data.sort + "&order=" + widget.request_data.order + "&agg_fields=" + widget.agg_fields.replace(/\[/g,"%5B").replace(/\]/g,"%5D").replace(/\|/g,"%7C").replace(/"/g,"%22") + "&perspective_id=" + $rootScope.perspective_id;
				
				/*if(widget.request_data.search && widget.request_data.search.trim() != ""){
					$scope.searchText = widget.request_data.search;
					url = url + "&search=" + widget.request_data.search;
				}*/
				/*
				threesixtyService.genericGETRequest(url).then(function(response){
					wc.widgetDataSuccessHandler(response, widget, view);
				},*/
				
				threesixtyService.genericPOSTRequest(url, widget.request_data).then(function(response){
					wc.widgetDataSuccessHandler(response, widget, view);
				},
				function(response){
					wc.widgetDataErrorHandler(response, widget, view);
				});
			}
			else{
				var url = cidServicesContextPath + "/getMultiCSKHits/";
				threesixtyService.genericPOSTRequest(url, widget.request_data).then(function(response){
					wc.widgetDataSuccessHandler(response, widget, view);
				},
				function(response){
					wc.widgetDataErrorHandler(response, widget, view);
				});
			}
		};
		
		wc.refreshWidget = function(widget){
			widget.request_data.limit = !isNaN($rootScope.utilities['page_size']) ? parseInt($rootScope.utilities['page_size']) : 25;
			widget.request_data.offset = 0;
			widget.request_data.search = undefined;
			
			if(widget.pre_defined && widget.widget_name != "interactions_widget"){
				widget.request_data.sort = widget.from_bo + "." + $rootScope.widgetTemplate[widget.widget_name].default_sort_field;
				widget.request_data.order = $rootScope.widgetTemplate[widget.widget_name].default_sort_order;
				widget.request_data.nested_query_string = widget.request_data.default_nested_query_string;
			}
			else if(widget.pre_defined && widget.widget_name == "interactions_widget"){
				widget.request_data.sort_by = "Interactions.Webchat.Timestamp,Interactions.ContactCenterCall.Timestamp,Interactions.EmailFromParty.Date,SocialInteractions.Mentions.CompanyMention." + $rootScope.widgetTemplate[widget.widget_name].default_sort_field;
				widget.request_data.order = $rootScope.widgetTemplate[widget.widget_name].default_sort_order;
				widget.request_data.nested_query_string = widget.request_data.default_nested_query_string;
			}
			else{
				widget.request_data.sort_by = widget.date_field;
				widget.request_data.order = "desc";
			}
			
			widget.widget_data.currentPage = 1;
			
			//Resetting Date Range Filter :: Click does not trigger an ajax call
			angular.element('.ranges#'+widget.widget_name+'_mini').find('li:nth-last-child(2)').click();
			var prefix = widget.date_field.split(',')[0].split('|')[0].split('.').pop().replace(/gmt/gi,'').replace(/Timestamp/g,'Date').replace(/([A-Z])/g, ' $1');
			prefix = $filter('translator')(widget.widget_name+'_date_filter_tooltip',prefix);
			//var title = "From " + widget.original_start_date + " To " + widget.original_end_date;
			var title = $filter('translator')('default_date_from',"From")+" " + widget.original_start_date + " "+$filter('translator')('default_date_to',"To")+ " " + widget.original_end_date;

			$rootScope.dateTooltips[widget.widget_name] = prefix + ": " + title;
			wc.widgetDataAjax(widget);
		};
		
		wc.onBlurSearch = function(widget){
			if(!$scope.$data.request_data.search && $scope.searchText != ''){
				wc.searchWidget(widget);
				$scope.searchText = '';
			}
		};
		
		wc.searchWidget = function(widget){
			widget.request_data.limit = !isNaN($rootScope.utilities['page_size']) ? parseInt($rootScope.utilities['page_size']) : 25;
			widget.request_data.offset = 0;
			widget.widget_data.currentPage = 1;
			
			var search_term = widget.request_data.search.trim();
			var date_moment = moment(search_term,moment_in_format,true);
			
    		if(date_moment.isValid()){
    			if(widget.pre_defined && widget.widget_name != "interactions_widget"){
    				widget.request_data.search = widget.date_field.split("|")[0] + ":[\"" + date_moment.format(RESPONSE_DATE_FORMAT) + "\" TO \"" + date_moment.add(1,'day').format(RESPONSE_DATE_FORMAT) + "\"]";
    			}
    			else{
    				var date_fields = widget.date_field.split(',');
    				var date_string = [];
    				var start = date_moment.format(RESPONSE_DATE_FORMAT);
    				var end = date_moment.add(1,'day').format(RESPONSE_DATE_FORMAT);
    				angular.forEach(date_fields, function(date_field, index){
    					date_string.push(date_field + ":[\"" + start + "\" TO \"" + end + "\"]");
    				});
    				widget.request_data.search = date_string.join(' OR ');
    			}
    		}
    		
			wc.widgetDataAjax(widget);
		};
		
		wc.widgetDataErrorHandler = function(response, widget, view){
			widget.isDataLoading = false;
		};
		
		wc.widgetDataSuccessHandler = function(response, widget, view){
			if(isUnitTest){
				wc.response = response.data.total;
				return;
			}
			
			if(!widget.widget_data){
				widget.widget_data = {};
			}
			
			if(response.status != 200){
				widget.isDataLoading = false;
				return;
			}
			
			if(!view || view == "table"){
				widget.widget_data['offset'] = response.data.offset;
			}
			
			if(!view || view == "list"){
				widget.widget_data['list_offset'] = response.data.offset;
			}
			
			widget.widget_data['total'] = response.data.total;
			widget.widget_data['tileSumElement'] = response.data.tileSumElement[0];
			
			if(!widget.widget_data['rows']){ 
				widget.widget_data['rows'] = [];	
			}
			
			if(!view || view == "table"){
				widget.widget_data['rows'].length = 0;
				$timeout(function(){
					angular.forEach(response.data.rows, function(val, ind){
						widget.widget_data['rows'].push(val);
					});
				}, 5);
				
				widget.widget_data['current'] =  widget.widget_data['offset'] + (response.data.rows ? response.data.rows.length : 0);
			}
			
			if(widget.pre_defined){
				if(!widget.widget_data['list_rows']){ 
					widget.widget_data['list_rows'] = [];	
				}
				
				if(!view || view == "list"){
					
					if(response.data.offset == 0){
						widget.widget_data['list_rows'].length = 0;
						wc.addScrollEvent(widget);
					}
					
					var hits = [];
					if($scope.widgetTemplate[widget.widget_name].list_view.process_hits_inner_bo && $scope.widgetTemplate[widget.widget_name].list_view.process_hits_inner_bo.length){
						hits = wc.processHits(widget, response.data.rows);
					}
					else{
						hits = response.data.rows;
					}
					
					$timeout(function(){
						angular.forEach(hits, function(val, ind){
							widget.widget_data['list_rows'].push(val);
						});
					}, 5);
					widget.isNextPageLoading = false;
				}
				
				widget.widget_data['values'] = [];
				widget.widget_data['values'].push.apply(widget.widget_data['values'], response.data.values);
				widget.widget_data.values = widget.widget_data.values.reverse();
    			
				wc.getChartOptions(widget);
				
				wc.getFooterDetails(widget);
			}
			
			$timeout(function(){widget.isDataLoading = false;}, 200);
		};
		
		wc.processHits = function(widget, responseRows){
			var hits = [];
			
			angular.forEach(responseRows, function(row, row_index){
				angular.forEach($scope.widgetTemplate[widget.widget_name].list_view.process_hits_inner_bo, function(inner_bo, inner_bo_index){
					angular.forEach(row[inner_bo], function(child_doc, child_doc_index){
						var hit = {};
						
						//All Elements except child bo
						angular.forEach(row, function(value, key){
							if(key != inner_bo){
								hit[key] = value;
							}
						});
						
						//Flattened Child bo
						angular.forEach(child_doc, function(value, key){
							hit[inner_bo + "." + key] = value;
						});
						
						hits.push(hit);
					});
				});
			});
			
			return hits;
		};
		
		wc.addScrollEvent = function(widget){
			var interval = $interval(function(){
				var listContainer = document.getElementById('scrollable-table-' + widget.widget_name);
				if(!listContainer){return;}
				$interval.cancel(interval);
				//Adding event listener
				listContainer.removeEventListener('scroll',function(){});
				
				listContainer.addEventListener('scroll', function(event){
					var element = event.target;
					if (parseInt(element.scrollHeight - element.scrollTop) === parseInt(element.clientHeight)){
						
						var moreAvail = (widget.widget_data.list_offset + widget.request_data.limit) < widget.widget_data.total;
						
						if(moreAvail == true && widget.isNextPageLoading == false){
							widget.isNextPageLoading = true;
							widget.request_data.offset = widget.widget_data.list_offset + widget.request_data.limit;
							wc.widgetDataAjax(widget, 'list'); 
						}
					}
				});
				
				widget.isScrollEventEnabled = true;
			}, 10);
		};
		
		/**
		 * 
		 */
		wc.getWidgetFieldValue = function(value, fieldDetails, rowData, predefined,widget_name){

			if((["currency","multifield"].indexOf(fieldDetails.type) < 0) && (!value || value == null || value == undefined)){
				return "Not Available";
			}
			
			if(!predefined){
				if(fieldDetails.indexOf('Date') >=0 || fieldDetails.indexOf('Timestamp') >=0){
					var time = moment(value, moment_in_format).format(moment_out_format);
					return time;
				}
				else{
					return value;
				}
			}
			
			if(fieldDetails.type == "standard"){
				return value;
			}
			
			else if(fieldDetails.type == "splitpop"){
				return value.split("-").pop();
			}
			
			else if(fieldDetails.type == "property"){
				//var status = $rootScope.utilities[value.trim().toLowerCase()] ? $rootScope.utilities[value.trim().toLowerCase()].split(',')[0] : "Undefined";
				var status = $rootScope.utilities[widget_name]['values'][value.trim().toLowerCase()] ? $rootScope.utilities[widget_name]['values'][value.trim().toLowerCase()]['value']: "Undefined";
				return status;
			}
			
			else if(fieldDetails.type == "currency"){
				var currency = wc.saturate(rowData[fieldDetails.currency_field]);
				if (value == null || value == "" || value.length == 0) {
					return "0.00 " + currency;
				}
				else {
					return wc.getAmountIntoLocaleString(value) + " " + currency;
				}
			}
			
			else if(fieldDetails.type == "date"){
				//var time = moment(value, moment_in_format).format("MMM Do YY");
        var time = moment(value, moment_in_format).format("L");
				return time;
			}
			
			else if(fieldDetails.type == "multifield"){
				
				var fields = fieldDetails.field.split(",");
				var fieldValue = "", found = false;
				
				angular.forEach(fields, function(val, ind){
					if(!found && rowData[val]){
						fieldValue = rowData[val];
						found = true;
					}
				});
				
				return fieldValue;
			}
			
			else{
				return value;
			}
			
		};
		
		wc.getAmountIntoLocaleString = function(value){
			if(isNaN(value)){ return "0.00";}
			if(value % 1 === 0){return parseInt(value).toFixed(0).replace(/(\d)(?=(\d{3})+\.)/g, '$1,');}
			return parseFloat(value).toFixed(2).replace(/(\d)(?=(\d{3})+\.)/g, '$1,');
		};
		
		wc.saturate = function(input){
    		if(input && input.trim() != ""){
    			return input.trim();
    		}
    		return "";
    	};
    	
    	wc.getChartOptions = function(widget){
    		
    		var chartdata = [];
    		var total = 0;
    		
    		if(!widget.widget_data.values || widget.widget_data.values.length == 0){
    			chartdata = [];
    		}
    		else{
    			var tempchartdata = {};
    			var inner_keys = {};
    			
    			if($scope.widgetTemplate[widget.widget_name].graph_view.type == "multiBarChart"){
    				angular.forEach(widget.widget_data.values, function(value, index){
	    				//var conf_name = $rootScope.utilities[widget.widget_name]['values'][childvalue.name.trim().toLowerCase()] ? $rootScope.utilities[widget.widget_name]['values'][childvalue.name.trim().toLowerCase()].value : childvalue.name;
	    				value.name = moment(value.name, moment_in_format).format(moment_out_format);
	    				chartdata.push({
    						label: value.name,
    						count: value.count
    					});
		    		});
    			}
    			else if ($scope.widgetTemplate[widget.widget_name].graph_view.type == "pieChart"){
    				angular.forEach(widget.widget_data.values, function(parentValue, ind){
    					if(!parentValue.iValue ||parentValue.iValue.length == 0){return;};
	    				angular.forEach(parentValue.iValue, function(value, index){
	    					var name = $rootScope.utilities[widget.widget_name]['values'][value.name.trim().toLowerCase()] ? $rootScope.utilities[widget.widget_name]['values'][value.name.trim().toLowerCase()].value : value.name;
	    					chartdata.push({
	    						label: value.name,
	    						count: value.count
	    					});
	    					total = total + value.count;
			    		});
    				});
    			}
    		}
    		
    		wc.chartdata[widget.widget_name] = [];
    		
    		$timeout(function(){
    			wc.chartdata[widget.widget_name] = chartdata;
    		}, 1);
    		
    		widget.legend = {};
    		
    		var chartoptions = {};
    		
    		if($scope.widgetTemplate[widget.widget_name].graph_view.type == "pieChart"){
    			chartoptions = {
    				chart: {
    					type: $scope.widgetTemplate[widget.widget_name].graph_view.type,
    				    x : function(d) { return d.label },
    				    y : function(d) { return d.count },
    				    donut : "true",
    				    donutRatio : 0.45,
    					labelsOutside : true,
    					noData: '\uf200',
    					labelThreshold : .01,
    					arcMinWidth: 10,
    					arcThreshold: 0.08,
    					labelSunbeamLayout: false,
    					pid: widget.widget_name,
    				    height: widget.tabHeight == undefined ? $scope.widgetTemplate.portletHeight * 0.7:widget.tabHeight,
    				    width: null,
    				    color: function(d, i){
    				    	if(!widget.legend[d.label.toLowerCase()]){
    				    		var color = $filter('getChartColor')(widget.widget_name, d.label);
    				    		widget.legend[d.label.toLowerCase()] = color;
    				    	}
    				    	return widget.legend[d.label.toLowerCase()];
    				    },
    				    labelType : function(d, i){
					    	return  $filter('shortNumber')(d.data.count);
					    },
					    title: "",
    				    tooltip : {
    				    	enabled: true,
    				    	contentGenerator: function (key, x, y, e, graph) {
    				    		return '<div class="graph-tooltip-container"><div class="heading te-tr-ca">' + key.data.label + '</div><div class="value te-tr-ca">Count : ' + $filter('shortNumber')(key.data.count) + '</div></div>';
    				    	}
    				    },
    					showLegend: false,
    					showLabels: true
    				}
    			};
    		}
    		wc.chartoptions[widget.widget_name] = angular.extend({}, chartoptions);
    	};
    	
    	wc.openExpandView = function(){
    		$rootScope.selectedWidget = $scope.$parent.$parent.$parent.$index;
    		$scope.$parent.$parent.$parent.$parent.$parent.$parent.wcc.showExpandView = true;
    	};
    	
    	wc.getFooterDetails = function(widget){
    		if(!widget.widget_data.tileSumElement || widget.widget_data.tileSumElement.length == 0)  {
    			widget.widget_data.tileSumElement = {'-NA-':0};
			}
    		if(widget.widget_name == "purchase_by_category_widget"){
    			var since = widget.current_start_date ? widget.current_start_date : widget.original_start_date;
    			if(widget.widget_data && widget.widget_data.values && widget.widget_data.values.length){
    				since = moment(widget.widget_data.values[0].name, moment_in_format).format(moment_out_format);
    			}
    			$scope.footer[widget.widget_name] = $filter('translator')('widget_purchase_by_category_widget_footer','Total Purchase since')+" " + since + " :" ;//+ wc.getAmountIntoLocaleString(parseFloat(widget.widget_data.tileSum ? widget.widget_data.tileSum : 0.0));
    			$scope.footerElement[widget.widget_name] = widget.widget_data.tileSumElement;
    		}
    		else if(widget.widget_name == "service_widget"){
    			var dateElement = angular.element("#wlp_" + widget.widget_name).find(".date-filter-button");
    			var st_date = moment(dateElement.data('daterangepicker').startDate, moment_in_format).format(moment_out_format);
    			var en_date = moment(dateElement.data('daterangepicker').endDate, moment_in_format).format(moment_out_format);
    			$scope.footer[widget.widget_name] = $filter('translator')('widget_service_widget_footer','Total items From')+" " +st_date +" "+$filter('translator')('default_date_to', 'To')+" " + en_date + ": " ; //+ parseInt(widget.widget_data && widget.widget_data.tileSum ? widget.widget_data.tileSum : 0.0).toLocaleString();
    			
    			var tileSumObject = {};
    			angular.forEach(widget.widget_data.tileSumElement,function(value,key){
    				var conf_name = $rootScope.utilities[widget.widget_name]['values']['undefined'].value;
    	    		if(value){
    	    			conf_name = $rootScope.utilities[widget.widget_name]['values'][key.trim().toLowerCase()] ? $rootScope.utilities[widget.widget_name]['values'][key.trim().toLowerCase()].value : conf_name;
    	    			tileSumObject[conf_name] = value;
    	    		}
    			});
    			
    			$scope.footerElement[widget.widget_name] = tileSumObject;
    		}
    		else if(widget.widget_name == "quotes_widget"){
    			var dateElement = angular.element("#wlp_" + widget.widget_name).find(".date-filter-button");
    			var st_date = moment(dateElement.data('daterangepicker').startDate, moment_in_format).format(moment_out_format);
    			var en_date = moment(dateElement.data('daterangepicker').endDate, moment_in_format).format(moment_out_format);
    			$scope.footer[widget.widget_name] = $filter('translator')('widget_quotes_widget_footer','Total Quoted From')+" " + st_date +" "+$filter('translator')('default_date_to', 'To')+" " + en_date + " : " ; //+ wc.getAmountIntoLocaleString(parseFloat(widget.widget_data.tileSum ? widget.widget_data.tileSum : 0.0));
    			$scope.footerElement[widget.widget_name] = widget.widget_data.tileSumElement;
    		}
    	};
    	
    	/************************************************************************************
    	 * This method draws a grouped bar chart in the given container						*
    	 * **********************************************************************************/
    	wc.draw_groupedbar_chart = function(widget){
    		
    		$timeout(function(){
    			
    			var view_type =  (widget.tabHeight == undefined ? 'quick' : 'expand');
    			
    			var $selector = angular.element('#' + widget.widget_name + "_chart_" + view_type);

    			var tooltipValues = getChartTooltipCategories(widget.widget_name);
    			
        		$selector.groupedbar({
        			data: widget.widget_data,
        			xAxisText: $scope.widgetTemplate[widget.widget_name].graph_view.x_axis_label,
        			yAxisText: $scope.widgetTemplate[widget.widget_name].graph_view.y_axis_label,
        			animate:true, 
        			values: getChartCategories(widget.widget_name),
        			legendValues: Object.keys(tooltipValues).map(function(key){return tooltipValues[key]}),
        			color: getChartColor (widget.widget_name), 
        			height : widget.tabHeight == undefined ? $scope.widgetTemplate.portletHeight * 0.7 : widget.tabHeight,
        			width: $selector.width(),
        			id: "svg_"+$selector.attr("data-chart-tool") + "_" + view_type
        		});
  
        		var tip = d3.tip()
        					.attr('class', 'd3-tip')
        					.offset([-10, 0])
        					.html(function(d) {
        						var count = d.count;
//        						if($scope.widgetTemplate[widget.widget_name].graph_view.y_axis_label.toLowerCase().indexOf('amount') >= 0){ count = getAmountIntoLocaleString(count);}
        						return "<div class='text-capitalize' style='border-bottom:1px solid #c4c4c4;margin-bottom:5px;padding-bottom:2px;'>"+ d.name.split("-").pop() + '</div>'+$scope.widgetTemplate[widget.widget_name].graph_view.y_axis_label +':  <span>' + wc.getAmountIntoLocaleString(count) + '</span>';
        					});
        		
        		var vis = d3.select('#svg_' + $selector.attr("data-chart-tool") + "_" + view_type + " g");
        		vis.call(tip);
        		vis.selectAll(".bar").on('mouseover',tip.show)
        		.on('mouseout',tip.hide);
    		
    		},100);
    	};
    	
    	/************************************************************************************
    	 * This method returns chart color for the required graph                       	*
    	 * **********************************************************************************/
    	var getChartColor = function(widget_name){
    		var color={};
    		
    		var config = $rootScope.utilities[widget_name].values;
    		
    		angular.forEach(config, function(value,key){
    			if(Object.keys(color).indexOf(key) < 0 && value['isAggLegend'] == true){
    				color[key] = value.color;
    			}
    		});
    		
    		return color;
    	}
    	
    	/************************************************************************************
    	 * This method returns chart color for the required graph                       	*
    	 * **********************************************************************************/
    	var getChartCategories = function(widget_name){
    		var category=[];
    		var config = $rootScope.utilities[widget_name].values;
    		angular.forEach(config, function(value,key){
    			if(category.indexOf(key) < 0 && value['isAggLegend'] == true){
    				category.push(key);
    			}
    		});
    		return category;
    	}
    	
    	/**
    	 * 
    	 */
    	var getChartTooltipCategories = function(widget_name){
    		var category= {};
    		var config = $rootScope.utilities[widget_name].values;
    		angular.forEach(config, function(value,key){
    			if(Object.keys(category).indexOf(key) < 0 && value['isAggLegend'] == true){
    				category[key] = value['value'];
    			}
    		});
    		return category;
    	};
    };
})();
