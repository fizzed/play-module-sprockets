var App = function () {
	return {
		handleIEFixes: function () {
	        //fix html5 placeholder attribute for ie7 & ie8
			if (jQuery.browser.msie && jQuery.browser.version.substr(0, 1) <= 9) { // ie7, ie8, ie9
				jQuery('input[placeholder], textarea[placeholder]').each(function () {
	                var input = jQuery(this);
	
	                jQuery(input).val(input.attr('placeholder'));
	
	                jQuery(input).focus(function () {
	                    if (input.val() == input.attr('placeholder')) {
	                        input.val('');
	                    }
	                });
	
	                jQuery(input).blur(function () {
	                    if (input.val() == '' || input.val() == input.attr('placeholder')) {
	                        input.val(input.attr('placeholder'));
	                    }
	                });
	            });
	        }
	    }
	};
}();
