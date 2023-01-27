def simplifyBranch():
    if True:
        print(True)  # sleep 100ms

    print(False)  # sleep 100ms


def simplifyBranch2():
    print(True)  # sleep 100ms
    if True:
        print(False)  # sleep 100ms

    print(True)  # sleep 100ms
