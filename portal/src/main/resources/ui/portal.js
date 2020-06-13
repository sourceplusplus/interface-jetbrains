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
});
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