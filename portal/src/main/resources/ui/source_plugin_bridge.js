var eb = new EventBus('http://localhost:7529/eventbus');
eb.enableReconnect(true);

var getPortalUuid = findGetParameter("portal_uuid");
var getAppUuid = findGetParameter("app_uuid");
var portalUuid = (getPortalUuid) ? getPortalUuid : null;
var appUuid = (getAppUuid) ? getAppUuid : null;
var traceOrderType = findGetParameter("order_type");
if (traceOrderType) {
    traceOrderType = traceOrderType.toUpperCase();
}

var mainGetQuery = '?portal_uuid=' + portalUuid;

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

function clickedViewAsExternalPortal() {
    eb.send('ClickedViewAsExternalPortal', {
        'portal_uuid': portalUuid
    }, function (error, message) {
        window.open(window.location.href.split('?')[0] + '?portal_uuid=' + message.body.portal_uuid + '&order_type=' + traceOrderType,
            '_blank');
    });
}