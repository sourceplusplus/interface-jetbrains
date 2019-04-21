$('#overview_link').attr('href', "overview.html" + mainGetQuery);
$('#sidebar_overview_link').attr('href', "overview.html" + mainGetQuery);

$('#traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');
$('#sidebar_traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#sidebar_traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');

var timeFrame = localStorage.getItem('spp.metric_time_frame');
if (timeFrame) {
    if (timeFrame === 'last_minute') {
        $('#current_metric_time_frame').text('LAST MINUTE');
    } else if (timeFrame === 'last_hour') {
        $('#current_metric_time_frame').text('LAST HOUR');
    } else if (timeFrame === 'last_day') {
        $('#current_metric_time_frame').text('LAST DAY');
    } else if (timeFrame === 'last_month') {
        $('#current_metric_time_frame').text('LAST MONTH');
    }
}

var viewingInnerTrace = false;
eb.onopen = function () {
    //remind plugin of the current metric time frame
    var timeFrame = localStorage.getItem('spp.metric_time_frame');
    if (timeFrame) {
        updateTime(timeFrame);
    }

    var displayTracesHandler = 'DisplayTraces';
    if (!pluginAvailable) {
        displayTracesHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + displayTracesHandler;
    }
    eb.registerHandler(displayTracesHandler, function (error, message) {
        var traceResult = message.body;
        if (traceResult.order_type != traceOrderType) {
            // console.log("Ignoring card for time frame: " + card.time_frame
            //     + " - Current time frame: " + currentTimeFrame);
            return
        }

        var appUuid = traceResult.app_uuid;
        eb.send('TooltipLogger', 'Displaying traces - Size: ' + traceResult.traces.length);
        // console.log('Displaying traces - Size: ' + traceResult.traces.length);
        $('#trace_table tr').remove();

        $('#traces_start_field').val(moment.unix(Number(traceResult.start)).format());
        $('#traces_stop_field').val(moment.unix(Number(traceResult.stop)).format());
        $('#traces_total_label').text("Total: " + traceResult.total);

        if (traceResult.traces.length > 0) {
            for (var i = traceResult.traces.length - 1; i >= 0; i--) {
                var trace = traceResult.traces[i];
                var globalTraceId = trace.trace_ids[0];
                var htmlTraceId = globalTraceId.split('.').join('');
                var operationName = trace.operation_names[0];
                if (operationName == traceResult.artifact_qualified_name) {
                    operationName = traceResult.artifact_simple_name;
                }

                var rowHtml = '<tr><td onclick=\'clickedDisplayTraceStack("' + appUuid + '","'
                    + traceResult.artifact_qualified_name + '","' + globalTraceId +
                    '");\' style="border-top: 0px !important; padding-left: 20px">';
                rowHtml += '<i class="large plus square outline icon"></i>'
                    + operationName.replace('<', '&lt;').replace('>', '&gt;');
                rowHtml += '</td>';

                var occurred = moment(Number(trace.start));
                var now = moment();
                var timeOccurredDuration = moment.duration(now.diff(occurred));
                rowHtml += '<td class="trace_time collapsing" id="trace_time_' + htmlTraceId + '" data-value="' + trace.start + '" style="text-align: center">'
                    + getPrettyDuration(timeOccurredDuration) + '</td>';
                rowHtml += '<td class="collapsing">' + trace.pretty_duration + '</td>';

                if (trace.error) {
                    rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="bug red icon"></i></td></tr>';
                } else {
                    rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; color:#808083; font-size: 20px"><i class="check circle outline icon"></i></td></tr>';
                }
                $('#trace_table').append(rowHtml);
            }
        }

        $('#traces_captured').text(traceResult.traces.length);
    });

    eb.registerHandler('DisplayInnerTraceStack', function (error, message) {
        eb.send('TooltipLogger', 'Displaying inner trace stack: ' + JSON.stringify(message));
        console.log('Displaying inner trace stack: ' + JSON.stringify(message));
        goBackToTraceStack(false);
        $('#stack_table tr').remove();

        viewingInnerTrace = true;
        if (message.body.inner_level > 0) {
            $('#latest_traces_header_text').text('Parent Stack');
        } else {
            $('#latest_traces_header_text').text('Latest Traces');
        }

        var traceStack = message.body.trace_stack;
        $('#trace_id_field').val(traceStack[0].span.trace_id);
        $('#time_occurred_field').val(moment(Number(traceStack[0].span.start_time)).format());
        $('#traces_span').css('display', 'none');

        for (var i = 0; i < traceStack.length; i++) {
            var spanInfo = traceStack[i];
            var span = spanInfo.span;
            var rowHtml = '<tr><td onclick="clickedDisplaySpanInfo(\'' + spanInfo.app_uuid + '\',\'' + spanInfo.root_artifact_qualified_name
                + '\',\'' + span.trace_id + '\',\'' + span.segment_id + '\',' + span.span_id + ');" style="border-top: 0px !important; padding-left: 20px">';
            rowHtml += '<i class="large minus square outline icon"></i>' +
                spanInfo.operation_name.replace('<', '&lt;').replace('>', '&gt;');
            rowHtml += '</td>';

            rowHtml += '<td class="collapsing">' + spanInfo.time_took + '</td>';
            rowHtml += '<td><div class="ui red progress" id="trace_bar_' + i + '" style="margin: 0">';
            rowHtml += '<div class="bar"></div></div></td></tr>';
            $('#stack_table').append(rowHtml);

            $('#trace_bar_' + i).progress({
                percent: spanInfo.total_trace_percent
            })
        }
    });

    eb.registerHandler('ClearTraceStack', function (error, message) {
        goBackToLatestTraces(false);
    });

    eb.registerHandler('ClearSpanInfo', function (error, message) {
        goBackToTraceStack(false);
    });

    eb.send('TracesTabOpened', {});
};

