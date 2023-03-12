function literalPass() {
    doSleep(true);
}

function doSleep(sleep) {
    if (sleep) {
        Thread.sleep(200);
    }
}

function literalPass2() {
    doSleep2(true);
}

function doSleep2(sleep) {
    doSleep(sleep);
}

function literalPass3() {
    literalPass2();
}

function literalPass4() {
    doSleep4(false);
}

function doSleep4(sleep) {
    if (sleep) {
    }
    Thread.sleep(200);
}

function literalPass6() {
    doSleep(false);
}
