def literalPass():
    doSleep(True)


def doSleep(sleep):
    if sleep:
        Thread.sleep(200)


def literalPass2():
    doSleep2(True)


def doSleep2(sleep):
    doSleep(sleep)


def literalPass3():
    literalPass2()


def literalPass4():
    doSleep4(False)


def doSleep4(sleep):
    if sleep:
        pass

    Thread.sleep(200)


def literalPass5():
    doSleep5(False)


def doSleep5(sleep):
    if sleep:
        Thread.sleep(400)

    Thread.sleep(200)


def literalPass6():
    doSleep(False)


def literalPass7():
    literalPass7_1(False)


def literalPass7_1(sleep):
    literalPass7_2(sleep)


def literalPass7_2(sleep):
    if sleep:
        Thread.sleep(100)
        Thread.sleep(100)

    Thread.sleep(100)
