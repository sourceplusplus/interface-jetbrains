//todo: merge this method with displayTraceStack
function displayInnerTraces(innerTraceStack) { //todo-chess-equality: [innerTraceStack: List<TraceSpanInfo>]
    portalLog('Displaying inner trace stack: ' + JSON.stringify(innerTraceStack));

    //todo: move all this stuff to setupUI()
    $('#latest_traces_header').removeClass('active');
    $('#span_info_panel').css('display', 'none');
    $('#top_trace_table').css('display', 'none');
    $('#trace_stack_table').css('display', '')
        .css('visibility', 'visible');
    $('#segment_id_span').css('display', 'none');
    $('#trace_stack_span').css('display', 'unset');

    $('#trace_stack_header').removeClass('inactive_tab')
        .addClass('active_sub_tab')
        .css('visibility', 'visible');

    $('#span_info_header').removeClass('active')
        .css('visibility', 'hidden');

    $('#top_trace_table').css('display', 'none');
    $('#trace_stack_table').css('visibility', 'visible');
    $('#traces_span').css('display', 'none');

    $('#latest_traces_header').removeClass('active_sub_tab')
        .addClass('inactive_tab');

    $('#trace_stack_header').addClass('active_sub_tab')
        .removeClass('inactive_tab')
        .css('visibility', 'visible');

    $('#span_info_header').removeClass('active_sub_tab')
        .css('visibility', 'hidden');

    $('#stack_table tr').remove();

    if (innerTraceStack.innerLevel > 0) {
        $('#latest_traces_header_text').text('Parent Stack');
    } else {
        if (traceOrderType === 'LATEST_TRACES') {
            $('#latest_traces_header_text').text('Latest Traces');
        } else if (traceOrderType === 'SLOWEST_TRACES') {
            $('#latest_traces_header_text').text('Slowest Traces');
        } else if (traceOrderType === 'FAILED_TRACES') {
            $('#latest_traces_header_text').text('Failed Traces');
        }
    }

    let traceStack = innerTraceStack.traceStack;
    $('#trace_id_field').val(traceStack[0].span.traceId);
    $('#time_occurred_field').val(moment(Number(traceStack[0].span.startTime)).format());
    $('#traces_span').css('display', 'none');

    for (let i = 0; i < traceStack.length; i++) {
        let spanInfo = traceStack[i];
        let span = spanInfo.span;
        var rowHtml = '<tr><td onclick="clickedDisplaySpanInfo(\'' + spanInfo.appUuid + '\',\'' + spanInfo.rootArtifactQualifiedName
            + '\',\'' + span.traceId + '\',\'' + span.segmentId + '\',' + span.spanId + ');" style="border-top: 0 !important; padding-left: 20px;">';

        if (COMPONENT_MAPPINGS[span.component] || span.component !== "Unknown") {
            let component = COMPONENT_MAPPINGS[span.component];
            if (component == null) {
                component = span.component;
            }
            rowHtml += '<img style="margin-right:5px;vertical-align:bottom" width="18px" height="18px" src="../themes/default/assets/components/' + component.toUpperCase() + '.png"></img>' +
              spanInfo.operationName.replace('<', '&lt;').replace('>', '&gt;');
        } else if (span.hasChildStack || (!externalPortal && span.artifactQualifiedName && i > 0)) {
            rowHtml += '<i style="font-size:1.5em;margin-right:5px;vertical-align:bottom" class="far fa-plus-square"></i>' +
                spanInfo.operationName.replace('<', '&lt;').replace('>', '&gt;');
        } else {
            rowHtml += '<i style="font-size:1.5em;margin-right:5px" class="far fa-info-square"></i>'
            rowHtml += '<span style="vertical-align:top">';
            rowHtml += spanInfo.operationName.replace('<', '&lt;').replace('>', '&gt;');
            rowHtml += '</span>';
        }
        rowHtml += '</td>';

        rowHtml += '<td class="collapsing">' + spanInfo.timeTook + '</td>';
        rowHtml += '<td><div class="ui red progress" id="trace_bar_' + i + '" style="margin: 0">';
        rowHtml += '<div class="bar"></div></div></td>';

        if (span.error) {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="skull crossbones red icon"></i></td></tr>';
        } else if (span.childError && i > 0) {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="exclamation triangle red icon"></i></td></tr>';
        } else {
            rowHtml += '<td class="collapsing" style="padding: 0; text-align: center; color:#808083; font-size: 20px"><i class="check icon"></i></td></tr>';
        }
        $('#stack_table').append(rowHtml);

        $('#trace_bar_' + i).progress({
            percent: spanInfo.totalTracePercent
        })
    }
}

function displaySpanInfo(spanInfo) { //todo-chess-equality: [spanInfo: TraceSpan]
    portalLog('Displaying trace span info: ' + JSON.stringify(spanInfo));

    //todo: move all this stuff to setupUI()
    $('#top_trace_table').css('display', 'none');
    $('#trace_stack_table').css('visibility', 'visible');
    $('#segment_id_span').css('display', 'unset');
    $('#trace_stack_span').css('display', 'none');

    $('#trace_stack_header').css('visibility', 'visible')
        .removeClass('active_sub_tab')
        .addClass('inactive_tab');
    $('#latest_traces_header').removeClass('active_sub_tab')
        .addClass('inactive_tab');

    $('#span_info_header').addClass('active_sub_tab')
        .removeClass('inactive_tab')
        .css('visibility', 'visible');
    $('#trace_stack_table').css('display', 'none')
        .css('visibility', 'hidden');
    $('#span_info_panel').css('display', '')
        .css('visibility', 'visible');

    $('#tag_table tr').remove();
    $('#span_info_start_trace_time').attr("data-value", spanInfo.startTime);
    $('#span_info_start_time').text(moment(spanInfo.startTime).format('h:mm:ss a'));
    $('#span_info_end_trace_time').attr("data-value", spanInfo.endTime);
    $('#span_info_end_time').text(moment(spanInfo.endTime).format('h:mm:ss a'));
    updateOccurredLabels();

    $('#segment_id_field').val(spanInfo.segmentId);

    var gotTags = false;
    for (let key of Object.keys(spanInfo.tags)) {
        gotTags = true;
        let value = spanInfo.tags[key];
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
        if (log.data.event === 'error') {
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