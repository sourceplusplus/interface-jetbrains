if (hideOverviewTab) {
    $('#overview_link').css('display', 'none');
    $('#sidebar_overview_link').css('display', 'none');
}

function setupUI() {
    if (traceOrderType == 'LATEST_TRACES') {
        $('#latest_traces_header_text').text('Latest Traces');
    } else if (traceOrderType == 'SLOWEST_TRACES') {
        $('#latest_traces_header_text').text('Slowest Traces');
    } else if (traceOrderType == 'FAILED_TRACES') {
        $('#latest_traces_header_text').text('Failed Traces');
    }

    $("input[type='text']").on("click", function () {
        $(this).select();
    });

    window.setInterval(updateOccurredLabels, 2000);
}

setupUI();

var keepTraceCount = (externalPortal) ? 25 : 10;
var displayedTraces = [];
var displayedTraceIds = new Map();

function displayTraces(traceResult) {
    if (traceResult.order_type != traceOrderType) {
        eb.send('PortalLogger', 'Ignoring display traces');
        console.log("Ignoring display traces")
        return
    }

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

    var appUuid = traceResult.app_uuid;
    eb.send('PortalLogger', 'Displaying traces - Size: ' + traceResult.traces.length);
    console.log('Displaying traces - Size: ' + traceResult.traces.length);

    $('#traces_start_field').val(moment.unix(Number(traceResult.start)).format());
    $('#traces_stop_field').val(moment.unix(Number(traceResult.stop)).format());
    $('#traces_total_label').text("Total: " + traceResult.total);

    if (traceResult.traces.length > 0) {
        for (var i = 0; i < traceResult.traces.length; i++) {
            var trace = traceResult.traces[i];
            var globalTraceId = trace.trace_ids[0];
            if (displayedTraceIds.has(globalTraceId)) {
                continue;
            }

            var htmlTraceId = globalTraceId.split('.').join('');
            var operationName = trace.operation_names[0];
            if (operationName == traceResult.artifact_qualified_name) {
                operationName = traceResult.artifact_simple_name;
            }

            var rowHtml = '<tr id="trace-' + htmlTraceId + '"><td onclick=\'clickedDisplayTraceStack("' + appUuid + '","'
                + traceResult.artifact_qualified_name + '","' + globalTraceId +
                '");\' style="border-top: 0 !important; padding-left: 20px">';
            rowHtml += '<i class="large plus square outline icon"></i>'
                + operationName.replace('<', '&lt;').replace('>', '&gt;');
            rowHtml += '</td>';

            var occurred = moment(Number(trace.start));
            var now = moment();
            var timeOccurredDuration = moment.duration(now.diff(occurred));
            rowHtml += '<td class="trace_time collapsing" id="trace_time_' + htmlTraceId + '" data-value="' + trace.start + '" style="text-align: center">'
                + getPrettyDuration(timeOccurredDuration, 1) + '</td>';
            rowHtml += '<td class="collapsing">' + trace.pretty_duration + '</td>';

            if (trace.error) {
                rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="exclamation triangle red icon"></i></td></tr>';
            } else {
                rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; color:#808083; font-size: 20px"><i class="check circle outline icon"></i></td></tr>';
            }

            var insertIndex = displayedTraces.length;
            if (traceResult.order_type == "SLOWEST_TRACES") {
                //sort by duration
                for (var z = 0; z < displayedTraces.length; z++) {
                    if (trace.duration >= displayedTraces[z].duration) {
                        insertIndex = z;
                        break;
                    }
                }
            } else {
                //sort by time
                for (var z = 0; z < displayedTraces.length; z++) {
                    if (trace.start >= displayedTraces[z].start) {
                        insertIndex = z;
                        break;
                    }
                }
            }
            document.getElementById("trace_table").insertRow(insertIndex).outerHTML = rowHtml;
            displayedTraceIds.set(globalTraceId, true);
            displayedTraces.splice(insertIndex, 0, trace);

            if (displayedTraces.length > keepTraceCount) {
                var deleteGlobalTraceId = displayedTraces.pop().trace_ids[0];
                displayedTraceIds.delete(deleteGlobalTraceId);
                $('#trace-' + deleteGlobalTraceId.split('.').join('')).remove();
            }
        }
    }

    $('#traces_captured').text(traceResult.traces.length);
}

