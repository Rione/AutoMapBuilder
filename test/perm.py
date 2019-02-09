def perm(m):
    result = []

    def perm_sub(n, a):
        if n == m:
            result.append(a.copy())
        else:
            for x in range(2, m + 1):
                if x not in a:
                    if n != 2 or a[0] > x:
                        a.append(x)
                        perm_sub(n + 1, a)
                        a.pop()

    #
    for x in range(2, m + 1):
        perm_sub(2, [x, 1])

    return result


if __name__ == '__main__':
    print(perm(5))
