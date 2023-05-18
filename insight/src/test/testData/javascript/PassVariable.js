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

function literalPass5() {
    doSleep5(false)
}

function doSleep5(sleep) {
    if (sleep) {
        Thread.sleep(400);
    }
    Thread.sleep(200);
}

function literalPass6() {
    doSleep(false);
}

function literalPass7() {
    literalPass7_1(false);
}

function literalPass7_1(sleep) {
    literalPass7_2(sleep);
}

function literalPass7_2(sleep) {
    if (sleep) {
        Thread.sleep(100);
        Thread.sleep(100);
    }
    Thread.sleep(100);
}