function displayInnerTraces(message) {
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

    eb.send('PortalLogger', 'Displaying inner trace stack: ' + JSON.stringify(message));
    console.log('Displaying inner trace stack: ' + JSON.stringify(message));
    $('#stack_table tr').remove();

    viewingInnerTrace = true;
    if (message.body.inner_level > 0) {
        $('#latest_traces_header_text').text('Parent Stack');
    } else {
        if (traceOrderType == 'LATEST_TRACES') {
            $('#latest_traces_header_text').text('Latest Traces');
        } else if (traceOrderType == 'SLOWEST_TRACES') {
            $('#latest_traces_header_text').text('Slowest Traces');
        }
    }

    var traceStack = message.body.trace_stack;
    $('#trace_id_field').val(traceStack[0].span.trace_id);
    $('#time_occurred_field').val(moment(Number(traceStack[0].span.start_time)).format());
    $('#traces_span').css('display', 'none');

    for (var i = 0; i < traceStack.length; i++) {
        var spanInfo = traceStack[i];
        var span = spanInfo.span;
        var rowHtml = '<tr><td onclick="clickedDisplaySpanInfo(\'' + spanInfo.app_uuid + '\',\'' + spanInfo.root_artifact_qualified_name
            + '\',\'' + span.trace_id + '\',\'' + span.segment_id + '\',' + span.span_id + ');" style="border-top: 0 !important; padding-left: 20px">';
        rowHtml += '<i class="large minus square outline icon"></i>' +
            spanInfo.operation_name.replace('<', '&lt;').replace('>', '&gt;');
        rowHtml += '</td>';

        rowHtml += '<td class="collapsing">' + spanInfo.time_took + '</td>';
        rowHtml += '<td><div class="ui red progress" id="trace_bar_' + i + '" style="margin: 0">';
        rowHtml += '<div class="bar"></div></div></td>';

        if (span.error) {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="skull crossbones red icon"></i></td></tr>';
        } else if (span.child_error && i > 0) {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="exclamation triangle red icon"></i></td></tr>';
        } else {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; color:#808083; font-size: 20px"><i class="check circle outline icon"></i></td></tr>';
        }
        $('#stack_table').append(rowHtml);

        $('#trace_bar_' + i).progress({
            percent: spanInfo.total_trace_percent
        })
    }
}

