//var eb = new EventBus('http://localhost:8888/eventbus');
//eb.enableReconnect(true); //todo: more logic behind this
//todo: it is likely that a disconnect signifies the portal is gone
//todo: so on reconnect there will need to be another portal uuid registered

var getPortalUuid = findGetParameter("portalUuid");
var portalUuid = (getPortalUuid) ? getPortalUuid : null;
var getRequiresRegistration = findGetParameter("requires_registration");
var requiresRegistration = (getRequiresRegistration) ? getRequiresRegistration : false;
var traceOrderType = findGetParameter("order_type");
if (traceOrderType) {
    traceOrderType = traceOrderType.toUpperCase();
}
var getExternal = findGetParameter("external");
var externalPortal = (getExternal) ? (getExternal === 'true') : false;
var getDarkMode = findGetParameter("dark_mode");
var darkMode = (getDarkMode) ? (getDarkMode === 'true') : false;
var getHideActivityTab = findGetParameter("hide_activity_tab");
var hideActivityTab = (getHideActivityTab) ? (getHideActivityTab === 'true') : false;

var mainGetQuery = '?portalUuid=' + portalUuid;
var mainGetQueryWithoutPortalUuid = "";
if (externalPortal) {
    mainGetQueryWithoutPortalUuid += '&external=true';
}
if (darkMode) {
    mainGetQueryWithoutPortalUuid += '&dark_mode=true';
}
if (hideActivityTab) {
    mainGetQueryWithoutPortalUuid += '&hide_activity_tab=true';
}
mainGetQuery += mainGetQueryWithoutPortalUuid;

function findGetParameter(parameterName) {
    let result = null, tmp = [];
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
        'portalUuid': portalUuid
    }, function (error, message) {
        window.open(window.location.href.split('?')[0] + '?portalUuid=' + message.body.portalUuid
            + '&external=true' + mainGetQueryWithoutPortalUuid, '_blank');
    });
}

function portalConnected() {
    console.log("Portal successfully connected. Portal UUID: " + portalUuid);
    if (requiresRegistration) {
        eb.send("REGISTER_PORTAL", {
            'appUuid': findGetParameter("appUuid"),
            'artifactQualifiedName': findGetParameter("artifactQualifiedName")
        }, function (error, message) {
            window.open(window.location.href.split('?')[0] + '?portalUuid=' + message.body.portalUuid
                + mainGetQueryWithoutPortalUuid, '_self');
        });
    } else if (externalPortal) {
        let keepAliveInterval = window.setInterval(function () {
            portalLog("Sent portal keep alive request. Portal UUID: " + portalUuid);
            eb.send('KeepAlivePortal', {'portalUuid': portalUuid}, function (error, message) {
                if (error) {
                    clearInterval(keepAliveInterval);
                }
            });
        }, 30000);
    }
}

function portalLog(message) {
    console.log(message);
    eb.send('PortalLogger', message);
}