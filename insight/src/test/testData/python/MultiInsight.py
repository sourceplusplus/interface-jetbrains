import random


def probabilityAndDuration():
    if random.random() > 0.5:  # sleep 100ms
        if random.random() > 0.5:  # sleep 100ms
            print(False)  # sleep 200ms

    else:
        print(True)  # sleep 200ms
