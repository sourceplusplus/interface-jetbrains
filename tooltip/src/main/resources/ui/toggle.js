var isToggled = false;
var toggleParam = localStorage.getItem('spp.toggle');
if (toggleParam !== null) {
    isToggled = toggleParam === 'true';

    if (isToggled) {
        $('#sidebar').removeClass('thin');
        $('#sidebar').addClass('very thin');
        $('#icon_toggle').removeClass('long arrow left icon');
        $('#icon_toggle').addClass('sidebar icon');
        $('#main-grid').css('width', '665px');
        $('.card').css('width', '30%');
        $('.mini-graph-bars').css('margin-left', '1.5em');
        $('#traces_total_label').css('display', 'unset');
        localStorage.setItem('spp.toggle', 'true');
    } else {
        $('#traces_total_label').css('display', 'none');
    }
}
$('#navigation-toggle').click(function () {
    if (isToggled) {
        $('#sidebar').removeClass('very');
        $('#icon_toggle').removeClass('sidebar icon');
        $('#icon_toggle').addClass('long arrow left icon');
        $('#main-grid').css('width', '575px');
        $('.card').css('width', '');
        $('.mini-graph-bars').css('margin-left', '.7em');
        $('#traces_total_label').css('display', 'none');
        localStorage.setItem('spp.toggle', 'false');
    } else {
        $('#sidebar').removeClass('thin');
        $('#sidebar').addClass('very thin');
        $('#icon_toggle').removeClass('long arrow left icon');
        $('#icon_toggle').addClass('sidebar icon');
        $('#main-grid').css('width', '665px');
        $('.card').css('width', '30%');
        $('.mini-graph-bars').css('margin-left', '1.5em');
        $('#traces_total_label').css('display', 'unset');
        localStorage.setItem('spp.toggle', 'true');
    }
    isToggled = !isToggled
});