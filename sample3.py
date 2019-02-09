import sys
import GraphDrawer
import Common
import sample2

drawer = GraphDrawer.GraphDrawer()
common = Common.Common()


def two_opt(nodes: list):
    route = sample2.greedy(nodes)

    for a in range(len(route)):
        for b in range(len(route)):
            if a == b or a + 1 == b:
                continue

            pass


def main():
    nodes = common.read_data('./data3')
    route = two_opt(nodes)
    drawer.route_regist(route, nodes)
    drawer.nodes_regist(nodes)
    drawer.show_plt()


if __name__ == '__main__':
    main()
