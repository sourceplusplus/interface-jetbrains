def literalPass():
    doSleep(True)


def doSleep(sleep):
    if sleep:
        print(False)  # sleep 200ms


def literalPass2():
    doSleep2(True)


def doSleep2(sleep):
    doSleep(sleep)