function clickedDisplaySpanInfo(appUuid, rootArtifactQualifiedName, traceId, segmentId, spanId) {
    eb.send('ClickedDisplaySpanInfo', {
        'app_uuid': appUuid, 'artifact_qualified_name': rootArtifactQualifiedName,
        'trace_id': traceId, 'segment_id': segmentId, 'span_id': spanId
    }, function (error, message) {
        eb.send('TooltipLogger', 'Displaying trace span info: ' + JSON.stringify(message));
        console.log('Displaying trace span info: ' + JSON.stringify(message));
        var spanInfo = message.body;
        displaySpanInfo(spanInfo);
    });
    $('#top_trace_table').css('display', 'none');
    $('#trace_stack_table').css('visibility', 'visible');
    $('#segment_id_span').css('display', 'unset');
    $('#trace_stack_span').css('display', 'none');

    $('#trace_stack_header').removeClass('active_sub_tab');
    $('#latest_traces_header').removeClass('active_sub_tab');
    $('#trace_stack_header').addClass('inactive_tab');
    $('#latest_traces_header').addClass('inactive_tab');

    $('#span_info_header').addClass('active_sub_tab');
    $('#span_info_header').removeClass('inactive_tab');
    $('#span_info_header').css('visibility', 'visible');
}

function displaySpanInfo(spanInfo) {
    eb.send('TooltipLogger', 'Displaying span info: ' + spanInfo);
    $('#trace_stack_table').css('display', 'none');
    $('#trace_stack_table').css('visibility', 'hidden');
    $('#span_info_panel').css('display', '');
    $('#span_info_panel').css('visibility', 'visible');

    $('#tag_table tr').remove();
    $('#span_info_start_trace_time').attr("data-value", spanInfo.start_time);
    $('#span_info_start_time').text(moment(spanInfo.start_time).format('h:mm:ss a'));
    $('#span_info_end_trace_time').attr("data-value", spanInfo.end_time);
    $('#span_info_end_time').text(moment(spanInfo.end_time).format('h:mm:ss a'));
    updateOccurredLabels();

    $('#segment_id_field').val(spanInfo.segment_id);

    var gotTags = false;
    for (let key of Object.keys(spanInfo.tags)) {
        gotTags = true;
        var value = spanInfo.tags[key];
        if (value !== '') {
            var rowHtml = '<tr>';
            rowHtml += '<td>' + key + '</td>';
            rowHtml += '<td>' + value + '</td>';
            rowHtml += '</tr>';
            $('#tag_table').append(rowHtml);
        }
    }
    if (gotTags) {
        $("#span_tag_div").removeClass("displaynone");
    } else {
        $("#span_tag_div").addClass("displaynone");
    }

    var gotLogs = false;
    $('#log_table').empty();
    spanInfo.logs.forEach(function (log) {
        gotLogs = true;
        var rowHtml = '<tr><td style="white-space: nowrap">';
        rowHtml += '<b>' + moment.unix(Number(log.time)).format() + '</b><br>';
        if (log.data.event == 'error') {
            rowHtml += log.data.stack.replace(/(?:\r\n|\r|\n)/g, '<br>');
        } else {
            rowHtml += log.data;
        }
        rowHtml += '</td></tr>';
        $('#log_table').append(rowHtml);
    });
    if (gotLogs) {
        $("#span_log_div").removeClass("displaynone");
    } else {
        $("#span_log_div").addClass("displaynone");
    }
}