function displaySpanInfo(spanInfo) {
    eb.send('PortalLogger', 'Displaying span info: ' + spanInfo);
    $('#top_trace_table').css('display', 'none');
    $('#trace_stack_table').css('visibility', 'visible');
    $('#segment_id_span').css('display', 'unset');
    $('#trace_stack_span').css('display', 'none');

    $('#trace_stack_header').css('visibility', 'visible');
    $('#trace_stack_header').removeClass('active_sub_tab');
    $('#latest_traces_header').removeClass('active_sub_tab');
    $('#trace_stack_header').addClass('inactive_tab');
    $('#latest_traces_header').addClass('inactive_tab');

    $('#span_info_header').addClass('active_sub_tab');
    $('#span_info_header').removeClass('inactive_tab');
    $('#span_info_header').css('visibility', 'visible');
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

function displayTraceStack(traceStack) {
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

    eb.send('PortalLogger', 'Displaying trace stack: ' + traceStack);
    $('#stack_table tr').remove();

    viewingInnerTrace = false;
    if (traceOrderType == 'LATEST_TRACES') {
        $('#latest_traces_header_text').text('Latest Traces');
    } else if (traceOrderType == 'SLOWEST_TRACES') {
        $('#latest_traces_header_text').text('Slowest Traces');
    }

    $('#trace_id_field').val(traceStack[0].span.trace_id);
    $('#time_occurred_field').val(moment(Number(traceStack[0].span.start_time)).format());

    for (var i = 0; i < traceStack.length; i++) {
        var spanInfo = traceStack[i];
        var span = spanInfo.span;
        var rowHtml = '<tr><td onclick="clickedDisplaySpanInfo(\'' + spanInfo.app_uuid + '\',\'' + spanInfo.root_artifact_qualified_name
            + '\',\'' + span.trace_id + '\',\'' + span.segment_id + '\',' + span.span_id + ');" style="border-top: 0 !important; padding-left: 20px">';
        rowHtml += '<i class="large minus square outline icon"></i>' +
            spanInfo.operation_name.replace('<', '&lt;').replace('>', '&gt;');
        rowHtml += '</td>';

        rowHtml += '<td class="collapsing">' + spanInfo.time_took + '</td>';
        rowHtml += '<td><div class="ui red progress" id="trace_bar_' + i + '" style="margin: 0">';
        rowHtml += '<div class="bar"></div></div></td>';

        if (span.error) {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="skull crossbones red icon"></i></td></tr>';
        } else if (span.child_error && i > 0) {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="exclamation triangle red icon"></i></td></tr>';
        } else {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; color:#808083; font-size: 20px"><i class="check circle outline icon"></i></td></tr>';
        }
        $('#stack_table').append(rowHtml);

        $('#trace_bar_' + i).progress({
            percent: spanInfo.total_trace_percent
        })
    }
}

function updateOccurredLabels() {
    $('.trace_time').each(function (i, traceTime) {
        if (traceTime.dataset["value"]) {
            var occurred = moment(Number(traceTime.dataset["value"]));
            var now = moment();
            var timeOccurredDuration = moment.duration(now.diff(occurred));
            traceTime.innerText = getPrettyDuration(timeOccurredDuration, 1);
        }
    });
}

function getPrettyDuration(duration, decimalPlaces) {
    var prettyDuration = null;
    var postText = null;
    if (duration.months() > 0) {
        var months = duration.weeks();
        duration = duration.subtract(months, 'months');
        prettyDuration = months + "mo " + (Math.round(duration.asWeeks() * 10) / 10).toFixed(decimalPlaces);
        postText = "w ago";
    } else if (duration.weeks() > 0) {
        var weeks = duration.weeks();
        duration = duration.subtract(weeks, 'weeks');
        prettyDuration = weeks + "w " + (Math.round(duration.asDays() * 10) / 10).toFixed(decimalPlaces);
        postText = "d ago";
    } else if (duration.days() > 0) {
        var days = duration.hours();
        duration = duration.subtract(days, 'days');
        prettyDuration = days + "d " + (Math.round(duration.asHours() * 10) / 10).toFixed(decimalPlaces);
        postText = "h ago";
    } else if (duration.hours() > 0) {
        var hours = duration.hours();
        duration = duration.subtract(hours, 'hours');
        prettyDuration = hours + "h " + (Math.round(duration.asMinutes() * 10) / 10).toFixed(decimalPlaces);
        postText = "m ago";
    } else if (duration.minutes() > 0) {
        var minutes = duration.minutes();
        duration = duration.subtract(minutes, 'minutes');
        prettyDuration = minutes + "m " + (Math.round(duration.asSeconds() * 10) / 10).toFixed(decimalPlaces);
        postText = "s ago";
    } else {
        prettyDuration = (Math.round(duration.asSeconds() * 10) / 10).toFixed(decimalPlaces);
        postText = "s ago";
    }

    if (prettyDuration.endsWith(".0")) {
        prettyDuration = prettyDuration.substr(0, prettyDuration.length - 2)
    }
    return prettyDuration + postText;
}