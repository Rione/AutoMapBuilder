import sys
import GraphDrawer
import Common
import sample2

drawer = GraphDrawer.GraphDrawer()
common = Common.Common()


def two_opt(nodes: list):
    route = sample2.greedy(nodes)

    total = 0
    while True:
        count = 0
        for i in range(len(route) - 2):
            i1 = i + 1
            for j in range(i + 2, len(route)):
                if j == len(route) - 1:
                    j1 = 0
                else:
                    j1 = j + 1
                if i != 0 or j1 != 0:
                    l1 = common.node_distance(route[i], route[i1], nodes)
                    l2 = common.node_distance(route[j], route[j1], nodes)
                    l3 = common.node_distance(route[i], route[j], nodes)
                    l4 = common.node_distance(route[i1], route[j1], nodes)
                    if l1 + l2 > l3 + l4:
                        # つなぎかえる
                        new_path = route[i1:j + 1]
                        route[i1:j + 1] = new_path[::-1]
                        count += 1
        total += count
        if count == 0:
            break
    print(total)
    return route


def main():
    nodes = common.read_data('./data4')
    route = two_opt(nodes)
    drawer.route_regist(route, nodes)
    drawer.nodes_regist(nodes)
    drawer.show_plt()


if __name__ == '__main__':
    main()
