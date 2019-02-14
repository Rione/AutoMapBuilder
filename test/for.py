if __name__ == '__main__':
    set = {}
    set.setdefault(1, 'a')
    set.setdefault(2, 'b')

    for s in set:
        if 1 == set.get(s):
            break

    for n in set:
        print(n)
