$(document).ready(function () {
    $('.ui.dropdown').dropdown();
    $('.ui.sidebar').sidebar('setting', 'transition', 'overlay');

    let latestTracesHeader = $('#latest_traces_header');
    if (latestTracesHeader.length) {
        latestTracesHeader.dropdown({on: null});
    }
    let traceStackHeader = $('#trace_stack_header');
    if (traceStackHeader.length) {
        traceStackHeader.dropdown({on: 'hover'});
    }

    $(".openbtn").on("click", function () {
        $(".ui.sidebar").toggleClass("very thin icon");
        $(".asd").toggleClass("marginlefting");
        $(".sidebar z").toggleClass("displaynone");
        $(".ui.accordion").toggleClass("displaynone");
        $(".ui.dropdown.item").toggleClass("displaynone");
        $(".hide_on_toggle").toggleClass("displaynone");
        $(".pusher").toggleClass("dimmed");

        $(".logo").find('img').toggle();
    });
    $('.ui.accordion').accordion({
        selector: {}
    });

    $('#overview_link').attr('href', "overview.html" + mainGetQuery);
    $('#sidebar_overview_link').attr('href', "overview.html" + mainGetQuery);

    $('#traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
    $('#traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');
    $('#traces_link_failed').attr('href', "traces.html" + mainGetQuery + '&order_type=failed_traces');
    $('#sidebar_traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
    $('#sidebar_traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');
    $('#sidebar_traces_link_failed').attr('href', "traces.html" + mainGetQuery + '&order_type=failed_traces');

    $('#configuration_link').attr('href', "configuration.html" + mainGetQuery);
    $('#sidebar_configuration_link').attr('href', "configuration.html" + mainGetQuery);
});

function loadCSS(filename) {
    let linkRef = document.createElement("link");
    linkRef.setAttribute("rel", "stylesheet");
    linkRef.setAttribute("type", "text/css");
    linkRef.setAttribute("href", filename);
    document.getElementsByTagName("head")[0].appendChild(linkRef);
}

function findGetParameter(parameterName) {
    var result = null,
        tmp = [];
    location.search
        .substr(1)
        .split("&")
        .forEach(function (item) {
            tmp = item.split("=");
            if (tmp[0] === parameterName) result = decodeURIComponent(tmp[1]);
        });
    return result;
}

var getDarkMode = findGetParameter("dark_mode");
var darkMode = (getDarkMode) ? (getDarkMode == 'true') : false;
if (darkMode) {
    loadCSS("css/dark_style.css");
} else {
    loadCSS("css/style.css");
}