function clickedDisplayTraceStack(appUuid, artifactQualifiedName, globalTraceId) {
    eb.send('ClickedDisplayTraceStack', {
        'app_uuid': appUuid,
        'artifact_qualified_name': artifactQualifiedName,
        'trace_id': globalTraceId
    }, function (error, message) {
        eb.send('TooltipLogger', 'Displaying trace stack: ' + JSON.stringify(message));
        console.log('Displaying trace stack: ' + JSON.stringify(message));
        var traceStack = message.body;
        displayTraceStack(traceStack);
    });
    $('#top_trace_table').css('display', 'none');
    $('#trace_stack_table').css('visibility', 'visible');
    $('#traces_span').css('display', 'none');

    $('#latest_traces_header').removeClass('active_sub_tab');
    $('#latest_traces_header').addClass('inactive_tab');

    $('#trace_stack_header').addClass('active_sub_tab');
    $('#trace_stack_header').removeClass('inactive_tab');
    $('#trace_stack_header').css('visibility', 'visible');

    $('#span_info_header').removeClass('active_sub_tab');
    $('#span_info_header').css('visibility', 'hidden');
}

function displayTraceStack(traceStack) {
    eb.send('TooltipLogger', 'Displaying trace stack: ' + traceStack);
    goBackToTraceStack(false);
    $('#stack_table tr').remove();

    viewingInnerTrace = false;
    $('#latest_traces_header_text').text('Latest Traces');

    $('#trace_id_field').val(traceStack[0].span.trace_id);
    $('#time_occurred_field').val(moment(Number(traceStack[0].span.start_time)).format());

    for (var i = 0; i < traceStack.length; i++) {
        var spanInfo = traceStack[i];
        var span = spanInfo.span;
        var rowHtml = '<tr><td onclick="clickedDisplaySpanInfo(\'' + spanInfo.app_uuid + '\',\'' + spanInfo.root_artifact_qualified_name
            + '\',\'' + span.trace_id + '\',\'' + span.segment_id + '\',' + span.span_id + ');" style="border-top: 0px !important; padding-left: 20px">';
        rowHtml += '<i class="large minus square outline icon"></i>' +
            spanInfo.operation_name.replace('<', '&lt;').replace('>', '&gt;');
        rowHtml += '</td>';

        rowHtml += '<td class="collapsing">' + spanInfo.time_took + '</td>';
        rowHtml += '<td><div class="ui red progress" id="trace_bar_' + i + '" style="margin: 0">';
        rowHtml += '<div class="bar"></div></div></td></tr>';
        $('#stack_table').append(rowHtml);

        $('#trace_bar_' + i).progress({
            percent: spanInfo.total_trace_percent
        })
    }
}

