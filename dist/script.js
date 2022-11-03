var treeRootID = '#deftreeRoot'
var treeSymbolInfoID = '#treeSymbolInfo'

var treeNodeTypeClass = 'treeNodeType'
var treeNodeDescClass = 'treeNodeDesc'
var treeSymbolClass = 'treeSymbol'

var searchCallback = function(str, node) {
    if (str.length < 2) {
	$.jstree.reference(treeRootID).clear_search();
	return;
    }
    var search = str.slice(1).toLowerCase();
    var match = {'length': 0};
    if (str[0] == 's') {
	match = $(node.text).filter('.' + treeSymbolClass);
    } else if (str[0] == 'n') {
	match = $(node.text).filter('.' + treeNodeTypeClass);
    }
    if (match.length > 0) {
	return match[0]
	    .textContent
	    .toLowerCase()
	    .includes(search);
    }
};

$(treeRootID)
    .on('click', function (event) {
	event.stopPropagation();
    })
    .on('changed.jstree', function (e, action) {
	$(treeSymbolInfoID).html("");
	action.selected.forEach(function (id) {
	    var selectedSymbol = $('#' + id);
	    if (selectedSymbol.attr('tv-fullName')) {
		$.get('/symbolInfo/' + selectedSymbol.attr('tv-fullName'), function (data) {
		    $(treeSymbolInfoID).append(data);
		});
	    }
	});
    })
    .jstree({
	'core': {
	  'animation': false,
	  'themes': {
	    'variant': 'large',
	    'icons': false
	  }
	},
	'search': {
	    'search_callback': searchCallback
	},
	'plugins': ['search']
    });

$.jstree.reference(treeRootID)
    .open_node($(treeRootID + ' li').first());

$('body').on('click', function (event) {
    $.jstree.reference(treeRootID).deselect_all();
});

$('#expandAll').on('click', function (event) {
    $.jstree.reference(treeRootID).open_all();
});

$('#collapseAll').on('click', function (event) {
    $.jstree.reference(treeRootID).close_all();
});

var to = false;
$('#searchSymbols').keyup(function () {
    if(to) { clearTimeout(to); }
    to = setTimeout(function () {
      var v = $('#searchSymbols').val();
      $.jstree.reference(treeRootID).search('s' + v);
    }, 250);
});
$('#searchNodes').keyup(function () {
    if(to) { clearTimeout(to); }
    to = setTimeout(function () {
      var v = $('#searchNodes').val();
      $.jstree.reference(treeRootID).search('n' + v);
    }, 250);
});
