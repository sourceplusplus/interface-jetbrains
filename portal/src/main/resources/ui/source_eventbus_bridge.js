var eb = new EventBus('http://localhost:7529/eventbus');
eb.enableReconnect(true);

var getPortalUuid = findGetParameter("portal_uuid");
var portalUuid = (getPortalUuid) ? getPortalUuid : null;
var getRequiresRegistration = findGetParameter("requires_registration");
var requiresRegistration = (getRequiresRegistration) ? getRequiresRegistration : false;
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
        if (traceOrderType) {
            window.open(window.location.href.split('?')[0] + '?portal_uuid=' + message.body.portal_uuid + '&order_type=' + traceOrderType,
                '_blank');
        } else {
            window.open(window.location.href.split('?')[0] + '?portal_uuid=' + message.body.portal_uuid, '_blank');
        }
    });
}

function portalConnected() {
    console.log("Source++ Portal successfully connected to eventbus bridge");
    if (requiresRegistration) {
        eb.send("REGISTER_PORTAL", {
            'app_uuid': findGetParameter("app_uuid"),
            'artifact_qualified_name': findGetParameter("artifact_qualified_name")
        }, function (error, message) {
            if (traceOrderType) {
                window.open(window.location.href.split('?')[0] + '?portal_uuid=' + message.body.portal_uuid + '&order_type=' + traceOrderType,
                    '_self');
            } else {
                window.open(window.location.href.split('?')[0] + '?portal_uuid=' + message.body.portal_uuid, '_self');
            }
        });
    } else {
        window.setInterval(keepPortalAlive, 60000 * 4);
    }
}

function keepPortalAlive() {
    eb.send('KeepAlivePortal', {'portal_uuid': portalUuid});
    console.log("Sent portal keep alive request");
}