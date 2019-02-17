import os

from src import Common
from test import sample2, GraphDrawer

drawer = GraphDrawer.GraphDrawer()
common = Common.Common()


def two_opt(nodes: list):
    route = sample2.greedy(nodes)

    total = 0
    while True:
        count = 0
        for a in range(len(route) - 2):
            a_end = a + 1
            for b in range(a + 2, len(route)):
                if b == len(route) - 1:
                    b_end = 0
                else:
                    b_end = b + 1
                if a != 0 or b_end != 0:
                    l1 = common.node_distance(route[a], route[a_end], nodes)
                    l2 = common.node_distance(route[b], route[b_end], nodes)
                    l3 = common.node_distance(route[a], route[b], nodes)
                    l4 = common.node_distance(route[a_end], route[b_end], nodes)
                    if l1 + l2 > l3 + l4:
                        # つなぎかえる
                        new_path = route[a_end:b + 1]
                        route[a_end:b + 1] = new_path[::-1]
                        count += 1
        total += count
        if count == 0:
            break
    print(total)
    return route


def main():
    nodes = common.read_data(os.getcwd() + '/map/data4')
    route = two_opt(nodes)
    drawer.route_regist(route, nodes)
    drawer.map_register(nodes)
    drawer.show_plt()


if __name__ == '__main__':
    main()
