import matplotlib.pyplot as plt


class GraphDrawer:

    def reset_plt(self):
        plt.cla()

    def edges_regist(self, edges: list):
        for edge in edges:
            plt.plot([edge[0][0], edge[1][0]], [edge[0][1], edge[1][1]], 'k-')

    def route_regist(self, route: list, nodes: list):
        for i in range(len(route)):
            if i == len(route) - 1:
                plt.plot([nodes[route[i] - 1][0], nodes[route[0] - 1][0]],
                         [nodes[route[i] - 1][1], nodes[route[0] - 1][1]], 'k-')
            else:
                plt.plot([nodes[route[i] - 1][0], nodes[route[i + 1] - 1][0]],
                         [nodes[route[i] - 1][1], nodes[route[i + 1] - 1][1]], 'k-')

    def nodes_regist(self, nodes: list):
        for node in nodes:
            plt.scatter(node[0], node[1])

    def show_plt(self):
        plt.show()
        plt.cla()
