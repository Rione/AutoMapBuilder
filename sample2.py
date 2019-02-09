import sys

import GraphDrawer
import Common

drawer = GraphDrawer.GraphDrawer()
common = Common.Common()


def greedy(nodes: list):
    route = []
    route.append(1)
    while len(route) < len(nodes):
        min_distance = sys.float_info.max
        min_route = 0
        for i in range(len(nodes)):
            if i + 1 not in route:
                result = common.distance(nodes[route[-1] - 1], nodes[i])
                if min_distance > result:
                    min_distance = result
                    min_route = i + 1
        route.append(min_route)
    print(route)
    return route


def main():
    nodes = common.read_data('./data3')
    route = greedy(nodes)
    drawer.route_regist(route, nodes)
    drawer.nodes_regist(nodes)
    drawer.show_plt()


if __name__ == '__main__':
    main()
