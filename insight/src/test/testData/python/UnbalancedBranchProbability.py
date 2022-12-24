import random


def unbalancedBranchProbability():
    if random.random() > 0.75:
        print(True)  # 75% probability
    else:
        print(False)  # 25% probability
