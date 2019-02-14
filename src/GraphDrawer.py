import matplotlib.pyplot as plt


class GraphDrawer:

    def __init__(self, g_nodes: dict):
        self.g_nodes = g_nodes

    def reset_plt(self):
        plt.cla()

    def nodes_regist(self, branch_list: dict):
        for id in branch_list:
            plt.scatter(self.g_nodes.get(id).x, self.g_nodes.get(id).y)

    def show_plt(self):
        plt.show()
        plt.cla()
