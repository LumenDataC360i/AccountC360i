(function() {
	'use strict';

	/**
	 * 
	 */
	angular
	.module('customersearch.filters',[]);


	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('searchlike', search);

	/**
	 * 
	 */
	function search(){
		return function(collection, searchesFor){
			if (!searchesFor) {
				return collection;
			}
			var results = [];
			searchesFor = searchesFor.toLowerCase();
			angular.forEach(collection, function(item) {
				if (item.toLowerCase().indexOf(searchesFor) !== -1) {
					results.push(item);
				}
			});

			return results;
		};
	}
	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('moment', momentTheDate);

	/**
	 * 
	 */
	function momentTheDate(){
		return function(string,formatter) {
			if(string == undefined || string.trim().length == 0) {
				return string;
			}
			var formatted = moment(string).format("D MMM'YY");
			if(formatter){
				formatted = moment(string).format(formatter);
			}
			return  formatted;
		}
	}
	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('toShortName', toShortName);

	/**************************************************************************
	 * This method Frames EntityLogo Text                                     *
	 ***************************************************************************/
	function toShortName(){
		return function(name, length){
			if(!name || name.toLowerCase() == 'na'){
				return 'NA';
			}

			if(!angular.isString(name)){ 
				return name.toString().charAt(0);
			}
			var data_name =  name.trim().replace(/  +/g, ' ').split(' '), data_output = "";
			length = length ? length : 2;
			for ( var i = 0; i < length; i++) {
				if(data_name[i] !=undefined)
					data_output += data_name[i].substring(0,1);
			}
			return data_output.toUpperCase();
		}
	}

	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('getEntityType', getEntityType);

	/**
	 * 
	 */
	function getEntityType(){
		return function(string) {
			var data= string.source;
			var entityType='Person';
			if(data.Control){
				entityType = data.Control[0].EntityType;
				if(data.Control.length > 1){
					data.Control.forEach(function(d,i){
						entityType = ['organization','org','o'].indexOf(d.EntityType.toLowerCase()) >= 0 ? "Organization" : "Person";
					});
				}
			}
			else if(data.Demographics.Organization || data.Demographics.OrganizationName){
				entityType = 'Organization';
			}
			else if(data.Demographics.Person || data.Demographics.PersonName){
                                entityType = 'Person';
                        }
			return entityType;
		}
	}
	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('getEntityName', ['$filter',getEntityName]);

	/**
	 * 
	 */
	function getEntityName($filter){
		return function(string) {
			var entityType = $filter('getEntityType')(string);
			var data = string.source;
			if( entityType == 'Organization'){
				name = data.Demographics.OrganizationName ? (data.Demographics.OrganizationName[0].Name || data.Demographics.OrganizationName[0].OriginalName || data.Demographics.OrganizationName[0].LocalName):'';
			}
			else{
				if(!data.Demographics.PersonName || data.Demographics.PersonName.length == 0){
					name = $filter('translator')('not_available_text','Not Available');
				}
				else{
					var firstName = (data.Demographics.PersonName[0].FormalGivenNameOne ||data.Demographics.PersonName[0].GivenNameOne ||data.Demographics.PersonName[0].OriginalName);
					var familyName = (data.Demographics.PersonName[0].FamilyName || data.Demographics.PersonName[0].OriginalFamilyName);
					name = firstName && familyName ? firstName + " " + familyName : firstName ? firstName : familyName ? familyName : '';
				}
				
			}
			
			if(!name || name.trim().length == 0){
				name = "Not Available";
			}
			
			return $filter('toShortName')(name.trim());
		}
	}
	
	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('saturate', saturate);

	/**
	 * 
	 */
	function saturate(){
		return function(input) {
			if(input == undefined){return "";}

			if($.isNumeric(input) || !(Object.prototype.toString.call(input) == '[object String]')){
				return input;
			}

			/*
			 * FIX: Sonar Issue : Redundant Condition Removal
			 * 	if(input && input != null && typeof input != undefined && input.trim() != ""){
			 */
			if(input && input.trim() != ""){
				return input.trim();
			}
			return "";
		}
	}
	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('na', ['$filter',checkNotAvailable]);

	/**
	 * 
	 */
	function checkNotAvailable ($filter){
		return function(string,asHtml) {
			if(string == undefined || string.trim().length == 0 || string.trim().toLowerCase() == "not available") {
				//var na_text = $filter('translator')('not_available_text','Not Available');
				var na_text = '-NA-'
				if(asHtml == undefined || asHtml == true)
					return "<span class='not-available'>"+na_text+"</span>";
				else
					return na_text;
			}
			return string;
		}
	}

	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('parseExpression', ['$parse', '$interpolate', '$filter', parseExpression]);

	function parseExpression($parse, $interpolate, $filter) {
		return function(expression, scope,bo) {
			if (!expression) {
				return;
			}
			if(bo == undefined){
				return $filter('translator')('not_available_text','Not Available');
			}
			scope.$bo = bo[0];
			if(scope.$bo == undefined || scope.$bo == null || scope.$bo.length == 0){
				return $filter('translator')('not_available_text','Not Available');
			}

			if (expression == true) {
				return true;
			}

			var parsedText = $parse(expression)(scope);

			if (!parsedText || parsedText.trim().length == 0 || parsedText == 'Not Available'){
				return $filter('translator')('not_available_text','Not Available');
			}
			return parsedText;
		};
	};
	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('getEntityIcon', ['$filter','$rootScope',getEntityIcon]);
	function getEntityIcon($filter,$rootScope) {
		return function(string) {
		var data= string.source;
		var genderIcon = '_ _';
		var entity = $filter('getEntityType')(string);
		var icon = {'icon':'_ _',"font":"FontAwesome","xoffset":"21%","yoffset":"19%"};
		if(entity == 'Organization'){
			icon = {'icon':$rootScope.searchresultsTemplate['field_one']['entity']['org']['icon-value'],"font":$rootScope.searchresultsTemplate['field_one']['entity']['org']['icon-font'],"xoffset":"21%","yoffset":"28%"};
		}
		else if(data.Demographics.Person &&  data.Demographics.Person[0].Gender){
			icon = data.Demographics.Person[0].Gender.toLowerCase() == 'female' ? {'icon':$rootScope.searchresultsTemplate['field_one']['entity']['person']['values']['female']['icon-value'],"font":$rootScope.searchresultsTemplate['field_one']['entity']['person']['values']['female']['icon-font'],"xoffset":"25%","yoffset":"30%"}:{'icon':$rootScope.searchresultsTemplate['field_one']['entity']['person']['values']['male']['icon-value'],"font":$rootScope.searchresultsTemplate['field_one']['entity']['person']['values']['male']['icon-font'],"xoffset":"22%","yoffset":"28%"}
		}
		return icon;
		}
	};
	/**
	 * 
	 */
	angular
	.module('customersearch.filters')
	.filter('getTotal', getTotal);
	function getTotal() {
		return function(items) {
			var total = 0;
			angular.forEach(items,function(value,key){
				total += value.count;
			});
			return total;
		}
	};
	
})();

