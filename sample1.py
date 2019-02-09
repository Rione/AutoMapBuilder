import sys

import GraphDrawer
import Common

drawer = GraphDrawer.GraphDrawer()
common = Common.Common()


def all_hands(nodes: list):
    min_distance = sys.float_info.max
    min_route = 0
    routes = common.perm(len(nodes))
    for i in range(len(routes)):
        result = common.sum_distance(routes[i], nodes)
        if min_distance > result:
            min_distance = result
            min_route = i

    return routes[min_route]


def main():
    nodes = common.read_data('./data3')
    route = all_hands(nodes)
    drawer.route_regist(route, nodes)
    drawer.nodes_regist(nodes)
    drawer.show_plt()


if __name__ == '__main__':
    main()
