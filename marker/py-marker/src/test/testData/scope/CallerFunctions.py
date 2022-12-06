def callerFunction():
    directCalledFunction()


def directCalledFunction():
    indirectCalledFunction()


def indirectCalledFunction():
    print(True)
