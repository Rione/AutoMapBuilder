import os, sys

sys.path.append(os.getcwd().replace('/src', ''))

from src import ga_main, greedy_main, two_opt_main

if __name__ == '__main__':
    print(greedy_main.main())
