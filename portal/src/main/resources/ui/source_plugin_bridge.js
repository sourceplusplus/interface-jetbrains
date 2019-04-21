var eb = new EventBus('http://localhost:7529/eventbus');
eb.enableReconnect(true);

var getPluginAvailable = findGetParameter("plugin_available");
var getAppUuid = findGetParameter("app_uuid");
var getSubscribedArtifactQualifiedName = findGetParameter("subscribed_artifact_qualified_name");
var pluginAvailable = (getPluginAvailable) ? getPluginAvailable == 'true' : true;
var appUuid = (getAppUuid) ? getAppUuid : null;
var subscribedArtifactQualifiedName = (getSubscribedArtifactQualifiedName) ? getSubscribedArtifactQualifiedName : null;
var traceOrderType = findGetParameter("order_type");
if (traceOrderType) {
    traceOrderType = traceOrderType.toUpperCase();
}

var mainGetQuery = '?plugin_available=' + getPluginAvailable + '&app_uuid=' + getAppUuid + '&subscribed_artifact_qualified_name=' + getSubscribedArtifactQualifiedName;
if (!getPluginAvailable) {
    mainGetQuery = "?1=1";
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