def literalPass():
    doSleep(True)


def doSleep(sleep):
    if sleep:
        print(False)  # sleep 200ms


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

    print(False)  # sleep 200ms