function goBackToLatestTraces(userClicked) {
    $('#span_info_panel').css('display', 'none');
    $('#latest_traces_header').addClass('active_sub_tab');
    $('#latest_traces_header').removeClass('inactive_tab');
    $('#top_trace_table').css('display', '');
    $('#trace_stack_table').css('visibility', 'hidden');
    $('#traces_span').css('display', 'unset');
    $('#trace_stack_span').css('display', 'none');
    $('#segment_id_span').css('display', 'none');

    $('#trace_stack_header').addClass('inactive_tab');
    $('#trace_stack_header').removeClass('active_sub_tab');
    $('#trace_stack_header').css('visibility', 'hidden');

    $('#span_info_header').addClass('inactive_tab');
    $('#span_info_header').removeClass('active_sub_tab');
    $('#span_info_header').css('visibility', 'hidden');

    if (userClicked && viewingInnerTrace) {
        viewingInnerTrace = false;
        eb.send('ClickedGoBackToLatestTraces', {});
    }
}

function goBackToTraceStack(userClicked) {
    $('#latest_traces_header').removeClass('active');
    $('#span_info_panel').css('display', 'none');
    $('#top_trace_table').css('display', 'none');
    $('#trace_stack_table').css('display', '');
    $('#trace_stack_table').css('visibility', 'visible');
    $('#segment_id_span').css('display', 'none');
    $('#trace_stack_span').css('display', 'unset');

    $('#trace_stack_header').removeClass('inactive_tab');
    $('#trace_stack_header').addClass('active_sub_tab');
    $('#trace_stack_header').css('visibility', 'visible');

    $('#span_info_header').removeClass('active');
    $('#span_info_header').css('visibility', 'hidden');
}

function updateTime(interval) {
    localStorage.setItem('spp.metric_time_frame', interval);
    eb.send('SetMetricTimeFrame', {value: interval});
}

function getPrettyDuration(duration) {
    if (duration.months() > 0) {
        var months = duration.weeks();
        duration = duration.subtract(months, 'months');
        return months + "mo " + (Math.round(duration.asWeeks() * 10) / 10).toFixed(2) + "w ago"
    } else if (duration.weeks() > 0) {
        var weeks = duration.weeks();
        duration = duration.subtract(weeks, 'weeks');
        return weeks + "w " + (Math.round(duration.asDays() * 10) / 10).toFixed(2) + "d ago"
    } else if (duration.days() > 0) {
        var days = duration.hours();
        duration = duration.subtract(days, 'days');
        return days + "d " + (Math.round(duration.asHours() * 10) / 10).toFixed(2) + "h ago"
    } else if (duration.hours() > 0) {
        var hours = duration.hours();
        duration = duration.subtract(hours, 'hours');
        return hours + "h " + (Math.round(duration.asMinutes() * 10) / 10).toFixed(2) + "m ago"
    } else if (duration.minutes() > 0) {
        var minutes = duration.minutes();
        duration = duration.subtract(minutes, 'minutes');
        return minutes + "m " + (Math.round(duration.asSeconds() * 10) / 10).toFixed(2) + "s ago"
    } else {
        return (Math.round(duration.asSeconds() * 10) / 10).toFixed(2) + "s ago"
    }
}

$("input[type='text']").on("click", function () {
    $(this).select();
});

function updateOccurredLabels() {
    $('.trace_time').each(function (i, traceTime) {
        if (traceTime.dataset["value"]) {
            var occurred = moment(Number(traceTime.dataset["value"]));
            var now = moment();
            var timeOccurredDuration = moment.duration(now.diff(occurred));
            traceTime.innerText = getPrettyDuration(timeOccurredDuration);
        }
    });
}

window.setInterval(updateOccurredLabels, 2000);