import random


def unbalancedBranchProbability():
    if random.random() > 0.75:
        print(True)  # 25% probability
    else:
        print(False)  # 75% probability
