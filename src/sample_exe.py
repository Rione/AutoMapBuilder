import os, sys

sys.path.append(os.getcwd().replace('/src', ''))

from src import ga_main, greedy_main, two_opt_main

if __name__ == '__main__':
    sample = []
    for i in range(50):
        print(i)
        sample.append(ga_main.main()[0])
    with open(os.getcwd() + '/result', mode='w') as f:
        for s in sample:
            f.write(str(s) + '\n')
